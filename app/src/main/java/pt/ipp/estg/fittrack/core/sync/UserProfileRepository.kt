package pt.ipp.estg.fittrack.core.sync

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserProfileRepository {
    private val fs = FirebaseFirestore.getInstance()

    suspend fun upsertProfile(uid: String, displayName: String) {
        val data = hashMapOf(
            "displayName" to displayName.trim(),
            "lastActiveAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        fs.collection("users").document(uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun getDisplayName(uid: String): String? {
        val snap = fs.collection("users").document(uid).get().await()
        return snap.getString("displayName")
    }
}
