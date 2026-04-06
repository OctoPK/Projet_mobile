package fr.iut.projetmobile.network

import android.util.Log
import fr.iut.projetmobile.model.Club
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client HTTP robuste avec gestion JWT et diagnostics avancés.
 */
object ApiClient {

    private const val TAG = "ApiClient"
    private const val BASE_URL = "http://192.168.1.16:8080/api"
    private const val TIMEOUT_MS = 10000

    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    // ------------------------------------------------------------------ GET
    @Throws(Exception::class)
    fun getClubs(): List<Club> {
        val connection = openConnection("$BASE_URL/clubs", "GET") ?: throw Exception("Connection failed")
        return try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                ClubParser.parseClubList(body)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "GET Clubs failed ($code): $errorBody")
                throw Exception("HTTP Error: $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun findClubIdForEmail(email: String): Int? {
        val connection = openConnection("$BASE_URL/clubs", "GET") ?: return null
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                ClubParser.extractClubIdForEmail(body, email)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "findClubIdForEmail failed", e)
            null
        } finally {
            connection.disconnect()
        }
    }

    fun getClubMembers(clubId: Int): List<Pair<String, String>>? {
        val connection = openConnection("$BASE_URL/clubs", "GET") ?: return null
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                ClubParser.extractMembersForClub(body, clubId)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "getClubMembers failed", e)
            null
        } finally {
            connection.disconnect()
        }
    }

    // ----------------------------------------------------------------- AUTH
    /**
     * Tente de se connecter en essayant plusieurs endpoints et formats de champs courants.
     */
    fun login(credentialsJson: String): Boolean {
        val original = JSONObject(credentialsJson)
        val email = original.optString("email", "")
        val password = original.optString("password", "")
        
        // On prépare les payloads possibles (Symfony attend souvent 'username' au lieu de 'email')
        val payloads = listOf(
            credentialsJson, // {"email": "...", "password": "..."}
            JSONObject().put("username", email).put("password", password).toString() // {"username": "...", "password": "..."}
        )

        // On teste les deux endpoints classiques de Symfony/LexikJWT
        val endpoints = listOf("$BASE_URL/login_check", "$BASE_URL/login")

        for (url in endpoints) {
            for (payload in payloads) {
                val connection = openConnection(url, "POST", skipAuth = true) ?: continue
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                
                try {
                    Log.d(TAG, "Tentative login sur $url")
                    OutputStreamWriter(connection.outputStream).use { it.write(payload) }
                    
                    val code = connection.responseCode
                    if (code in 200..299) {
                        val body = connection.inputStream.bufferedReader().readText()
                        val json = JSONObject(body)
                        // Extraction du token (clé 'token', 'jwt' ou 'access_token')
                        authToken = json.optString("token", json.optString("jwt", json.optString("access_token", "")))
                        
                        if (!authToken.isNullOrBlank()) {
                            Log.d(TAG, "Login REUSSI sur $url")
                            return true
                        }
                    } else {
                        val error = connection.errorStream?.bufferedReader()?.readText()
                        Log.w(TAG, "Echec login sur $url ($code): $error")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors du login sur $url", e)
                } finally {
                    connection.disconnect()
                }
            }
        }
        return false
    }

    // ----------------------------------------------------------------- SYNC
    fun createClub(club: Club): Boolean {
        val connection = openConnection("$BASE_URL/clubs", "POST") ?: return false
        return try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val json = ClubParser.clubToJson(club)
            OutputStreamWriter(connection.outputStream).use { it.write(json) }
            val code = connection.responseCode
            code in 200..299
        } catch (e: Exception) { false } finally { connection.disconnect() }
    }

    fun updateClub(club: Club): Boolean {
        val connection = openConnection("$BASE_URL/clubs/${club.id}", "POST") ?: return false
        return try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-HTTP-Method-Override", "PUT")
            val json = ClubParser.clubToJson(club)
            OutputStreamWriter(connection.outputStream).use { it.write(json) }
            val code = connection.responseCode
            val error = if (code !in 200..299) connection.errorStream?.bufferedReader()?.readText() else null
            Log.d(TAG, "PUT response code: $code, error: $error")
            code in 200..299
        } catch (e: Exception) { false } finally { connection.disconnect() }
    }

    // ----------------------------------------------------------- UTILITAIRES

    private fun openConnection(urlStr: String, method: String, skipAuth: Boolean = false): HttpURLConnection? {
        return try {
            val url = URL(urlStr)
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                
                // On empeche la redirection automatique parce qu'elle fait sauter les entetes (ex: Auth)
                instanceFollowRedirects = false

                // Ajout du header Authorization si un token est disponible
                if (!skipAuth && !authToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open connection to $urlStr", e)
            null
        }
    }
}
