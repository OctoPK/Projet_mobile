package fr.iut.projetmobile.ui
import android.os.Bundle
import android.view.View
import android.widget.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import fr.iut.projetmobile.R
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.repository.ClubRepository
import kotlin.concurrent.thread
class EditClubActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CLUB_ID = "club_id"
        const val MODE_CREATE   = -1
    }
    private lateinit var repository: ClubRepository
    private lateinit var etNom   : EditText
    private lateinit var etVille : EditText
    private lateinit var btnSave : Button
    private lateinit var tvDirty : TextView
    private lateinit var progressBar: ProgressBar
    private var currentClub: Club? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_club)

        repository   = ClubRepository(this)
        etNom        = findViewById(R.id.etNom)
        etVille      = findViewById(R.id.etVille)
        btnSave      = findViewById(R.id.btnSave)
        tvDirty      = findViewById(R.id.tvDirty)
        progressBar  = findViewById(R.id.progressBar)

        findViewById<Button>(R.id.btnEdit)?.visibility = View.GONE
        findViewById<Button>(R.id.btnBack)?.visibility = View.GONE

        btnSave.visibility = View.VISIBLE
        etNom.isEnabled = true
        etVille.isEnabled = true

        val clubId = intent.getIntExtra(EXTRA_CLUB_ID, MODE_CREATE)

        val tvNavTitle = findViewById<TextView>(R.id.tvNavTitle)

        if (clubId == MODE_CREATE) {
            tvNavTitle.text = "Nouveau club"
        } else {
            tvNavTitle.text = "Modifier le club"
            loadClub(clubId)
        }

        btnSave.setOnClickListener { saveClub() }
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
                currentClub = club
                displayClub(club)
            }
        }
    }
    private fun displayClub(club: Club) {
        etNom.setText(club.nom)
        etVille.setText(club.ville)
        tvDirty.visibility = if (club.isDirty) View.VISIBLE else View.GONE
    }
    private fun saveClub() {
        val nom   = etNom.text.toString().trim()
        val ville = etVille.text.toString().trim()
        if (nom.isEmpty()) { etNom.error   = "Champ requis"; return }
        if (ville.isEmpty()) { etVille.error = "Champ requis"; return }
        val id = currentClub?.id ?: (-(System.currentTimeMillis() % 100000).toInt())
        val updatedClub = Club(
            id      = id,
            nom     = nom,
            ville   = ville,
            isDirty = true
        )
        progressBar.visibility = View.VISIBLE
        thread {
            try {
                repository.saveLocally(updatedClub)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("EditClubActivity", "Erreur lors de la sauvegarde locale", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@EditClubActivity, "Erreur sauvegarde: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
