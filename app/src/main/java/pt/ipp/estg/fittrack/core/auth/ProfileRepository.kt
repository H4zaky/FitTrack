package pt.ipp.estg.fittrack.core.auth

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.fittrack.core.contacts.ContactsUtil

class ProfileRepository(
    private val fs: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun userDoc(uid: String) = fs.collection("users").document(uid)

    private fun DocumentSnapshot.toProfile(uid: String, emailFallback: String?): UserProfile {
        val name = getString("name")?.trim().takeUnless { it.isNullOrBlank() } ?: "User"
        val em = getString("email")?.trim().takeUnless { it.isNullOrBlank() } ?: (emailFallback ?: "")
        val phone = getString("phone")?.trim() ?: ""
        val createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        return UserProfile(uid = uid, name = name, email = em, phone = phone, createdAt = createdAt)
    }

    suspend fun loadOrCreateProfile(uid: String, email: String?): UserProfile {
        fun snapToProfile(d: com.google.firebase.firestore.DocumentSnapshot): UserProfile {
            val name = d.getString("name")?.trim().takeUnless { it.isNullOrBlank() } ?: "User"
            val em = d.getString("email")?.trim().takeUnless { it.isNullOrBlank() } ?: (email ?: "")
            val phone = d.getString("phone") ?: d.getLong("phone")?.toString() ?: ""
            val createdAt = d.getLong("createdAt") ?: System.currentTimeMillis()
            return UserProfile(uid = uid, name = name, email = em, phone = phone, createdAt = createdAt)
        }

        // 1) SERVER
        try {
            val d = userDoc(uid).get(Source.SERVER).await()
            Log.d("PROFILE", "SERVER exists=${d.exists()} data=${d.data}")
            if (d.exists()) return snapToProfile(d)
        } catch (e: FirebaseFirestoreException) {
           Log.e("PROFILE", "SERVER failed code=${e.code} msg=${e.message}", e)
            if (e.code != FirebaseFirestoreException.Code.UNAVAILABLE) throw e
        }

        // 2) CACHE
        try {
            val d = userDoc(uid).get(Source.CACHE).await()
            Log.d("PROFILE", "CACHE exists=${d.exists()} data=${d.data}")
            if (d.exists()) return snapToProfile(d)
        } catch (_: Exception) {}

        // 3) create if missing (TX) e tenta ler outra vez
        val data = mapOf(
            "uid" to uid,
            "name" to "User",
            "email" to (email ?: ""),
            "phone" to "",
            "createdAt" to System.currentTimeMillis()
        )

        runCatching {
            fs.runTransaction { tr ->
                val s = tr.get(userDoc(uid))
                if (!s.exists()) tr.set(userDoc(uid), data)
                Unit
            }.await()
        }

        // 4) tenta DEFAULT (server/cache)
        runCatching {
            val d = userDoc(uid).get().await()
            android.util.Log.d("PROFILE", "DEFAULT exists=${d.exists()} data=${d.data}")
            if (d.exists()) return snapToProfile(d)
        }

        return UserProfile(uid = uid, name = "User", email = email ?: "", phone = "", createdAt = System.currentTimeMillis())
    }

    suspend fun setName(uid: String, name: String) {
        userDoc(uid).set(mapOf("name" to name.trim()), SetOptions.merge()).await()
    }

    suspend fun setPhone(uid: String, phone: String) {
        val normalized = ContactsUtil.normalizePhone(phone)
        userDoc(uid).set(mapOf("phone" to normalized), SetOptions.merge()).await()
    }

    suspend fun setNameAndEmailOnRegister(uid: String, name: String, email: String) {
        val createdAt = System.currentTimeMillis()
        userDoc(uid).set(
            mapOf(
                "uid" to uid,
                "name" to name.trim(),
                "email" to email.trim(),
                "phone" to "",
                "createdAt" to createdAt
            ),
            SetOptions.merge()
        ).await()
    }
}
