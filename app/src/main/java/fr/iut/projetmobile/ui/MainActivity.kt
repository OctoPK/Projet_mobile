package fr.iut.projetmobile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.iut.projetmobile.R
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.repository.ClubRepository
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ClubRepository
    private lateinit var listView: ListView
    private lateinit var ivNetworkStatus: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var fab: FloatingActionButton

    private var clubs: List<Club> = emptyList()

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadList()
            if (isNetworkAvailable()) {
                performSync()
            }
        }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView    = findViewById(R.id.listView)
        ivNetworkStatus = findViewById(R.id.ivNetworkStatus)
        tvStatus    = findViewById(R.id.tvStatus)
        fab         = findViewById(R.id.fab)

        try {
            repository  = ClubRepository(this)
            loadList()
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialisation failed", e)
            tvStatus.text = "Erreur initialisation"
            return
        }

        setupNetworkCallback()

        listView.setOnItemClickListener { _, _, position, _ ->
            val club = clubs[position]
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_CLUB_ID, club.id)
            detailLauncher.launch(intent)
        }

        fab.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_CLUB_ID, DetailActivity.MODE_CREATE)
            detailLauncher.launch(intent)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    ivNetworkStatus.setImageResource(android.R.drawable.presence_online)
                }
                performSync()
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    ivNetworkStatus.setImageResource(android.R.drawable.presence_offline)
                }
            }
        }
        val req = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, networkCallback!!)

        // Initial state
        if (isNetworkAvailable()) {
            ivNetworkStatus.setImageResource(android.R.drawable.presence_online)
            performSync()
        } else {
            ivNetworkStatus.setImageResource(android.R.drawable.presence_offline)
        }
    }

    private fun performSync() {
        runOnUiThread { tvStatus.text = "Synchronisation en cours…" }
        thread {
            val success = try {
                repository.sync()
            } catch (e: Exception) {
                Log.e("MainActivity", "Sync failed", e)
                false
            }
            runOnUiThread {
                if (success) {
                    tvStatus.text = "Synchronisé ✓"
                    loadList()
                } else {
                    tvStatus.text = "Échec de synchronisation"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
        }
    }

    private fun loadList() {
        thread {
            try {
                clubs = repository.getAll()
                val items = clubs.map { "${it.nom} — ${it.ville}${if (it.isDirty) "  ✎" else ""}" }
                runOnUiThread {
                    listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
                    tvStatus.text = if (clubs.isEmpty()) "Aucune donnée (lancez une synchro)" else "${clubs.size} clubs"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "loadList failed", e)
                runOnUiThread {
                    tvStatus.text = "Erreur lecture locale"
                }
            }
        }
    }
}
