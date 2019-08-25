package com.allanrodriguez.sudokusolver.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupEnterPuzzleFragment(savedInstanceState)
        setupNavigationDrawer()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // TODO: Add other options, move settings and about here
        return super.onOptionsItemSelected(item)
    }

    private fun setupEnterPuzzleFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            try {
                val fragment = EnterPuzzleFragment()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.content_main, fragment, EnterPuzzleFragment::class.java.simpleName).commit()
            } catch (ex: Exception) {
                Log.e(TAG, ex.localizedMessage)
            }
        }
    }

    private fun setupNavigationDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val toggle =
                ActionBarDrawerToggle(this,
                        drawerLayout,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close)

        drawerLayout.addDrawerListener(toggle)

        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}
