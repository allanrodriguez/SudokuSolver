package com.allanrodriguez.sudokusolver.activities

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.MY_PERMISSIONS_REQUEST_CAMERA
import com.allanrodriguez.sudokusolver.fragments.BottomNavigationDrawerFragment
import com.allanrodriguez.sudokusolver.fragments.CameraDialogFragment
import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val tag: String = "MainActivity"
    private val cameraDialogTag = "CameraDialogFragment"
    private val navDrawer: BottomNavigationDrawerFragment = BottomNavigationDrawerFragment.newInstance()
    private val onClearActionClicked = fun (_: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                val fragment: Fragment = supportFragmentManager.findFragmentByTag(EnterPuzzleFragment.TAG) as Fragment
                val enterPuzzleVm: EnterPuzzleViewModel = ViewModelProviders.of(fragment).get(EnterPuzzleViewModel::class.java)
                enterPuzzleVm.clear()

                val snackbar: Snackbar = Snackbar.make(toolbar, "Sudoku puzzle was cleared", Snackbar.LENGTH_LONG)
                snackbar
                        .setAction("Dismiss") { snackbar.dismiss() }
                        .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            try {
                val fragment: EnterPuzzleFragment = EnterPuzzleFragment.newInstance()
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_main, fragment, EnterPuzzleFragment.TAG)
                        .commit()
            } catch (ex: IllegalAccessException) {
                Log.e(tag, ex.localizedMessage)
            } catch (ex: InstantiationException) {
                Log.e(tag, ex.localizedMessage)
            }
        }

        fab.setOnClickListener { launchCameraDialog() }
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
            R.id.action_clear -> {
                AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to clear the sudoku puzzle?")
                        .setPositiveButton(R.string.yes, onClearActionClicked)
                        .setNegativeButton(R.string.no, onClearActionClicked)
                        .show()
            }
            R.id.action_settings -> return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA
                && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCameraDialog()
        } else {
            Log.i(tag, "Camera permission was denied.")
        }
    }

    private fun launchCameraDialog() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                Log.i(tag, "Camera permission was granted.")
                val dialog: CameraDialogFragment = CameraDialogFragment.newInstance()
                supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up,
                                R.anim.slide_down,
                                R.anim.slide_up,
                                R.anim.slide_down)
                        .add(android.R.id.content, dialog, cameraDialogTag)
                        .addToBackStack(null)
                        .commit()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) -> {
                Log.i(tag, "Showing camera permission request rationale.")
                AlertDialog.Builder(this)
                        .setTitle("Sudoku Solver needs permission to use your camera to read puzzles from pictures.")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Log.i(tag, "Requesting camera permission...")
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.CAMERA),
                                    MY_PERMISSIONS_REQUEST_CAMERA)
                        }
                        .show()
            }
            else -> {
                Log.i(tag, "Requesting camera permission...")
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA)
            }
        }
    }
}
