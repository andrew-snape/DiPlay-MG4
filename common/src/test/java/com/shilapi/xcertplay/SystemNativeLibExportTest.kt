package com.shilapi.xcertplay

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemNativeLibExportTest {
    @Test
    fun matchesOnlyTheStockCarPlayLibraries() {
        assertTrue(SystemNativeLibExport.matches("libjni_carplay.so"))
        assertTrue(SystemNativeLibExport.matches("libjni_carplayscreen.so"))
        assertTrue(SystemNativeLibExport.matches("libcarplay.so"))
        assertFalse(SystemNativeLibExport.matches("libjni_carplay.so.bak"))
        assertFalse(SystemNativeLibExport.matches("libjni_other.so"))
        assertFalse(SystemNativeLibExport.matches("libc.so"))
    }

    @Test
    fun findsLibrariesInEachFolderAndNamesThemForTheZip() {
        val root = Files.createTempDirectory("libs").toFile()
        try {
            val lib64 = File(root, "system/lib64").apply { mkdirs() }
            val vendor = File(root, "vendor/lib64").apply { mkdirs() }
            File(lib64, "libjni_carplay.so").writeText("a")
            File(lib64, "libjni_carplayscreen.so").writeText("bb")
            File(lib64, "libc.so").writeText("no")
            File(vendor, "libcarplay_x.so").writeText("ccc")
            val missing = File(root, "system/lib")

            val found = SystemNativeLibExport.find(listOf(missing, lib64, vendor))

            assertEquals(
                listOf("libjni_carplay.so", "libjni_carplayscreen.so", "libcarplay_x.so"),
                found.map { it.file.name },
            )
            assertEquals(listOf(1L, 2L, 3L), found.map { it.size })
            assertEquals(
                "native-libs/${lib64.path.trim('/').replace('/', '_')}/libjni_carplay.so",
                found.first().entryName,
            )
            assertEquals(3, SystemNativeLibExport.zipEntries(found).size)
            assertTrue(SystemNativeLibExport.summary(found).first().startsWith("found "))
            assertTrue(SystemNativeLibExport.summary(emptyList()).single().contains("no libjni_carplay"))
        } finally {
            root.deleteRecursively()
        }
    }
}
