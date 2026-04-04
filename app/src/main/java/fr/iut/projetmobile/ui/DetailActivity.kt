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

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CLUB_ID = "club_id"
    }

    private lateinit var repository: ClubRepository

    private lateinit var etNom   : EditText
    private lateinit var etVille : EditText
    private lateinit var btnEdit : Button
    private lateinit var btnSave : Button
    private lateinit var btnBack : Button
    private lateinit var tvDirty : TextView
    private lateinit var progressBar: ProgressBar

    private var currentClub: Club? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        repository   = ClubRepository(this)
        etNom        = findViewById(R.id.etNom)
        etVille      = findViewById(R.id.etVille)
        btnEdit      = findViewById(R.id.btnEdit)
        btnSave      = findViewById(R.id.btnSave)
        btnBack      = findViewById(R.id.btnBack)
        tvDirty      = findViewById(R.id.tvDirty)
        progressBar  = findViewById(R.id.progressBar)

        val clubId = intent.getIntExtra(EXTRA_CLUB_ID, Int.MIN_VALUE)
        if (clubId == Int.MIN_VALUE) {
            Log.w("DetailActivity", "EXTRA_CLUB_ID manquant")
            finish()
            return
        }

        supportActionBar?.title = "Détail du club"
        setEditMode(false)
        loadClub(clubId)
        btnEdit.setOnClickListener { setEditMode(true) }

        btnSave.setOnClickListener { saveClub() }
        btnBack.setOnClickListener { finish() }
    }

    private fun loadClub(id: Int) {
        progressBar.visibility = View.VISIBLE
        thread {
            val club = repository.getById(id)
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (club == null) {
                    Log.w("DetailActivity", "Club introuvable: id=$id")
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

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        etNom.isEnabled   = enabled
        etVille.isEnabled = enabled
        btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun saveClub() {
        val nom   = etNom.text.toString().trim()
        val ville = etVille.text.toString().trim()

        if (nom.isEmpty()) { etNom.error   = "Champ requis"; return }
        if (ville.isEmpty()) { etVille.error = "Champ requis"; return }

        val id = currentClub?.id ?: run {
            Log.w("DetailActivity", "Tentative de sauvegarde sans club chargé")
            return
        }

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
                    Log.i("DetailActivity", "Saved locally: id=${updatedClub.id}")
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("DetailActivity", "Save failed", e)
                runOnUiThread { progressBar.visibility = View.GONE }
            }
        }
    }
}
