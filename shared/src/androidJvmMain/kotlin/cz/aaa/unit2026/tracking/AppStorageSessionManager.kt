package cz.aaa.unit2026.tracking

import cz.aaa.unit2026.AppStorage
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/**
 * Persists the Supabase [UserSession] in the app-wide [AppStorage] so anonymous
 * auth identity survives app restarts.
 *
 * Without persistence, every process start would mint a fresh anonymous user
 * and the user would lose their `account_members` row — effectively orphaning
 * their account and all its tracking_sessions on every restart.
 */
internal class AppStorageSessionManager(
    private val storage: AppStorage,
) : SessionManager {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        storage.save(KEY, json.encodeToString(UserSession.serializer(), session))
    }

    override suspend fun loadSession(): UserSession? {
        val raw = storage.load(KEY)
        if (raw.isNullOrEmpty()) return null
        return runCatching { json.decodeFromString(UserSession.serializer(), raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        // AppStorage has no delete method; overwrite with empty. loadSession()
        // treats empty as absent.
        storage.save(KEY, "")
    }

    private companion object {
        const val KEY = "supabase_user_session"
    }
}