package fr.iut.projetmobile.ui
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
    private lateinit var etNom   : EditText
    private lateinit var etVille : EditText
    private lateinit var btnEdit : Button
    private lateinit var btnBack : Button
    private lateinit var tvDirty : TextView
    private lateinit var progressBar: ProgressBar
    private var clubId: Int = -1
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

        findViewById<Button>(R.id.btnSave)?.visibility = View.GONE

        clubId = intent.getIntExtra(EXTRA_CLUB_ID, -1)

        findViewById<TextView>(R.id.tvNavTitle).text = "Détail du club"

        btnEdit.visibility = View.VISIBLE
        etNom.isEnabled = false
        etVille.isEnabled = false

        loadClub(clubId)

        btnEdit.setOnClickListener {
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
        etNom.setText(club.nom)
        etVille.setText(club.ville)
        tvDirty.visibility = if (club.isDirty) View.VISIBLE else View.GONE
    }
}
