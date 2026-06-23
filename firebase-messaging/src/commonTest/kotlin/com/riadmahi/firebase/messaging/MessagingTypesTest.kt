package com.riadmahi.firebase.messaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs

/**
 * Tests for Firebase Cloud Messaging types to validate API consistency.
 *
 * Reference: https://firebase.google.com/docs/cloud-messaging
 */
class MessagingTypesTest {

    // region RemoteMessage - matches Firebase RemoteMessage

    @Test
    fun `RemoteMessage should contain message data`() {
        val message = RemoteMessage(
            messageId = "msg_123",
            messageType = "gcm",
            from = "123456789",
            to = null,
            collapseKey = null,
            data = mapOf("key1" to "value1", "key2" to "value2"),
            notification = null,
            sentTime = 1234567890L,
            ttl = 3600
        )

        assertEquals("msg_123", message.messageId)
        assertEquals("gcm", message.messageType)
        assertEquals("123456789", message.from)
        assertEquals(mapOf("key1" to "value1", "key2" to "value2"), message.data)
        assertEquals(1234567890L, message.sentTime)
        assertEquals(3600, message.ttl)
    }

    @Test
    fun `RemoteMessage should contain notification payload`() {
        val notification = RemoteMessage.Notification(
            title = "New Message",
            body = "You have a new message",
            icon = null,
            imageUrl = "https://example.com/image.png",
            sound = "default",
            tag = null,
            clickAction = "OPEN_ACTIVITY",
            channelId = "messages",
            badge = null
        )

        val message = RemoteMessage(
            messageId = "msg_456",
            messageType = null,
            from = "topic/news",
            to = null,
            collapseKey = "news_updates",
            data = emptyMap(),
            notification = notification,
            sentTime = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            ttl = 86400
        )

        assertEquals("New Message", message.notification?.title)
        assertEquals("You have a new message", message.notification?.body)
        assertEquals("https://example.com/image.png", message.notification?.imageUrl)
        assertEquals("messages", message.notification?.channelId)
    }

    // endregion

    // region RemoteMessage.Notification - matches Firebase Notification

    @Test
    fun `Notification should have title and body`() {
        val notification = RemoteMessage.Notification(
            title = "Title",
            body = "Body text",
            icon = null,
            imageUrl = null,
            sound = null,
            tag = null,
            clickAction = null,
            channelId = null,
            badge = null
        )

        assertEquals("Title", notification.title)
        assertEquals("Body text", notification.body)
    }

    @Test
    fun `Notification should support Android-specific fields`() {
        val notification = RemoteMessage.Notification(
            title = "Alert",
            body = "Alert body",
            icon = "ic_notification",
            imageUrl = null,
            sound = "alert_sound",
            tag = "alert_tag",
            clickAction = "OPEN_ALERT",
            channelId = "alerts",
            badge = null
        )

        assertEquals("ic_notification", notification.icon)
        assertEquals("alert_sound", notification.sound)
        assertEquals("alert_tag", notification.tag)
        assertEquals("OPEN_ALERT", notification.clickAction)
        assertEquals("alerts", notification.channelId)
    }

    @Test
    fun `Notification should support iOS-specific fields`() {
        val notification = RemoteMessage.Notification(
            title = "iOS Alert",
            body = "iOS Alert body",
            icon = null,
            imageUrl = null,
            sound = "default",
            tag = null,
            clickAction = null,
            channelId = null,
            badge = "5"
        )

        assertEquals("5", notification.badge)
    }

    // endregion

    // region MessagingException - matches Firebase Messaging errors

    @Test
    fun `TokenRetrievalFailed should indicate token error`() {
        val exception = MessagingException.TokenRetrievalFailed("Failed to get FCM token")

        assertIs<MessagingException>(exception)
        assertEquals("Failed to get FCM token", exception.message)
    }

    @Test
    fun `TokenDeletionFailed should indicate deletion error`() {
        val exception = MessagingException.TokenDeletionFailed("Failed to delete token")

        assertIs<MessagingException>(exception)
        assertEquals("Failed to delete token", exception.message)
    }

    @Test
    fun `TopicOperationFailed should indicate topic subscription error`() {
        val exception = MessagingException.TopicOperationFailed("Failed to subscribe to topic")

        assertIs<MessagingException>(exception)
        assertEquals("Failed to subscribe to topic", exception.message)
    }

    @Test
    fun `NotRegistered should indicate device not registered`() {
        val exception = MessagingException.NotRegistered("Device not registered for push")

        assertIs<MessagingException>(exception)
        assertEquals("Device not registered for push", exception.message)
    }

    @Test
    fun `MessagingException should accept cause`() {
        val cause = RuntimeException("Network error")
        val exception = MessagingException.Unknown("Failed", cause)

        assertEquals(cause, exception.cause)
    }

    // endregion
}
