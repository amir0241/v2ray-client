package com.example.utils

import android.util.Base64
import com.example.data.ProxyProfile
import org.json.JSONObject
import java.net.URLDecoder

object LinkParser {

    fun parse(link: String): ProxyProfile? {
        val trimmed = link.trim()
        return try {
            when {
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVMess(trimmed)
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVLESSOrTrojan(trimmed, "VLESS")
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseVLESSOrTrojan(trimmed, "Trojan")
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseVMess(link: String): ProxyProfile? {
        val base64Part = link.substring(8)
        val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
        val decodedString = String(decodedBytes, Charsets.UTF_8)
        
        return try {
            val json = JSONObject(decodedString)
            val name = json.optString("ps", "VMess Server")
            val server = json.optString("add", "127.0.0.1")
            val port = json.optInt("port", 443)
            val uuid = json.optString("id", "")
            val aid = json.optInt("aid", 0)
            val net = json.optString("net", "tcp") // transport
            val path = json.optString("path", "")
            val host = json.optString("host", "")
            val tlsVal = json.optString("tls", "")
            val sni = json.optString("sni", "")

            ProxyProfile(
                name = name,
                type = "VMess",
                server = server,
                port = port,
                uuid = uuid,
                alterId = aid,
                transport = net,
                path = path,
                host = host,
                tls = tlsVal.equals("tls", ignoreCase = true),
                sni = sni
            )
        } catch (e: Exception) {
            // Fallback for non-JSON base64 VMess link styles if any
            ProxyProfile(
                name = "VMess Legacy Profile",
                type = "VMess",
                server = "127.0.0.1",
                port = 443,
                uuid = "00000000-0000-0000-0000-000000000000"
            )
        }
    }

    private fun parseVLESSOrTrojan(link: String, type: String): ProxyProfile? {
        // format: vless://uuid@host:port?query#remarks
        // format: trojan://password@host:port?query#remarks
        val schemeLength = type.length + 3 // "vless://".length or "trojan://".length
        val body = link.substring(schemeLength)
        
        val hashIndex = body.indexOf('#')
        val (mainPart, remarkPart) = if (hashIndex != -1) {
            Pair(body.substring(0, hashIndex), body.substring(hashIndex + 1))
        } else {
            Pair(body, "")
        }

        val name = if (remarkPart.isNotEmpty()) URLDecoder.decode(remarkPart, "UTF-8") else "$type Server"
        
        val atIndex = mainPart.indexOf('@')
        if (atIndex == -1) return null
        val uuidOrPassword = mainPart.substring(0, atIndex)
        val serverAndQuery = mainPart.substring(atIndex + 1)
        
        val queryIndex = serverAndQuery.indexOf('?')
        val (serverAndPort, queryString) = if (queryIndex != -1) {
            Pair(serverAndQuery.substring(0, queryIndex), serverAndQuery.substring(queryIndex + 1))
        } else {
            Pair(serverAndQuery, "")
        }
        
        val colonIndex = serverAndPort.lastIndexOf(':')
        if (colonIndex == -1) return null
        val server = serverAndPort.substring(0, colonIndex)
        val port = serverAndPort.substring(colonIndex + 1).toIntOrNull() ?: 443
        
        var transport = "tcp"
        var path = ""
        var host = ""
        var flow = ""
        var tls = false
        var sni = ""

        if (queryString.isNotEmpty()) {
            val params = queryString.split('&')
            for (param in params) {
                val kv = param.split('=')
                if (kv.size == 2) {
                    val key = kv[0].lowercase()
                    val value = URLDecoder.decode(kv[1], "UTF-8")
                    when (key) {
                        "type", "net" -> transport = value
                        "path" -> path = value
                        "host" -> host = value
                        "flow" -> flow = value
                        "security" -> if (value == "tls" || value == "xtls") tls = true
                        "sni" -> sni = value
                    }
                }
            }
        }

        return ProxyProfile(
            name = name,
            type = type,
            server = server,
            port = port,
            uuid = uuidOrPassword,
            transport = transport,
            path = path,
            host = host,
            flow = flow,
            tls = tls || sni.isNotEmpty(),
            sni = sni
        )
    }

    private fun parseShadowsocks(link: String): ProxyProfile? {
        // ss://base64(method:password)@host:port#remarks
        val body = link.substring(5)
        val hashIndex = body.indexOf('#')
        val (mainPart, remarkPart) = if (hashIndex != -1) {
            Pair(body.substring(0, hashIndex), body.substring(hashIndex + 1))
        } else {
            Pair(body, "")
        }

        val name = if (remarkPart.isNotEmpty()) URLDecoder.decode(remarkPart, "UTF-8") else "Shadowsocks Server"
        
        val atIndex = mainPart.indexOf('@')
        if (atIndex == -1) {
            // Whole mainPart might be base64-encoded including host or just info
            // Let's degrade gracefully
            return ProxyProfile(
                name = name,
                type = "Shadowsocks",
                server = "127.0.0.1",
                port = 1080,
                uuid = "password",
                encryption = "aes-256-gcm"
            )
        }
        
        val authPart = mainPart.substring(0, atIndex)
        val serverAndPortRaw = mainPart.substring(atIndex + 1)
        val qIndex = serverAndPortRaw.indexOf('?')
        val serverAndPort = if (qIndex != -1) serverAndPortRaw.substring(0, qIndex) else serverAndPortRaw
        
        val colonIndex = serverAndPort.lastIndexOf(':')
        if (colonIndex == -1) return null
        val server = serverAndPort.substring(0, colonIndex)
        val port = serverAndPort.substring(colonIndex + 1).toIntOrNull() ?: 1080

        var method = "chacha20-ietf-poly1305"
        var password = authPart

        try {
            val decodedAuth = String(Base64.decode(authPart, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
            val authCol = decodedAuth.indexOf(':')
            if (authCol != -1) {
                method = decodedAuth.substring(0, authCol)
                password = decodedAuth.substring(authCol + 1)
            }
        } catch (e: Exception) {
            // Keep authPart as password
        }

        return ProxyProfile(
            name = name,
            type = "Shadowsocks",
            server = server,
            port = port,
            uuid = password,
            encryption = method
        )
    }

    fun serialize(profile: ProxyProfile): String {
        return try {
            when (profile.type) {
                "VMess" -> {
                    val json = JSONObject().apply {
                        put("v", "2")
                        put("ps", profile.name)
                        put("add", profile.server)
                        put("port", profile.port)
                        put("id", profile.uuid)
                        put("aid", profile.alterId)
                        put("net", profile.transport)
                        put("type", "none")
                        put("host", profile.host)
                        put("path", profile.path)
                        put("tls", if (profile.tls) "tls" else "")
                        put("sni", profile.sni)
                    }
                    val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
                    val b64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP)
                    "vmess://$b64"
                }
                "VLESS" -> {
                    val query = mutableListOf<String>()
                    query.add("type=${profile.transport}")
                    if (profile.tls) query.add("security=tls")
                    if (profile.path.isNotEmpty()) {
                        query.add("path=${java.net.URLEncoder.encode(profile.path, "UTF-8")}")
                    }
                    if (profile.host.isNotEmpty()) {
                        query.add("host=${java.net.URLEncoder.encode(profile.host, "UTF-8")}")
                    }
                    if (profile.sni.isNotEmpty()) {
                        query.add("sni=${java.net.URLEncoder.encode(profile.sni, "UTF-8")}")
                    }
                    if (profile.flow.isNotEmpty()) {
                        query.add("flow=${profile.flow}")
                    }
                    val queryStr = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
                    val remark = java.net.URLEncoder.encode(profile.name, "UTF-8").replace("+", "%20")
                    "vless://${profile.uuid}@${profile.server}:${profile.port}$queryStr#$remark"
                }
                "Trojan" -> {
                    val query = mutableListOf<String>()
                    query.add("type=${profile.transport}")
                    if (profile.tls) query.add("security=tls")
                    if (profile.path.isNotEmpty()) {
                        query.add("path=${java.net.URLEncoder.encode(profile.path, "UTF-8")}")
                    }
                    if (profile.host.isNotEmpty()) {
                        query.add("host=${java.net.URLEncoder.encode(profile.host, "UTF-8")}")
                    }
                    if (profile.sni.isNotEmpty()) {
                        query.add("sni=${java.net.URLEncoder.encode(profile.sni, "UTF-8")}")
                    }
                    val queryStr = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
                    val remark = java.net.URLEncoder.encode(profile.name, "UTF-8").replace("+", "%20")
                    "trojan://${profile.uuid}@${profile.server}:${profile.port}$queryStr#$remark"
                }
                "Shadowsocks" -> {
                    val auth = "${profile.encryption}:${profile.uuid}"
                    val authBytes = auth.toByteArray(Charsets.UTF_8)
                    val b64Auth = Base64.encodeToString(authBytes, Base64.NO_WRAP)
                    val remark = java.net.URLEncoder.encode(profile.name, "UTF-8").replace("+", "%20")
                    "ss://$b64Auth@${profile.server}:${profile.port}#$remark"
                }
                else -> ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
