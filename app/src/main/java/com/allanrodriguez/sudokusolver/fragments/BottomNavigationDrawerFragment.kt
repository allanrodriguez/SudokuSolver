package com.allanrodriguez.sudokusolver.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.allanrodriguez.sudokusolver.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.fragment_bottom_navigation_drawer.*

class BottomNavigationDrawerFragment : BottomSheetDialogFragment(),
        NavigationView.OnNavigationItemSelectedListener {

    private var currentNavItemId: Int = R.id.enter_puzzle

    companion object {
        fun newInstance() = BottomNavigationDrawerFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bottom_navigation_drawer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        navigation_view.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        var fragment: Fragment? = null

        if (id != currentNavItemId) {
            when (id) {
                R.id.enter_puzzle -> fragment = EnterPuzzleFragment.newInstance()
//                R.id.history -> return true // TODO: Add fragment for this
            }

            if (fragment != null) {
                currentNavItemId = id
                fragmentManager!!
                        .beginTransaction()
                        .replace(R.id.content_main, fragment)
                        .commit()
            }
        }

        dismiss()

        return true
    }
}
