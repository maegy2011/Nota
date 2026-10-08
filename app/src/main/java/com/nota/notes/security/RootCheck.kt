package com.nota.notes.security

import android.os.Build
import java.io.File

// كشف الجذر/العبث (M7): إشارات خفيفة بلا مكتبات — تحذير غير حاجب، فالقرار للمستخدم.
// ملاحظة أمانة: على جهاز مروت لا ضمانة لأي حماية (المهاجم يقرأ الذاكرة)، والتحذير للتوعية فقط.
object RootCheck {
    fun isRooted(): Boolean =
        checkBuildTags() || checkSuFiles() || checkWhichSu()

    private fun checkBuildTags(): Boolean {
        val tags = Build.TAGS ?: return false
        return tags.contains("test-keys")
    }

    private fun checkSuFiles(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/system/sbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkWhichSu(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
        p.inputStream.bufferedReader().readLine() != null
    } catch (e: Exception) {
        false
    }
}
