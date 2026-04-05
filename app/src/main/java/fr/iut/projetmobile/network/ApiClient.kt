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

    // ⚠️ Remplacez par l'IP/URL de votre serveur (ex: émulateur → 10.0.2.2)
    private const val BASE_URL = "http://10.0.2.2:8000/api"
    private const val TIMEOUT_MS = 5000

    // ------------------------------------------------------------------ GET
    /**
     * Récupère tous les clubs depuis le serveur.
     * Retourne une liste vide en cas d'erreur réseau.
     */
    fun getClubs(): List<Club> {
        val connection = openConnection("$BASE_URL/clubs/", "GET") ?: return emptyList()
        return try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                parseClubList(body)
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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
            val json = clubToJson(club)
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
            val json = clubToJson(club)
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

    private fun parseClubList(json: String): List<Club> {
        val clubs = mutableListOf<Club>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            clubs.add(parseClub(array.getJSONObject(i)))
        }
        return clubs
    }

    private fun parseClub(obj: JSONObject) = Club(
        id         = obj.optInt("club_id", obj.optInt("id", -1)),
        nom        = obj.optString("club_name", obj.optString("nom", "")),
        rue        = obj.optString("club_street", null),
        ville      = obj.optString("club_city", obj.optString("ville", "")),
        codePostal = obj.optString("club_postal_code", null),
        isApproved = obj.optBoolean("is_approved", false)
    )

    private fun clubToJson(club: Club): String {
        return JSONObject().apply {
            put("club_name", club.nom)
            put("club_city", club.ville)
            put("club_street", club.rue)
            put("club_postal_code", club.codePostal)
        }.toString()
    }
}
