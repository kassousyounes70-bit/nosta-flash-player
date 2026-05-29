package com.ncore.flashplayer

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * التحقق من تذكرة الدخول القادمة من التطبيق الرئيسي NostGames
 * التذكرة = BASE64(gameId + ":" + timestamp + ":" + HMAC)
 */
object TicketValidator {

    // يجب أن يكون نفس المفتاح في التطبيق الرئيسي
    private const val SECRET_KEY = "NOSTA_SECRET_2025"

    // مدة صلاحية التذكرة = 5 دقائق
    private const val TICKET_TTL_MS = 5 * 60 * 1000L

    fun isValid(ticket: String): Boolean {
        return try {
            val decoded = String(Base64.decode(ticket, Base64.DEFAULT))
            val parts   = decoded.split(":")
            if (parts.size != 3) return false

            val gameId    = parts[0]
            val timestamp = parts[1].toLong()
            val signature = parts[2]

            // فحص انتهاء الصلاحية
            val now = System.currentTimeMillis()
            if (now - timestamp > TICKET_TTL_MS) return false

            // فحص التوقيع
            val expected = generateHmac("$gameId:$timestamp")
            signature == expected

        } catch (e: Exception) { false }
    }

    fun generate(gameId: String): String {
        val timestamp = System.currentTimeMillis()
        val signature = generateHmac("$gameId:$timestamp")
        val raw       = "$gameId:$timestamp:$signature"
        return Base64.encodeToString(raw.toByteArray(), Base64.DEFAULT)
    }

    private fun generateHmac(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray())
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }
}
