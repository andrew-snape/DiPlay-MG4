package com.shilapi.xcertplay

import android.util.Base64
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Diagnostics (MG4): copies the stock AllGo CarPlay native libraries (`libjni_carplay*.so`,
 * `libcarplay*.so`) off the car, so call audio, Siri audio and the microphone path can be read
 * (docs/MG4_SWI165_ALLGO_NOTES.md, "Not visible"). They are searched for in the system and vendor
 * library folders.
 *
 * Read-only: files are only read, never changed. Where they go:
 * - **Save report** (USB or a chosen folder) is one text file, so each library is written into it as
 *   base64 between markers, with its size and SHA-256;
 * - **Download to iPhone** adds each library to the zip as `native-libs/<folder>/<name>`.
 */
internal object SystemNativeLibExport {
    private const val TAG = "DiPlay-NativeLibs"
    val DIRECTORIES = listOf("/system/lib", "/system/lib64", "/vendor/lib", "/vendor/lib64")
    private const val MAX_FILE_BYTES = 24L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 48L * 1024 * 1024

    /** A library found on the car, and the path it has inside the zip. */
    data class Found(val file: File, val entryName: String) {
        val size: Long get() = file.length()
    }

    fun matches(name: String): Boolean =
        name.endsWith(".so") && (name.startsWith("libjni_carplay") || name.startsWith("libcarplay"))

    /** Every matching library directly inside [roots], in a stable order. A folder that cannot be read is skipped. */
    fun find(roots: List<File> = DIRECTORIES.map(::File)): List<Found> =
        roots.flatMap { root ->
            val names = runCatching { root.listFiles() }.getOrNull().orEmpty()
            names.filter { it.isFile && matches(it.name) }
                .sortedBy { it.name }
                .map { Found(it, entryName(root, it)) }
        }

    /** `native-libs/system_lib64/libjni_carplay.so` for `/system/lib64/libjni_carplay.so`. */
    internal fun entryName(root: File, file: File): String =
        "native-libs/" + root.path.trim('/').replace('/', '_') + "/" + file.name

    /** The libraries within the size limits, as zip entry names with their files. */
    fun zipEntries(found: List<Found> = find()): List<Pair<String, File>> {
        var total = 0L
        return found.filter { entry ->
            val size = entry.size
            (size <= MAX_FILE_BYTES && total + size <= MAX_TOTAL_BYTES).also { if (it) total += size }
        }.map { it.entryName to it.file }
    }

    /** What was found, as report lines: path and size, or "none found". */
    fun summary(found: List<Found> = find()): List<String> =
        if (found.isEmpty()) {
            listOf("Searched ${DIRECTORIES.joinToString(", ")}: no libjni_carplay*.so or libcarplay*.so found")
        } else {
            found.map { "found ${it.file.path} size=${it.size}" }
        }

    /**
     * Adds the section to [report]. With [inline] each library is written as base64; otherwise the
     * section lists them and says they travel in the zip.
     */
    fun appendTo(report: StringBuilder, inline: Boolean) {
        report.appendLine("--- Stock native libraries ---")
        val found = find()
        summary(found).forEach { line ->
            Log.i(TAG, line)
            report.appendLine(line)
        }
        if (found.isNotEmpty() && !inline) {
            report.appendLine("Copies are in this download's zip under native-libs/")
        } else {
            var total = 0L
            for (entry in found) {
                val size = entry.size
                if (size > MAX_FILE_BYTES || total + size > MAX_TOTAL_BYTES) {
                    report.appendLine("SKIPPED ${entry.entryName} size=$size (over the size limit)")
                    continue
                }
                val bytes = runCatching { entry.file.readBytes() }.getOrElse {
                    report.appendLine("UNREADABLE ${entry.entryName} size=$size ${it.javaClass.simpleName}")
                    null
                } ?: continue
                total += bytes.size
                report.appendLine("BEGIN-FILE ${entry.entryName} size=${bytes.size} sha256=${sha256(bytes)}")
                report.append(Base64.encodeToString(bytes, Base64.DEFAULT))
                report.appendLine("END-FILE ${entry.entryName}")
            }
        }
        report.appendLine()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
