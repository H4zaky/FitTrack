package pt.ipp.estg.fittrack.core.sync

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

suspend fun DocumentReference.getWithCacheFallback(): DocumentSnapshot? {
    return try {
        this.get(Source.SERVER).await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            (e.message?.contains("offline", ignoreCase = true) == true)
        ) {
            try {
                this.get(Source.CACHE).await()
            } catch (_: Exception) {
                null
            }
        } else {
            throw e
        }
    }
}


suspend fun <T> Task<T>.awaitOrNullWhenOffline(): T? {
    return try {
        this.await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            (e.message?.contains("offline", ignoreCase = true) == true)
        ) null else throw e
    }
}
