const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Triggered when a new non-sensitive SMS alert is pushed to Realtime Database.
 * Sends FCM push notification to the admin device.
 */
exports.onSmsAlertCreated = functions.database
  .ref("/alerts/{alertId}")
  .onCreate(async (snapshot, context) => {
    const alertData = snapshot.val();
    if (!alertData) return null;

    const sender = alertData.sender || "Unknown Sender";
    const timestamp = alertData.timestamp || Date.now();
    const isTest = alertData.isTestAlert ? " [TEST]" : "";

    // Retrieve Admin FCM registration token
    const tokenSnapshot = await admin.database().ref("/devices/admin/fcm_token").once("value");
    const fcmToken = tokenSnapshot.val();

    if (!fcmToken) {
      console.log("No admin FCM token registered.");
      return null;
    }

    const dateStr = new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const message = {
      token: fcmToken,
      notification: {
        title: `New SMS Alert${isTest}`,
        body: `SMS received from ${sender} at ${dateStr}`
      },
      data: {
        alertId: context.params.alertId,
        sender: sender,
        timestamp: String(timestamp)
      },
      android: {
        priority: "high",
        notification: {
          sound: "default",
          channelId: "sms_alerts_channel"
        }
      }
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("FCM notification dispatched successfully:", response);
      return response;
    } catch (error) {
      console.error("Error dispatching FCM notification:", error);
      return null;
    }
  });
