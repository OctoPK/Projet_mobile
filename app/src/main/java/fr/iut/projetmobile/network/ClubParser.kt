package fr.iut.projetmobile.network

import fr.iut.projetmobile.model.Club
import org.json.JSONArray
import org.json.JSONObject

object ClubParser {

    fun parseClubList(jsonRaw: String): List<Club> {
        val clubsList = mutableListOf<Club>()
        var json = jsonRaw.trim()
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1).trim()
        }

        if (json.isBlank()) {
            throw Exception("API returned a blank body")
        }

        val array: JSONArray = try {
            JSONArray(json)
        } catch (e: Exception) {
            val root = JSONObject(json)
            when {
                root.has("clubs") -> {
                    val clubsNode = root.get("clubs")
                    if (clubsNode is JSONArray) clubsNode
                    else if (clubsNode is JSONObject && clubsNode.has("data")) clubsNode.getJSONArray("data")
                    else throw Exception("Unrecognized 'clubs' format: $clubsNode")
                }
                root.has("data") -> {
                    val dataNode = root.get("data")
                    when {
                        dataNode is JSONArray -> dataNode
                        // Réponse paginée Laravel : { "data": { "current_page": 1, "data": [...], ... } }
                        dataNode is JSONObject && dataNode.has("data") -> {
                            val inner = dataNode.get("data")
                            if (inner is JSONArray) inner
                            else throw Exception("Unrecognized 'data.data' format: $inner")
                        }
                        else -> throw Exception("Unrecognized 'data' format: $dataNode")
                    }
                }
                root.has("hydra:member") -> root.getJSONArray("hydra:member")
                else -> {
                    val keys = root.keys().asSequence().toList()
                    android.util.Log.e("ClubParser", "JSON Root object keys: \$keys. Root object: \$root")
                    throw Exception("No valid array found in JSON (keys: \$keys)")
                }
            }
        }

        for (i in 0 until array.length()) {
            val element = array.getJSONObject(i)
            clubsList.add(parseClub(element))
        }
        android.util.Log.d("ClubParser", "Parsed \${clubsList.size} clubs successfully.")
        return clubsList
    }

    private fun parseClub(obj: JSONObject) = Club(
        id         = obj.optInt("club_id", obj.optInt("id", -1)),
        nom        = obj.optString("club_name", obj.optString("nom", "")),
        rue        = obj.optString("club_street", ""),
        ville      = obj.optString("club_city", obj.optString("ville", "")),
        codePostal = obj.optString("club_postal_code", ""),
        isApproved = obj.optBoolean("is_approved", false),
        memberCount = obj.optJSONArray("members")?.length() ?: 0
    )

    fun clubToJson(club: Club): String {
        return JSONObject().apply {
            put("club_name", club.nom)
            put("club_city", club.ville)
            put("club_street", club.rue)
            put("club_postal_code", club.codePostal)
        }.toString()
    }
}