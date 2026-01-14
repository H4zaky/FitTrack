package pt.ipp.estg.fittrack.core.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone

data class ContactPickResult(val name: String, val phone: String)

object ContactsUtil {

    fun readPickedContact(context: Context, contactUri: Uri): ContactPickResult? {
        val cr = context.contentResolver

        cr.query(
            contactUri,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME, Phone.TYPE),
            null,
            null,
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                val number = c.getString(0)
                val name = c.getString(1) ?: "Amigo"
                if (!number.isNullOrBlank()) {
                    return ContactPickResult(
                        name = name,
                        phone = normalizePhone(number)
                    )
                }
            }
        }

        val contactId = cr.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID),
            null, null, null
        )?.use { c ->
            if (!c.moveToFirst()) return null
            c.getString(0)
        } ?: return null

        val phones = cr.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME, Phone.TYPE),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        ) ?: return null

        phones.use { c ->
            if (!c.moveToFirst()) return null

            var bestNumber: String? = null
            var bestName: String? = null
            var foundMobile = false

            do {
                val number = c.getString(0)
                val name = c.getString(1) ?: "Amigo"
                val type = c.getInt(2)

                if (!number.isNullOrBlank()) {
                    if (type == Phone.TYPE_MOBILE) {
                        bestNumber = number
                        bestName = name
                        foundMobile = true
                        break
                    }
                    if (!foundMobile && bestNumber == null) {
                        bestNumber = number
                        bestName = name
                    }
                }
            } while (c.moveToNext())

            val finalNumber = bestNumber ?: return null
            return ContactPickResult(
                name = bestName ?: "Amigo",
                phone = normalizePhone(finalNumber)
            )
        }
    }

    fun normalizePhone(raw: String): String {
        var s = raw.trim()
        s = s.replace(Regex("[^0-9+]"), "")
        if (s.startsWith("00")) s = "+" + s.drop(2)
        if (s.count { it == '+' } > 1) {
            s = s.replace("+", "")
        }
        return s
    }
}
