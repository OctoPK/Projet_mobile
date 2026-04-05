package fr.iut.projetmobile.ui
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
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
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstStart = prefs.getBoolean("isFirstStart", true)
        if (isFirstStart) {
            Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_LONG).show()
            prefs.edit().putBoolean("isFirstStart", false).apply()
        }

        // Configure NavHeader
        findViewById<TextView>(R.id.tvNavTitle).text = "Clubs"
        val btnNavProfile = findViewById<ImageView>(R.id.btnNavProfile)
        btnNavProfile.visibility = View.GONE // Removed profil from top since it's in the FAB menu now

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

        fab.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Profil")
            popup.menu.add(0, 2, 0, "Mon Club")
            popup.menu.add(0, 3, 0, "Déconnexion")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    }
                    2 -> {
                        Toast.makeText(this, "Mon Club : Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
                        true
                    }
                    3 -> {
                        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
                        finish() // Simulate logout
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
                    tvStatus.text = "Synchronisé"
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
                runOnUiThread {
                    listView.adapter = ClubAdapter(this, clubs)
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
    private inner class ClubAdapter(context: Context, private val items: List<Club>) : ArrayAdapter<Club>(context, R.layout.item_club, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_club, parent, false)
            val club = items[position]
            val tvName = view.findViewById<TextView>(R.id.tvClubName)
            val tvCity = view.findViewById<TextView>(R.id.tvClubCity)
            val tvState = view.findViewById<TextView>(R.id.tvClubState)
            val btnViewDetails = view.findViewById<Button>(R.id.btnViewDetails)
            tvName.text = club.nom
            val cityText = if (!club.codePostal.isNullOrEmpty()) "${club.ville} ${club.codePostal}" else club.ville
            tvCity.text = cityText
            tvState.text = if (club.isApproved) "Approuvé" else "En attente"
            if (club.isDirty) {
                tvState.text = "Modifié localement 👀"
                tvState.setTextColor(android.graphics.Color.parseColor("#E65100"))
            } else if (!club.isApproved) {
                tvState.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            } else {
                tvState.setTextColor(android.graphics.Color.parseColor("#666666"))
            }
            btnViewDetails.setOnClickListener {
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_CLUB_ID, club.id)
                detailLauncher.launch(intent)
            }
            btnViewDetails.isFocusable = false
            return view
        }
    }
}
