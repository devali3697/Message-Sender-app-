package com.personal.messageadmin

import com.personal.messageadmin.data.model.AdminSmsAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAlertTest {

    @Test
    fun testAlertSortingByTimestampDescending() {
        val alert1 = AdminSmsAlert(id = "1", timestamp = 1000L)
        val alert2 = AdminSmsAlert(id = "2", timestamp = 3000L)
        val alert3 = AdminSmsAlert(id = "3", timestamp = 2000L)

        val list = mutableListOf(alert1, alert2, alert3)
        list.sortByDescending { it.timestamp }

        assertEquals("2", list[0].id)
        assertEquals("3", list[1].id)
        assertEquals("1", list[2].id)
    }

    @Test
    fun testUnreadCountCalculation() {
        val list = listOf(
            AdminSmsAlert(id = "1", read = false),
            AdminSmsAlert(id = "2", read = true),
            AdminSmsAlert(id = "3", read = false)
        )

        val unreadCount = list.count { !it.read }
        assertEquals(2, unreadCount)
    }
}
