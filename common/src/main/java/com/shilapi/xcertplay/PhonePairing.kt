package com.shilapi.xcertplay

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

/** Which discovered Bluetooth devices to offer as an iPhone. Kept pure so it can be unit tested. */
internal object PhonePairingRules {
    const val SEARCH_LIMIT_MS = 60_000L

    fun isLikelyPhone(majorClass: Int?, name: String?): Boolean =
        majorClass == BluetoothClass.Device.Major.PHONE || name?.contains("iphone", ignoreCase = true) == true

    /** Shows the last five address characters only when two devices share a name. */
    fun label(name: String?, address: String, nameShared: Boolean, fallback: String): String {
        val shown = name?.takeIf { it.isNotBlank() } ?: fallback
        return if (nameShared) "$shown · ${address.takeLast(5)}" else shown
    }
}

/**
 * Pairs an iPhone from inside the app: discovery, bond request and passkey confirmation, so the
 * user never lands in the Android settings app. Any step the car's Bluetooth stack refuses calls
 * [openSystemSettings] instead. Caller must have ACCESS_COARSE_LOCATION (or fine) on Android 9.
 */
internal class PhonePairing(
    private val activity: Activity,
    private val adapter: BluetoothAdapter,
    private val log: (String) -> Unit,
    private val openSystemSettings: () -> Unit,
    private val onBonded: (BluetoothDevice) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val found = mutableListOf<BluetoothDevice>()
    private val names = HashMap<String, String?>()
    private lateinit var adapterView: ArrayAdapter<String>
    private lateinit var status: TextView
    private var dialog: AlertDialog? = null
    private var confirmDialog: AlertDialog? = null
    private var bondingAddress: String? = null
    private var startedAt = 0L
    private var registered = false
    private var allSeen = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> if (device != null) onFound(device, intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onDiscoveryFinished()
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> if (device != null) onBondChanged(device, intent)
                BluetoothDevice.ACTION_PAIRING_REQUEST -> if (device != null) onPairingRequest(this, device, intent)
            }
        }
    }

    fun start() {
        startedAt = System.currentTimeMillis()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        }
        activity.registerReceiver(receiver, filter)
        registered = true
        showDialog()
        makeDiscoverable()
        val started = runCatching { adapter.cancelDiscovery(); adapter.startDiscovery() }.getOrDefault(false)
        log("Pairing: discovery started=$started")
        if (!started) fallback()
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }
        if (registered) runCatching { activity.unregisterReceiver(receiver) }
        registered = false
        confirmDialog?.dismiss(); confirmDialog = null
        dialog?.setOnDismissListener(null); dialog?.dismiss(); dialog = null
    }

    private fun fallback() {
        close()
        openSystemSettings()
    }

    /** Hidden API, allowed because the app runs as the system uid. Failure only means the car stays as it was. */
    private fun makeDiscoverable() {
        val result = runCatching {
            BluetoothAdapter::class.java.getMethod("setScanMode", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 120) as? Boolean
        }.getOrNull()
        log("Pairing: discoverable=$result")
    }

    private fun showDialog() {
        val density = activity.resources.displayMetrics.density
        status = TextView(activity).apply {
            text = activity.getString(R.string.pair_in_app_help)
            textSize = 16f
            setPadding((24 * density).toInt(), (8 * density).toInt(), (24 * density).toInt(), (8 * density).toInt())
        }
        adapterView = ArrayAdapter(activity, android.R.layout.simple_list_item_1, mutableListOf())
        val list = ListView(activity).apply {
            adapter = adapterView
            setOnItemClickListener { _, _, index, _ -> if (index in found.indices) pair(found[index]) }
        }
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(list, LinearLayout.LayoutParams(-1, (200 * density).toInt()))
        }
        dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.pair_in_app_title))
            .setView(body)
            .setNeutralButton(activity.getString(R.string.open_bluetooth_settings)) { _, _ -> fallback() }
            .setNegativeButton(activity.getString(R.string.cancel)) { _, _ -> close() }
            .setOnCancelListener { close() }
            .show()
    }

    private fun refreshList() {
        adapterView.clear()
        adapterView.addAll(found.map { d ->
            val shared = found.count { it.name == d.name } > 1
            PhonePairingRules.label(d.name, d.address, shared, activity.getString(R.string.paired_device))
        })
    }

    private fun onFound(device: BluetoothDevice, intent: Intent) {
        allSeen++
        if (device.bondState == BluetoothDevice.BOND_BONDED) return
        val major = (intent.getParcelableExtra<BluetoothClass>(BluetoothDevice.EXTRA_CLASS))?.majorDeviceClass
        val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: device.name
        if (!PhonePairingRules.isLikelyPhone(major, name)) return
        if (found.none { it.address == device.address }) {
            found += device
            names[device.address] = name
            refreshList()
            status.text = activity.getString(R.string.pair_in_app_pick)
            log("Pairing: found phones=${found.size} all=$allSeen")
        }
    }

    private fun onDiscoveryFinished() {
        if (bondingAddress != null) return
        if (System.currentTimeMillis() - startedAt < PhonePairingRules.SEARCH_LIMIT_MS) {
            runCatching { adapter.startDiscovery() }
        } else if (found.isEmpty()) {
            status.text = activity.getString(R.string.no_iphone_found)
            handler.post { retryButton() }
        }
    }

    private fun retryButton() {
        val d = dialog ?: return
        d.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(R.string.search_again)) { _, _ ->
            startedAt = System.currentTimeMillis()
            status.text = activity.getString(R.string.pair_in_app_help)
            runCatching { adapter.startDiscovery() }
        }
    }

    private fun pair(device: BluetoothDevice) {
        runCatching { adapter.cancelDiscovery() }
        bondingAddress = device.address
        val requested = runCatching { device.createBond() }.getOrDefault(false)
        log("Pairing: bond requested=$requested device=${device.address.takeLast(5)}")
        if (!requested) return fallback()
        status.text = activity.getString(R.string.confirm_code_on_iphone)
    }

    private fun onPairingRequest(receiver: BroadcastReceiver, device: BluetoothDevice, intent: Intent) {
        if (device.address != bondingAddress) return
        val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
        val privileged = activity.checkSelfPermission(Manifest.permission.BLUETOOTH_PRIVILEGED) == PackageManager.PERMISSION_GRANTED
        val handled = variant == PAIRING_VARIANT_PASSKEY_CONFIRMATION && privileged && receiver.isOrderedBroadcast
        log("Pairing: pairing request variant=$variant handled=$handled")
        if (!handled) return
        receiver.abortBroadcast()
        val key = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 0)
        confirmDialog?.dismiss()
        confirmDialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.pair_in_app_title))
            .setMessage(activity.getString(R.string.does_iphone_show_code, "%06d".format(key)))
            .setPositiveButton(activity.getString(R.string.pair)) { _, _ -> runCatching { device.setPairingConfirmation(true) } }
            .setNegativeButton(activity.getString(R.string.cancel)) { _, _ -> runCatching { device.setPairingConfirmation(false) } }
            .setCancelable(false)
            .show()
    }

    private fun onBondChanged(device: BluetoothDevice, intent: Intent) {
        if (device.address != bondingAddress) return
        val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
        val previous = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
        when {
            state == BluetoothDevice.BOND_BONDED -> {
                log("Pairing: bond result=bonded")
                close()
                onBonded(device)
            }
            state == BluetoothDevice.BOND_NONE && previous == BluetoothDevice.BOND_BONDING -> {
                log("Pairing: bond result=failed")
                confirmDialog?.dismiss(); confirmDialog = null
                bondingAddress = null
                status.text = activity.getString(R.string.pairing_failed)
                retryButton()
                runCatching { adapter.startDiscovery() }
            }
        }
    }

    private companion object {
        const val PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2
    }
}
