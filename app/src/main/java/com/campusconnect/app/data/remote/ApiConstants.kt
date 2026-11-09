package com.campusconnect.app.data.remote

object ApiConstants {
    // 10.0.2.2 is standard Android emulator alias for host loopback
    const val BASE_URL = "http://10.0.2.2:8000/api/"
    
    // Fallback URL for physical device on local Wi-Fi or testing
    const val LOCALHOST_URL = "http://127.0.0.1:8000/api/"
}
