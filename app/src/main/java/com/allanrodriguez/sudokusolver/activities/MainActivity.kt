package com.allanrodriguez.sudokusolver.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var isLargeLayout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        if (savedInstanceState == null) {
            try {
                val fragment = EnterPuzzleFragment()
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_main, fragment, EnterPuzzleFragment::class.java.simpleName)
                        .commit()
            } catch (ex: IllegalAccessException) {
                Log.e(TAG, ex.localizedMessage)
            } catch (ex: InstantiationException) {
                Log.e(TAG, ex.localizedMessage)
            }
        }

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        navigation_view.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // TODO: Add other options, move settings and about here
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}
