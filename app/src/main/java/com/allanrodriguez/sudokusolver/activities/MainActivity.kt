package com.allanrodriguez.sudokusolver.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.fragments.BottomNavigationDrawerFragment
import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity() {

    val name: String = "MainActivity"

    private val navDrawer: BottomNavigationDrawerFragment = BottomNavigationDrawerFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            try {
                val fragment: EnterPuzzleFragment = EnterPuzzleFragment.newInstance()
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_main, fragment)
                        .commit()
            } catch (ex: IllegalAccessException) {
                Log.e(name, ex.localizedMessage)
            } catch (ex: InstantiationException) {
                Log.e(name, ex.localizedMessage)
            }
        }

        fab.setOnClickListener { _ ->
            // TODO: Open camera dialog here
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home -> navDrawer.show(supportFragmentManager, navDrawer.tag)
            R.id.action_settings -> return true
        }

        return super.onOptionsItemSelected(item)
    }
}
