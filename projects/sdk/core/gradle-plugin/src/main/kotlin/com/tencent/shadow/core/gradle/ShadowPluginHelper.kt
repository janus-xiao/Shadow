package com.tencent.shadow.core.gradle

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.experimental.and

open class ShadowPluginHelper {
    companion object {
        fun getFileMD5(file: File): String? {
            if (!file.isFile) {
                return null
            }

            val buffer = ByteArray(1024)
            var len: Int
            var inStream: FileInputStream? = null
            val digest = MessageDigest.getInstance("MD5")
            try {
                inStream = FileInputStream(file)
                do {
                    len = inStream.read(buffer, 0, 1024)
                    if (len != -1) {
                        digest.update(buffer, 0, len)
                    }
                } while (len != -1)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } finally {
                inStream?.close()
            }
            return bytes2HexStr(digest.digest())
        }

        private fun bytes2HexStr(bytes : ByteArray?): String {
            val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
            if (bytes == null || bytes.isEmpty()) {
                return ""
            }

            val buf = CharArray(2 * bytes.size)
            try {
                for (i in bytes.indices) {
                    var b = bytes[i]
                    buf[2 * i + 1] = HEX_ARRAY[(b and  0xF).toInt()]
                    b = b.toInt().ushr(4).toByte()
                    buf[2 * i + 0] = HEX_ARRAY[(b and  0xF).toInt()]
                }
            } catch (e : Exception) {
                return ""
            }

            return String(buf)
        }

        fun isCIEnv(): Boolean {
            return System.getenv("CI").equals("true", true)
        }

        fun isFinalRelease() : Boolean {
            return System.getenv("isFinalRelease").equals("true", true)
        }

        fun gitShortRev(): String {
            val proc = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            return proc.inputStream.bufferedReader().readText()
        }

        fun gitBranch(): String {
            return if (ShadowPluginHelper.isCIEnv()) {
                val tagSVN = System.getenv("tag_svn")
                tagSVN.substring(tagSVN.lastIndexOf('@') + 1, tagSVN.length)
            } else {
                val proc = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD")
                proc.inputStream.bufferedReader().readText()
            }
        }

        fun gitDescribe(): String {
            val proc = Runtime.getRuntime().exec("git describe HEAD")
            return proc.inputStream.bufferedReader().readText()
        }

        fun versionName(): String {
            val gitShortRev = gitShortRev()
            return when {
                ShadowPluginHelper.isFinalRelease() ->
                    "${System.getenv("MajorVersion")}.${System.getenv("MinorVersion")}" +
                            ".${System.getenv("FixVersion")}" + ".${System.getenv("BuildNo")}-$gitShortRev"

                ShadowPluginHelper.isCIEnv() -> {
                    val gitBranch = gitBranch()
                    val split = gitBranch.split('/')
                    val branchShortName = split[1]
                    val fileVersionName = "$branchShortName-${System.getenv("BuildNo")}-$gitShortRev"
                    fileVersionName
                }
                else -> "local"
            }

        }
    }
}