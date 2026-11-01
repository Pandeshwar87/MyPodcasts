package com.example.mypodcasts.util

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtil {

    /**
     * Gets the phone's local IPv4 Wi-Fi address (e.g., 192.168.1.50).
     */
    fun getLocalIpAddress(context: Context): String? {
        // Method 1: NetworkInterface traversal prioritizing wlan / wifi
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First pass: look specifically for wlan or wifi interface
            for (intf in interfaces) {
                if (intf.name.lowercase().contains("wlan") || intf.name.lowercase().contains("wifi")) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "0.0.0.0") {
                                return host
                            }
                        }
                    }
                }
            }

            // Second pass: any valid non-loopback IPv4 address (excluding cellular / dummy)
            for (intf in interfaces) {
                if (!intf.name.lowercase().contains("rmnet") && !intf.name.lowercase().contains("dummy")) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "0.0.0.0" && !host.startsWith("127.")) {
                                return host
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Method 2: WifiManager Formatter fallback
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val ipAddress = wifiManager.connectionInfo.ipAddress
                if (ipAddress != 0) {
                    @Suppress("DEPRECATION")
                    return Formatter.formatIpAddress(ipAddress)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
