package fr.iut.projetmobile.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import fr.iut.projetmobile.database.AppDatabase
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.network.ApiClient

/**
 * Repository : source unique de vérité.
 * - Toujours lire/écrire dans la base locale (Room).
 * - Synchroniser avec le serveur quand le réseau est disponible.
 */
class ClubRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).clubDao()

    // ---------------------------------------------------------- Lecture locale

    fun getAll(): List<Club> = dao.getAll()

    fun getById(id: Int): Club? = dao.getById(id)

    // ---------------------------------------------------------- Écriture locale

    /**
     * Modifie un club localement et le marque "dirty" pour sync ultérieure.
     */
    fun saveLocally(club: Club) {
        dao.insert(club.copy(isDirty = true))
    }

    // ---------------------------------------------------------- Synchronisation

    /**
     * Synchronisation complète :
     * 1. Envoie les modifications locales (dirty) au serveur.
     * 2. Récupère la liste à jour depuis le serveur et remplace la base locale.
     *
     * ⚠️ À appeler dans un thread de fond (coroutine, Thread…), jamais sur l'UI thread.
     */
    fun sync(): Boolean {
        if (!isNetworkAvailable()) return false

        // Étape 1 : push des données "dirty"
        val dirty = dao.getDirty()
        for (club in dirty) {
            val ok = if (club.id < 0) {        // id négatif = créé localement
                ApiClient.createClub(club)
            } else {
                ApiClient.updateClub(club)
            }
            if (ok) dao.insert(club.copy(isDirty = false))
        }

        // Étape 2 : pull de la liste complète
        // Lance une exception si l'API est inaccessible
        val remote = ApiClient.getClubs()

        // On sauvegarde les enregistrements qui n'ont pas pu être synchronisés pour ne pas les perdre
        val unpushedDirty = dao.getDirty()

        // Si la récupération réussit, on met à jour la base locale
        dao.deleteAll()
        dao.insertAll(remote)

        // Réinsère les clubs modifiés localement pour les préserver
        for (dirtyClub in unpushedDirty) {
            dao.insert(dirtyClub)
        }

        return true
    }

    // ---------------------------------------------------------- Réseau

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
