package fr.iut.projetmobile.repository

import android.content.Context
import fr.iut.projetmobile.database.AppDatabase
import fr.iut.projetmobile.database.PendingActionEntity
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.network.ApiClient
import fr.iut.projetmobile.network.ClubParser
import fr.iut.projetmobile.sync.ConnectivityObserver
import fr.iut.projetmobile.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ClubRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val clubDao = db.clubDao()
    private val pendingActionDao = db.pendingActionDao()

    private val syncManager = SyncManager(pendingActionDao)
    private val connectivityObserver = ConnectivityObserver(context)

    fun getAll(): List<Club> = clubDao.getAll()

    fun getById(id: Int): Club? = clubDao.getById(id)

    /**
     * Sauvegarde en base locale et met l'action dans la file d'attente pour le serveur.
     */
    fun saveLocally(club: Club) {
        clubDao.insert(club)

        val actionType = if (club.id < 0) SyncManager.ACTION_CREATE_CLUB else SyncManager.ACTION_UPDATE_CLUB
        val jsonPayload = ClubParser.clubToJson(club)

        val action = PendingActionEntity(
            actionType = actionType,
            clubId = club.id,
            payload = jsonPayload
        )
        pendingActionDao.insert(action)
    }

    /**
     * Pousse la file d'attente puis récupère la nouvelle liste serveur.
     * Retourne true si tout est synchronisé (file d'attente vide ET refresh OK).
     */
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        if (!connectivityObserver.isOnline()) return@withContext false

        // 1. On vide la file d'attente vers l'API
        val syncActionsSuccess = syncManager.syncPendingActions()

        // 2. On récupère la base serveur pour se mettre à jour
        return@withContext try {
            val remote = ApiClient.getClubs()

            // On récupère les IDs des clubs qui ont encore des actions en attente
            val pendingActions = pendingActionDao.getAllPendingActions()
            val pendingClubIds = pendingActions.map { it.clubId }.toSet()

            clubDao.deleteAll()
            // On insère d'abord tout ce qui vient du serveur
            clubDao.insertAll(remote)

            // 3. On écrase/ajoute les versions locales "dirty" par-dessus
            if (pendingActions.isNotEmpty()) {
                for (action in pendingActions) {
                    val payloadStr = action.payload ?: continue
                    val payload = JSONObject(payloadStr)
                    
                    val club = Club(
                        id = payload.optInt("club_id", payload.optInt("id", action.clubId)),
                        nom = payload.optString("club_name", ""),
                        rue = payload.optString("club_street", "").takeIf { it.isNotBlank() },
                        ville = payload.optString("club_city", ""),
                        codePostal = payload.optString("club_postal_code", "").takeIf { it.isNotBlank() },
                        isApproved = payload.optBoolean("is_approved", false),
                        memberCount = payload.optInt("member_count", 0),
                        isDirty = true
                    )
                    clubDao.insert(club)
                }
            }

            // Le succès global dépend du fait que la file d'attente ait pu être vidée
            syncActionsSuccess
        } catch (e: Exception) {
            android.util.Log.e("ClubRepository", "Sync error during fetch: ${e.message}")
            false
        }
    }

    // On expose l'observeur pour l'écouter dans le ViewModel/UI
    fun getNetworkObserver() = connectivityObserver.observe()
}
