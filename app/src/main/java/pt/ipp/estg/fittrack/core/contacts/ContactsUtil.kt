package pt.ipp.estg.fittrack.core.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class ContactPickResult(val name: String, val phone: String)

object ContactsUtil {
    fun readPickedContact(context: Context, contactUri: Uri): ContactPickResult? {
        val cr = context.contentResolver

        // Primeiro tenta ir buscar o CONTACT_ID
        val contactId: String = cr.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID),
            null,
            null,
            null
        )?.use { c ->
            if (!c.moveToFirst()) return null
            c.getString(0)
        } ?: return null

        val phoneCursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        ) ?: return null

        phoneCursor.use { c ->
            if (!c.moveToFirst()) return null
            val phone = c.getString(0) ?: return null
            val name = c.getString(1) ?: "Amigo"
            return ContactPickResult(name = name, phone = normalizePhone(phone))
        }
    }

    fun normalizePhone(raw: String): String =
        raw.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
}