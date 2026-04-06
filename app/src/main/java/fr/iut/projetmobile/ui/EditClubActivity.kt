package fr.iut.projetmobile.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.iut.projetmobile.R
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.repository.ClubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        lifecycleScope.launch {
            val club = withContext(Dispatchers.IO) {
                repository.getById(id)
            }
            progressBar.visibility = View.GONE
            if (club == null) {
                finish()
                return@launch
            }
            currentClub = club
            displayClub(club)
        }
    }

    private fun displayClub(club: Club) {
        etNom.setText(club.nom)
        etVille.setText(club.ville)
        tvDirty.visibility = View.GONE
    }

    private fun saveClub() {
        val nom   = etNom.text.toString().trim()
        val ville = etVille.text.toString().trim()
        if (nom.isEmpty()) { etNom.error   = "Champ requis"; return }
        if (ville.isEmpty()) { etVille.error = "Champ requis"; return }

        val id = currentClub?.id ?: (-(System.currentTimeMillis() % 100000).toInt())
        
        // On crée l'objet en préservant les données non éditées (rue, code postal, etc.)
        val updatedClub = Club(
            id = id,
            nom = nom,
            ville = ville,
            rue = currentClub?.rue,
            codePostal = currentClub?.codePostal,
            isApproved = currentClub?.isApproved ?: false,
            memberCount = currentClub?.memberCount ?: 0,
            isDirty = true
        )

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.saveLocally(updatedClub)
                }
                progressBar.visibility = View.GONE
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Log.e("EditClubActivity", "Erreur lors de la sauvegarde locale", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditClubActivity, "Erreur sauvegarde: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
