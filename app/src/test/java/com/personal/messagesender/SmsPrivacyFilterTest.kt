package com.personal.messagesender

import com.personal.messagesender.data.model.SmsAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SmsPrivacyFilterTest {

    @Test
    fun testSmsAlertCreationExcludesBody() {
        val alert = SmsAlert(
            id = UUID.randomUUID().toString(),
            sender = "+15551234567",
            timestamp = 1700000000000L,
            synced = false,
            isTestAlert = false
        )

        assertNotNull(alert.id)
        assertEquals("+15551234567", alert.sender)
        assertEquals(1700000000000L, alert.timestamp)
        assertFalse(alert.synced)

        val map = alert.toMap()
        assertFalse(map.containsKey("body"))
        assertFalse(map.containsKey("messageBody"))
        assertFalse(map.containsKey("otp"))
        assertFalse(map.containsKey("content"))
    }

    @Test
    fun testAlertDeduplicationKey() {
        val timestamp = 1700000000000L
        val sender = "+15559876543"

        val alert1 = SmsAlert(sender = sender, timestamp = timestamp)
        val alert2 = SmsAlert(sender = sender, timestamp = timestamp + 2000L) // 2 sec diff

        val isDuplicate = alert1.sender == alert2.sender && Math.abs(alert1.timestamp - alert2.timestamp) < 10000L
        assertTrue("Alerts within 10s from same sender should be treated as duplicates", isDuplicate)
    }
}
