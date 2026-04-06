package fr.iut.projetmobile.sync

import android.util.Log
import fr.iut.projetmobile.database.PendingActionDao
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.network.ApiClient
import org.json.JSONObject

class SyncManager(
    private val pendingActionDao: PendingActionDao
) {
    companion object {
        private const val TAG = "SyncManager"
        const val ACTION_CREATE_CLUB = "CREATE_CLUB"
        const val ACTION_UPDATE_CLUB = "UPDATE_CLUB"
    }

    /**
     * Tente de synchroniser les actions. Retourne true si TOUTES les actions ont été traitées.
     */
    suspend fun syncPendingActions(): Boolean {
        val actions = pendingActionDao.getAllPendingActions()
        if (actions.isEmpty()) return true

        Log.d(TAG, "Synchronisation de ${actions.size} action(s) en attente...")
        var allSuccess = true

        for (action in actions) {
            try {
                val payloadStr = action.payload ?: continue
                val payload = JSONObject(payloadStr)
                val id = payload.optInt("club_id", payload.optInt("id", action.clubId))

                val club = Club(
                    id = id,
                    nom = payload.optString("club_name", ""),
                    rue = payload.optString("club_street", "").takeIf { it.isNotBlank() },
                    ville = payload.optString("club_city", ""),
                    codePostal = payload.optString("club_postal_code", "").takeIf { it.isNotBlank() },
                    isApproved = payload.optBoolean("is_approved", false),
                    memberCount = payload.optInt("member_count", 0)
                )

                val success = when (action.actionType) {
                    ACTION_CREATE_CLUB -> ApiClient.createClub(club)
                    ACTION_UPDATE_CLUB -> ApiClient.updateClub(club)
                    else -> false
                }

                if (success) {
                    pendingActionDao.delete(action)
                    Log.d(TAG, "Action ${action.actionType} synchronisée.")
                } else {
                    Log.e(TAG, "Échec serveur pour l'action ${action.actionType}")
                    allSuccess = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur parsing/réseau: ${e.message}")
                allSuccess = false
            }
        }
        return allSuccess
    }
}
