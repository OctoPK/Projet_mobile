package fr.iut.projetmobile.ui
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.iut.projetmobile.R
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.network.ApiClient
import fr.iut.projetmobile.repository.ClubRepository
import org.json.JSONObject
import kotlin.concurrent.thread
class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "AppPrefs"
        const val PREF_IS_FIRST_START = "isFirstStart"
        const val PREF_USER_EMAIL = "userEmail"
        const val PREF_USER_NAME = "userName"
        const val PREF_USER_ROLE = "userRole"
    }

    interface NetworkState {
        fun handle(context: MainActivity)
    }

    object DisconnectedState : NetworkState {
        override fun handle(context: MainActivity) {
            context.runOnUiThread {
                context.ivNetworkStatus.setImageResource(R.drawable.ic_circle_red)
            }
        }
    }

    object ConnectedNoApiState : NetworkState {
        override fun handle(context: MainActivity) {
            context.runOnUiThread {
                context.ivNetworkStatus.setImageResource(R.drawable.ic_circle_orange)
            }
        }
    }

    object ConnectedApiState : NetworkState {
        override fun handle(context: MainActivity) {
            context.runOnUiThread {
                context.ivNetworkStatus.setImageResource(R.drawable.ic_circle_green)
            }
        }
    }

    private var currentNetworkState: NetworkState = DisconnectedState

    private val retryHandler = Handler(Looper.getMainLooper())
    private val retryRunnable = Runnable {
        if (currentNetworkState == ConnectedNoApiState) {
            performSync()
        }
    }

    fun setNetworkState(state: NetworkState) {
        currentNetworkState = state
        state.handle(this)
        if (state == ConnectedApiState || state == DisconnectedState) {
            retryHandler.removeCallbacks(retryRunnable)
        }
    }

    private lateinit var repository: ClubRepository
    private lateinit var listView: ListView
    private lateinit var ivNetworkStatus: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var prefs: android.content.SharedPreferences
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
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!isUserLoggedIn()) {
            showFirstLoginDialog()
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

            if (!isUserLoggedIn()) {
                popup.menu.add(0, 10, 0, getString(R.string.menu_login))
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        10 -> {
                            showFirstLoginDialog()
                            true
                        }
                        else -> false
                    }
                }
            } else {
                popup.menu.add(0, 1, 0, getString(R.string.menu_profile))
                popup.menu.add(0, 2, 0, getString(R.string.menu_my_club))
                popup.menu.add(0, 3, 0, getString(R.string.menu_logout))
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            startActivity(Intent(this, ProfileActivity::class.java))
                            true
                        }
                        2 -> {
                            val userEmail = prefs.getString(PREF_USER_EMAIL, "") ?: ""
                            if (userEmail.isNotEmpty()) {
                                Toast.makeText(this@MainActivity, "Recherche de votre club...", Toast.LENGTH_SHORT).show()
                                thread {
                                    val clubId = fr.iut.projetmobile.network.ApiClient.findClubIdForEmail(userEmail)
                                    runOnUiThread {
                                        if (clubId != null && clubId != -1) {
                                            val detailIntent = Intent(this@MainActivity, DetailActivity::class.java)
                                            detailIntent.putExtra(DetailActivity.EXTRA_CLUB_ID, clubId)
                                            detailLauncher.launch(detailIntent)
                                        } else {
                                            Toast.makeText(this@MainActivity, "Vous n'êtes membre d'aucun club.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this@MainActivity, "Email inconnu, veuillez vous reconnecter.", Toast.LENGTH_SHORT).show()
                            }
                            true
                        }
                        3 -> {
                            prefs.edit()
                                .putBoolean(PREF_IS_FIRST_START, true)
                                .remove(PREF_USER_EMAIL)
                                .remove(PREF_USER_NAME)
                                .remove(PREF_USER_ROLE)
                                .apply()
                            Toast.makeText(this, getString(R.string.menu_logout_success), Toast.LENGTH_SHORT).show()
                            true
                        }
                        else -> false
                    }
                }
            }

            popup.show()
        }
    }

    private fun showFirstLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etLoginPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.login_dialog_title)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(R.string.login_action_sign_in, null)
            .setNegativeButton(R.string.login_action_cancel, null)
            .create()

        dialog.setOnShowListener {
            val btnSignIn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSignIn.setOnClickListener {
                val email = etEmail.text?.toString()?.trim().orEmpty()
                val password = etPassword.text?.toString().orEmpty()

                when {
                    email.isEmpty() -> {
                        etEmail.error = getString(R.string.login_email_required)
                        etEmail.requestFocus()
                        return@setOnClickListener
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        etEmail.error = getString(R.string.login_email_invalid)
                        etEmail.requestFocus()
                        return@setOnClickListener
                    }
                    password.isEmpty() -> {
                        etPassword.error = getString(R.string.login_password_required)
                        etPassword.requestFocus()
                        return@setOnClickListener
                    }
                }

                etEmail.error = null
                etPassword.error = null
                btnSignIn.isEnabled = false
                btnCancel.isEnabled = false

                thread {
                    val credentialsJson = JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .toString()

                    val isLogged = try {
                        ApiClient.login(credentialsJson)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Login failed", e)
                        false
                    }

                    runOnUiThread {
                        btnSignIn.isEnabled = true
                        btnCancel.isEnabled = true

                        if (isLogged) {
                            prefs.edit()
                                .putBoolean(PREF_IS_FIRST_START, false)
                                .putString(PREF_USER_EMAIL, email)
                                .putString(PREF_USER_NAME, buildDisplayNameFromEmail(email))
                                .putString(PREF_USER_ROLE, getString(R.string.profile_default_role))
                                .apply()
                            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun buildDisplayNameFromEmail(email: String): String {
        val localPart = email.substringBefore("@").trim()
        if (localPart.isEmpty()) return email
        return localPart
            .split('.', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
            .ifBlank { email }
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
                setNetworkState(ConnectedNoApiState)
                performSync()
            }
            override fun onLost(network: Network) {
                setNetworkState(DisconnectedState)
            }
        }
        val req = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, networkCallback!!)
        if (isNetworkAvailable()) {
            setNetworkState(ConnectedNoApiState)
            performSync()
        } else {
            setNetworkState(DisconnectedState)
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
                    setNetworkState(ConnectedApiState)
                    tvStatus.text = "Synchronisé"
                    loadList()
                } else {
                    setNetworkState(ConnectedNoApiState)
                    tvStatus.text = "Échec API (nouvelle tentative dans 10s...)"
                    retryHandler.removeCallbacks(retryRunnable)
                    retryHandler.postDelayed(retryRunnable, 10000)
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
                    tvStatus.text = if (clubs.isEmpty()) "Aucune donnée" else "${clubs.size} clubs"
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
            val tvMemberCount = view.findViewById<TextView>(R.id.tvMemberCount)

            tvName.text = club.nom
            val cityText = if (!club.codePostal.isNullOrEmpty()) "${club.ville} ${club.codePostal}" else club.ville
            tvCity.text = cityText
            tvState.text = if (club.isApproved) "Approuvé" else "En attente"

            tvMemberCount.text = "${club.memberCount} membre(s)"

            if (club.isDirty) {
                tvState.text = "Modifié localement"
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

    private fun isUserLoggedIn(): Boolean {
        return !prefs.getBoolean(PREF_IS_FIRST_START, true)
    }
}
