package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_profiles")
data class ProxyProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "VMess", "VLESS", "Shadowsocks", "Trojan"
    val server: String,
    val port: Int,
    val uuid: String, // Acts as UUID or Password
    val encryption: String = "auto", // or cipher method for Shadowsocks (e.g. aes-256-gcm)
    val alterId: Int = 0,
    val transport: String = "tcp", // "tcp", "ws" (Websocket), "grpc"
    val path: String = "",
    val host: String = "",
    val flow: String = "",
    val tls: Boolean = false,
    val sni: String = "",
    val latency: Int = -1, // in milliseconds, -1 means untested, -2 means timeout/error
    val isSelected: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
