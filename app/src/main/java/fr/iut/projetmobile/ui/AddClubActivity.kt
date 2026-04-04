package fr.iut.projetmobile.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import fr.iut.projetmobile.R
import fr.iut.projetmobile.model.Club
import fr.iut.projetmobile.repository.ClubRepository
import kotlin.concurrent.thread

class AddClubActivity : AppCompatActivity() {

    private lateinit var repository: ClubRepository

    private lateinit var etNom: EditText
    private lateinit var etVille: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_club)

        supportActionBar?.title = "Nouveau club"

        repository = ClubRepository(this)
        etNom = findViewById(R.id.etNom)
        etVille = findViewById(R.id.etVille)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        btnSave.setOnClickListener { saveClub() }
    }

    private fun saveClub() {
        val nom = etNom.text.toString().trim()
        val ville = etVille.text.toString().trim()

        if (nom.isEmpty()) {
            etNom.error = "Champ requis"
            return
        }
        if (ville.isEmpty()) {
            etVille.error = "Champ requis"
            return
        }

        val newClub = Club(
            id = generateTemporaryId(),
            nom = nom,
            ville = ville,
            isDirty = true
        )

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false
        thread {
            try {
                repository.saveLocally(newClub)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Log.i("AddClubActivity", "Saved locally: id=${newClub.id}")
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("AddClubActivity", "Save failed", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                }
            }
        }
    }

    private fun generateTemporaryId(): Int {
        // ID local negatif pour distinguer une creation non synchronisee.
        return -(System.currentTimeMillis() and Int.MAX_VALUE.toLong()).toInt()
    }
}

