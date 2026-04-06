package fr.iut.projetmobile.ui

import android.content.Context
import android.os.Bundle
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
        val userRole = prefs.getString(MainActivity.PREF_USER_ROLE, null)
            ?: getString(R.string.profile_default_role)

        findViewById<TextView>(R.id.tvUserName).text = userName
        findViewById<TextView>(R.id.tvUserEmail).text = userEmail
        findViewById<TextView>(R.id.tvUserRole).text = userRole
    }
}
