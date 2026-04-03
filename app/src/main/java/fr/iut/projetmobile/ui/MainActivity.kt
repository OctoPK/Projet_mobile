package fr.iut.projetmobile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.util.Log
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
    private lateinit var btnSync: Button
    private lateinit var tvStatus: TextView
    private lateinit var fab: FloatingActionButton

    private var clubs: List<Club> = emptyList()

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) loadList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView    = findViewById(R.id.listView)
        btnSync     = findViewById(R.id.btnSync)
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

        btnSync.setOnClickListener {
            tvStatus.text = "Synchronisation…"
            btnSync.isEnabled = false
            thread {
                val success = try {
                    repository.sync()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Sync failed", e)
                    false
                }
                runOnUiThread {
                    tvStatus.text = if (success) "Synchronisé ✓" else "Hors ligne — données locales"
                    btnSync.isEnabled = true
                    loadList()
                }
            }
        }

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
