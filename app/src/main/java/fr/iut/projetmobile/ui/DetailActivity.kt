package fr.iut.projetmobile.ui
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import fr.iut.projetmobile.R
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.repository.ClubRepository
import kotlin.concurrent.thread
class DetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CLUB_ID = "club_id"
    }
    private lateinit var repository: ClubRepository
    private lateinit var etNom   : TextView
    private lateinit var etVille : TextView
    private lateinit var btnEdit : Button
    private lateinit var btnBack : Button
    private lateinit var tvDirty : TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvIdentifiant: TextView
    private var clubId: Int = -1
    private var isLoggedIn: Boolean = false
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) loadClub(clubId)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        repository   = ClubRepository(this)
        etNom        = findViewById(R.id.etNom)
        etVille      = findViewById(R.id.etVille)
        btnEdit      = findViewById(R.id.btnEdit)
        btnBack      = findViewById(R.id.btnBack)
        tvDirty      = findViewById(R.id.tvDirty)
        progressBar  = findViewById(R.id.progressBar)
        tvIdentifiant = findViewById(R.id.tvIdentifiant)

        findViewById<Button>(R.id.btnSave)?.visibility = View.GONE

        clubId = intent.getIntExtra(EXTRA_CLUB_ID, -1)

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        isLoggedIn = !prefs.getBoolean(MainActivity.PREF_IS_FIRST_START, true)

        findViewById<TextView>(R.id.tvNavTitle).text = getString(R.string.detail_title)

        btnEdit.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        etNom.isEnabled = false
        etVille.isEnabled = false

        loadClub(clubId)

        btnEdit.setOnClickListener {
            if (!isLoggedIn) {
                return@setOnClickListener
            }
            val intent = Intent(this, EditClubActivity::class.java)
            intent.putExtra(EditClubActivity.EXTRA_CLUB_ID, clubId)
            editLauncher.launch(intent)
        }
    }
    private fun loadClub(id: Int) {
        progressBar.visibility = View.VISIBLE
        thread {
            val club = repository.getById(id)
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (club == null) {
                    finish()
                    return@runOnUiThread
                }
                displayClub(club)
            }
        }
    }
    private fun displayClub(club: Club) {
        etNom.text = club.nom

        val adresse = mutableListOf<String>()
        if (!club.rue.isNullOrEmpty()) adresse.add(club.rue)
        val villeCP = if (!club.codePostal.isNullOrEmpty()) "${club.ville} ${club.codePostal}" else club.ville
        if (villeCP.isNotEmpty()) adresse.add(villeCP)

        etVille.text = if (adresse.isNotEmpty()) adresse.joinToString(", ") else "Adresse non renseignée"

        tvIdentifiant.text = "CLUB-${club.id}"
        tvDirty.visibility = if (club.isDirty) View.VISIBLE else View.GONE
    }
}
