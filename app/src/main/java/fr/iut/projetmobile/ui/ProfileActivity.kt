package fr.iut.projetmobile.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import fr.iut.projetmobile.R

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.tvNavTitle).text = getString(R.string.profile_title)

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val userName = prefs.getString(MainActivity.PREF_USER_NAME, null)
            ?: getString(R.string.profile_default_name)
        val userEmail = prefs.getString(MainActivity.PREF_USER_EMAIL, null)
            ?: getString(R.string.profile_default_email)

        findViewById<TextView>(R.id.tvUserName).text = userName
        findViewById<TextView>(R.id.tvUserEmail).text = userEmail

        // Cacher le vieux TextView du rôle, utiliser la FlowLayout
        findViewById<TextView>(R.id.tvUserRole).visibility = android.view.View.GONE

        val llRoles = findViewById<LinearLayout>(R.id.llRolesContainer)

        // Simuler la récupération des rôles. Dans le futur, utilisez l'API /api/user et parcourez "roles" ou "pivot.role"
        val userRoles = listOf("Adhérent", "Gestionnaire Raid", "Responsable Course")

        for (role in userRoles) {
            val badge = TextView(this).apply {
                text = role
                textSize = 12f
                setPadding(32, 12, 32, 12)

                // Set layout params with margins
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }

                // Choose colors based on the role to match the web version
                when (role.lowercase()) {
                    "adhérent" -> {
                        setTextColor(Color.parseColor("#155724"))
                        setBackgroundColor(Color.parseColor("#d4edda"))
                        // Equivalent to setting a shape drawable with corner radius, but simpler programmatically:
                        background = getDrawable(R.drawable.badge_green)
                    }
                    "gestionnaire raid" -> {
                        setTextColor(Color.parseColor("#4b0082"))
                        setBackgroundColor(Color.parseColor("#e6e6fa"))
                        background = getDrawable(R.drawable.badge_purple)
                    }
                    "responsable course" -> {
                        setTextColor(Color.parseColor("#ff8c00"))
                        setBackgroundColor(Color.parseColor("#ffebd6"))
                        background = getDrawable(R.drawable.badge_orange)
                    }
                    else -> {
                        setTextColor(Color.parseColor("#383d41"))
                        setBackgroundColor(Color.parseColor("#e2e3e5"))
                        background = getDrawable(R.drawable.badge_gray)
                    }
                }
            }
            llRoles.addView(badge)
        }
    }
}
