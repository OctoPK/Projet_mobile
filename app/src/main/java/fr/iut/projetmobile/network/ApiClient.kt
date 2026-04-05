package fr.iut.projetmobile.network

import fr.iut.projetmobile.model.Club
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client HTTP utilisant uniquement l'API Java standard (HttpURLConnection).
 * Aucune bibliothèque tierce (pas de Retrofit, OkHttp, Gson…).
 * Le parsing JSON utilise org.json, inclus dans Android.
 *
 * Adaptez BASE_URL à l'adresse de votre serveur.
 */
object ApiClient {

    private const val BASE_URL = "http://192.168.1.16:8080/api"
    private const val TIMEOUT_MS = 5000

    // ------------------------------------------------------------------ GET
    /**
     * Récupère tous les clubs depuis le serveur.
     * Lance une exception en cas d'erreur de connexion ou HTTP.
     */
    @Throws(Exception::class)
    fun getClubs(): List<Club> {
        val connection = openConnection("$BASE_URL/clubs", "GET") ?: throw Exception("Connection failed")
        return try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                ClubParser.parseClubList(body)
            } else {
                throw Exception("HTTP Error: $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    // ----------------------------------------------------------------- AUTH
    /**
     * Authentification de l'utilisateur (POST /api/login).
     * @return un token ou true si succès, selon ce que l'API renvoie.
     */
    fun login(credentialsJson: String): Boolean {
        val connection = openConnection("$BASE_URL/login", "POST") ?: return false
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        return try {
            java.io.OutputStreamWriter(connection.outputStream).use { it.write(credentialsJson) }
            connection.responseCode == java.net.HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection.disconnect()
        }
    }

    // ----------------------------------------------------------------- POST
    /**
     * Crée un nouveau club sur le serveur.
     * Retourne true si succès (HTTP 201).
     */
    fun createClub(club: Club): Boolean {
        val connection = openConnection("$BASE_URL/clubs/", "POST") ?: return false
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        return try {
            val json = ClubParser.clubToJson(club)
            OutputStreamWriter(connection.outputStream).use { it.write(json) }
            connection.responseCode == HttpURLConnection.HTTP_CREATED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection.disconnect()
        }
    }

    // ------------------------------------------------------------------ PUT
    /**
     * Met à jour un club existant sur le serveur.
     * Retourne true si succès (HTTP 200).
     */
    fun updateClub(club: Club): Boolean {
        val connection = openConnection("$BASE_URL/clubs/${club.id}/", "PUT") ?: return false
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        return try {
            val json = ClubParser.clubToJson(club)
            OutputStreamWriter(connection.outputStream).use { it.write(json) }
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection.disconnect()
        }
    }

    // ----------------------------------------------------------- UTILITAIRES

    private fun openConnection(urlStr: String, method: String): HttpURLConnection? {
        return try {
            val url = URL(urlStr)
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
