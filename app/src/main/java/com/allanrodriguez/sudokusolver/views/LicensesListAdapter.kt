package com.allanrodriguez.sudokusolver.views

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.allanrodriguez.sudokusolver.databinding.LicenseRowBinding
import com.allanrodriguez.sudokusolver.viewmodels.LicenseViewModel

class LicensesListAdapter(val licenses: Array<LicenseViewModel>) : RecyclerView.Adapter<LicensesListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: LicenseRowBinding = LicenseRowBinding.inflate(inflater, parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(licenses[position])
    }

    override fun getItemCount(): Int {
        return licenses.size
    }

    class ViewHolder(private val binding: LicenseRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewModel: LicenseViewModel) {
            binding.licenseVM = viewModel
            binding.executePendingBindings()
        }
    }
}