package com.allanrodriguez.sudokusolver.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders

import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.MY_PERMISSIONS_REQUEST_CAMERA
import com.allanrodriguez.sudokusolver.fragments.AboutDialogFragment
import com.allanrodriguez.sudokusolver.fragments.BottomNavigationDrawerFragment
import com.allanrodriguez.sudokusolver.fragments.CameraDialogFragment
import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val navDrawer: BottomNavigationDrawerFragment = BottomNavigationDrawerFragment.newInstance()
    private val tag: String = "MainActivity"

    private var isLargeLayout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        if (savedInstanceState == null) {
            try {
                val fragment: EnterPuzzleFragment = EnterPuzzleFragment.newInstance()
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_main, fragment, EnterPuzzleFragment::class.java.simpleName)
                        .commit()
            } catch (ex: IllegalAccessException) {
                Log.e(tag, ex.localizedMessage)
            } catch (ex: InstantiationException) {
                Log.e(tag, ex.localizedMessage)
            }
        }

        fab.setOnClickListener { view ->
            if (view is FloatingActionButton) {
                view.isClickable = false
                launchCameraDialog()
                view.isClickable = true
            }
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
            R.id.action_about -> {
                val aboutDialogFragment: AboutDialogFragment = AboutDialogFragment.newInstance()
                showDialogFragment(aboutDialogFragment)
            }
            R.id.action_clear -> {
                AlertDialog.Builder(this)
                        .setMessage(R.string.text_clear_sudoku)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            val fragment: Fragment =
                                    supportFragmentManager.findFragmentByTag(EnterPuzzleFragment::class.java.simpleName) as Fragment
                            val enterPuzzleVm: EnterPuzzleViewModel =
                                    ViewModelProviders.of(fragment).get(EnterPuzzleViewModel::class.java)
                            enterPuzzleVm.clear()

                            Snackbar.make(main_coordinatorLayout, R.string.text_sudoku_cleared, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.action_dismiss) { }
                                    .setAnchorView(if (fab.visibility == View.VISIBLE) fab else toolbar)
                                    .show()
                        }
                        .setNegativeButton(R.string.no, null)
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
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
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
                val cameraDialog: CameraDialogFragment = CameraDialogFragment.newInstance()
                showDialogFragment(cameraDialog)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
            ) -> {
                Log.i(tag, "Showing camera permission request rationale.")
                AlertDialog.Builder(this)
                        .setTitle("Sudoku Solver needs permission to use your camera to read puzzles from pictures.")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Log.i(tag, "Requesting camera permission...")
                            ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.CAMERA),
                                    MY_PERMISSIONS_REQUEST_CAMERA
                            )
                        }
                        .show()
            }
            else -> {
                Log.i(tag, "Requesting camera permission...")
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA
                )
            }
        }
    }

    private fun showDialogFragment(dialog: DialogFragment) {
        // Show the dialog in a small pop-up window if app is run on a tablet.
        if (isLargeLayout) {
            dialog.show(supportFragmentManager, dialog::class.java.simpleName)
        } else {
            supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_up,
                            R.anim.slide_down,
                            R.anim.slide_up,
                            R.anim.slide_down
                    )
                    .add(android.R.id.content, dialog, dialog::class.java.simpleName)
                    .addToBackStack(null)
                    .commit()
        }
    }
}
