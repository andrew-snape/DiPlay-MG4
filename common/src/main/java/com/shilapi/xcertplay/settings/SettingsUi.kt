// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay.settings

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * Grouped-list pieces in the style of iOS Settings, sized for a car screen: one-line rows at
 * least 64 dp tall, values on the right, explanations in a footer under each group.
 */
internal class SettingsUi(private val context: Context) {
    fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    fun row() = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    fun text(value: CharSequence, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun pressable(view: View) {
        view.background = RippleDrawable(ColorStateList.valueOf(PRESSED), null, ColorDrawable(Color.WHITE))
        view.isClickable = true
        view.isFocusable = true
    }

    fun largeTitle(value: String) = text(value, 34f, TEXT, true).apply { setPadding(dp(4), dp(8), 0, dp(20)) }

    fun groupHeader(value: String) = text(value.uppercase(), 15f, MUTED).apply {
        letterSpacing = .04f
        setPadding(dp(20), dp(6), dp(20), dp(8))
    }

    fun groupFooter(value: String, warning: Boolean = false) = text(value, 15f, if (warning) WARNING else MUTED).apply {
        setPadding(dp(20), dp(8), dp(20), 0)
    }

    /** A rounded group; add rows with [addRow] so separators go between them. */
    fun group() = column().apply {
        background = rounded(SURFACE, 14)
        clipToOutline = true
    }

    fun addRow(group: LinearLayout, row: View) {
        if (group.childCount > 0) group.addView(View(context).apply { setBackgroundColor(SEPARATOR) },
            LinearLayout.LayoutParams(-1, maxOf(1, dp(1) / 2)).apply { marginStart = dp(20) })
        group.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    /** A row's view, with its "Reconnect" tag hidden until a change needs one. */
    class Row(val view: View, val tag: View?)

    private fun baseRow(title: String, titleColor: Int = TEXT): Pair<LinearLayout, LinearLayout> {
        val line = row().apply { minimumHeight = dp(64); setPadding(dp(20), dp(10), dp(16), dp(10)) }
        val titles = row()
        titles.addView(text(title, 20f, titleColor).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END },
            LinearLayout.LayoutParams(-2, -2).apply { weight = 0f })
        line.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        return line to titles
    }

    private fun reconnectTag(titles: LinearLayout): View = text(context.getString(com.shilapi.xcertplay.host.R.string.settings_reconnect_tag), 13f, BG, true).apply {
        background = rounded(WARNING, 8)
        setPadding(dp(8), dp(2), dp(8), dp(2))
        visibility = View.GONE
        titles.addView(this, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
    }

    fun switchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit): Pair<Row, Switch> {
        val (line, titles) = baseRow(title)
        val tag = reconnectTag(titles)
        val toggle = Switch(context).apply {
            isChecked = checked
            contentDescription = title
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(ON, OFF))
            trackTintMode = android.graphics.PorterDuff.Mode.SRC
            scaleX = 1.25f; scaleY = 1.25f
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
        line.addView(toggle, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        // The whole row flips the switch, as on iOS.
        line.setOnClickListener { toggle.toggle() }
        return Row(line, tag) to toggle
    }

    /** A row with a grey value on the right and, when it opens something, a chevron. */
    fun valueRow(title: String, value: String?, chevron: Boolean, warning: Boolean = false, onClick: (() -> Unit)?): Row {
        val (line, titles) = baseRow(title)
        val tag = reconnectTag(titles)
        if (value != null) line.addView(text(value, 18f, if (warning) WARNING else MUTED).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.END
            maxWidth = dp(360)
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(16) })
        if (chevron) line.addView(text("›", 30f, CHEVRON), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12); bottomMargin = dp(3) })
        if (onClick != null) { pressable(line); line.setOnClickListener { onClick() } }
        line.contentDescription = if (value == null) title else "$title, $value"
        return Row(line, tag)
    }

    /** Accent-coloured text that does something; greys out when disabled. */
    fun actionRow(title: String, onClick: () -> Unit): TextView = text(title, 20f, ACCENT).apply {
        minHeight = dp(64); gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(10), dp(20), dp(10))
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(DISABLED, ACCENT)))
        pressable(this)
        setOnClickListener { onClick() }
    }

    /** Two or three short options, side by side, in the row. */
    fun segmentedRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit): Row {
        val (line, titles) = baseRow(title)
        val tag = reconnectTag(titles)
        val control = row().apply { background = rounded(SEGMENT, 10); setPadding(dp(3), dp(3), dp(3), dp(3)) }
        val segments = options.mapIndexed { index, option ->
            text(option, 17f, TEXT, true).apply {
                gravity = Gravity.CENTER; minWidth = dp(104); minHeight = dp(46)
                setPadding(dp(12), 0, dp(12), 0)
                contentDescription = "$title, $option"
                control.addView(this, LinearLayout.LayoutParams(-2, dp(46)))
            }
        }
        var current = selected
        fun paint() = segments.forEachIndexed { index, segment ->
            segment.background = if (index == current) rounded(SEGMENT_SELECTED, 8) else null
            segment.isSelected = index == current
        }
        segments.forEachIndexed { index, segment ->
            segment.setOnClickListener {
                if (index == current) return@setOnClickListener
                current = index; paint(); onSelect(index)
            }
        }
        paint()
        line.addView(control, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(16) })
        return Row(line, tag)
    }

    fun optionRow(title: String, selected: Boolean, onClick: () -> Unit): View {
        val (line, _) = baseRow(title)
        line.addView(text(if (selected) "✓" else "", 22f, ACCENT, true), LinearLayout.LayoutParams(dp(32), -2))
        pressable(line)
        line.isSelected = selected
        line.contentDescription = title
        line.setOnClickListener { onClick() }
        return line
    }

    fun sidebarItem(icon: Int, tile: Int, title: String, selected: Boolean, onClick: () -> Unit): View {
        val line = row().apply {
            minimumHeight = dp(60); setPadding(dp(12), dp(8), dp(12), dp(8))
            background = RippleDrawable(ColorStateList.valueOf(PRESSED), if (selected) rounded(SELECTED, 12) else null, rounded(Color.WHITE, 12))
            isSelected = selected
            contentDescription = title
            setOnClickListener { onClick() }
        }
        line.addView(iconTile(icon, tile), LinearLayout.LayoutParams(dp(36), dp(36)))
        line.addView(text(title, 20f, TEXT, selected), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
        return line
    }

    fun iconTile(icon: Int, tile: Int) = FrameLayout(context).apply {
        background = rounded(tile, 9)
        addView(ImageView(context).apply {
            setImageResource(icon); imageTintList = ColorStateList.valueOf(Color.WHITE)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
    }

    /** "‹ Display" on the left and the page title in the middle. */
    fun navBar(backLabel: String?, title: String?, onBack: () -> Unit): View = FrameLayout(context).apply {
        minimumHeight = dp(64)
        if (title != null) addView(text(title, 20f, TEXT, true).apply { gravity = Gravity.CENTER; maxLines = 1 },
            FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        if (backLabel != null) addView(text("‹  $backLabel", 20f, ACCENT).apply {
            gravity = Gravity.CENTER_VERTICAL; minHeight = dp(56); setPadding(dp(4), 0, dp(16), 0)
            contentDescription = backLabel
            background = RippleDrawable(ColorStateList.valueOf(PRESSED), null, rounded(Color.WHITE, 12))
            setOnClickListener { onBack() }
        }, FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL))
    }

    companion object {
        // The app's dark palette (DiPlayActivity), with iOS's system green for switches that are on.
        val BG = Color.rgb(12, 17, 27)
        val SIDEBAR = Color.rgb(16, 22, 34)
        val SURFACE = Color.rgb(24, 33, 48)
        val SEPARATOR = Color.rgb(48, 60, 80)
        val TEXT = Color.rgb(241, 245, 252)
        val MUTED = Color.rgb(160, 174, 196)
        val CHEVRON = Color.rgb(98, 112, 134)
        val DISABLED = Color.rgb(96, 108, 128)
        val ACCENT = Color.rgb(166, 200, 255)
        val WARNING = Color.rgb(255, 196, 128)
        val ON = Color.rgb(48, 209, 88)
        val OFF = Color.rgb(64, 76, 96)
        val SEGMENT = Color.rgb(40, 51, 70)
        val SEGMENT_SELECTED = Color.rgb(88, 106, 134)
        val SELECTED = Color.rgb(10, 132, 255)
        val PRESSED = 0x33FFFFFF
    }
}
