package pt.ipp.estg.fittrack.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import pt.ipp.estg.fittrack.core.auth.AuthRepository
import pt.ipp.estg.fittrack.core.auth.ProfileRepository
import pt.ipp.estg.fittrack.core.auth.UserProfile
import pt.ipp.estg.fittrack.core.sync.FirebaseSync
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.ui.navigation.AppShell

@Composable
fun AuthGate() {
    val authRepo = remember { AuthRepository() }
    val profileRepo = remember { ProfileRepository() }
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var firebaseUser: FirebaseUser? by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            firebaseUser = fa.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loading by remember { mutableStateOf(false) }

    // ✅ evita “piscar” para loading outra vez para o mesmo uid
    var loadedUid by remember { mutableStateOf<String?>(null) }

    // ✅ controla sync-down sem bloquear UI para sempre
    var syncing by remember { mutableStateOf(false) }
    var syncedUid by remember { mutableStateOf<String?>(null) }

    val uid = firebaseUser?.uid

    LaunchedEffect(uid) {
        // logout
        if (uid == null) {
            TrackingPrefs.clearUser(context)
            profile = null
            loadedUid = null
            syncing = false
            syncedUid = null
            loading = false
            return@LaunchedEffect
        }

        if (loadedUid == uid && profile != null) return@LaunchedEffect

        loading = true

        val fallback = UserProfile(
            uid = uid,
            name = "User",
            email = firebaseUser?.email.orEmpty(),
            phone = "",
            createdAt = System.currentTimeMillis()
        )

        Log.d("FitTrack", "AuthGate: logged user uid=$uid email=${firebaseUser?.email}")

        try {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    withTimeout(4000) {
                        profileRepo.loadOrCreateProfile(uid, firebaseUser?.email)
                    }
                }.getOrNull()
            }

            profile = loaded ?: fallback
            loadedUid = uid

            Log.d("FitTrack", "AuthGate: profile ready name=${profile?.name} email=${profile?.email}")
        } catch (e: Exception) {
            Log.e("AuthGate", "loadOrCreateProfile failed", e)
            profile = fallback
            loadedUid = uid
        } finally {
            loading = false
            Log.d("FitTrack", "AuthGate: loading=false")
        }
    }

    // --- Logged out ---
    if (firebaseUser == null) {
        AuthScreen(
            onLogin = { email, pass ->
                authRepo.login(email, pass)
            },
            onRegister = { name, email, pass ->
                authRepo.register(email, pass)
                val u = auth.currentUser ?: return@AuthScreen
                runCatching { profileRepo.setNameAndEmailOnRegister(u.uid, name, email) }
                Log.d("FitTrack", "AuthGate uid=${u.uid} email=${u.email}")
            },
            onResetPassword = { email ->
                authRepo.sendPasswordReset(email)
            }
        )
        return
    }

    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val p = profile!!

    LaunchedEffect(p.uid, p.name, p.phone) {
        TrackingPrefs.setUser(context, p.uid, p.name, p.phone)
    }

    LaunchedEffect(p.uid) {
        if (syncedUid == p.uid) return@LaunchedEffect

        syncing = true
        try {
            withContext(Dispatchers.IO) {
                FirebaseSync.syncDownAndReconcile(context, p.uid)
            }
            syncedUid = p.uid
        } catch (e: Exception) {
            Log.e("AuthGate", "syncDownAndReconcile failed", e)
        } finally {
            syncing = false
        }
    }

    AppShell(
        userId = p.uid,
        currentName = p.name,
        userEmail = p.email,
        currentPhone = p.phone,
        onLogout = {
            TrackingPrefs.clearUser(context)
            TrackingPrefs.clearActiveSession(context)
            FirebaseAuth.getInstance().signOut()
        },
        onSaveName = { newName ->
            withContext(Dispatchers.IO) { profileRepo.setName(p.uid, newName) }
            profile = profile?.copy(name = newName.trim())
        },
        onSavePhone = { newPhone ->
            withContext(Dispatchers.IO) { profileRepo.setPhone(p.uid, newPhone) }
            profile = profile?.copy(phone = newPhone.trim())
        }
    )
}
