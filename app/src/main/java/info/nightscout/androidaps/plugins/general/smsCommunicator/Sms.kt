package info.nightscout.androidaps.plugins.general.smsCommunicator

import android.telephony.SmsMessage

class Sms {

    var phoneNumber: String
    var text: String
    var date: Long
    var received = false
    var sent = false
    var processed = false
    var ignored = false

    internal constructor(message: SmsMessage) {
        phoneNumber = message.originatingAddress ?: ""
        text = message.messageBody
        date = message.timestampMillis
        received = true
    }

    internal constructor(phoneNumber: String, text: String) {
        this.phoneNumber = phoneNumber
        this.text = text
        date = System.currentTimeMillis()
        sent = true
    }

    internal constructor(other: Sms, number: String? = null) {
        phoneNumber = number ?: other.phoneNumber
        text = other.text
        date = other.date
        received = other.received
        sent = other.sent
        processed = other.processed
        ignored = other.ignored
    }

    override fun toString(): String {
        return "SMS from $phoneNumber: $text"
    }
}