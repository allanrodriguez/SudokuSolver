package com.allanrodriguez.sudokusolver.views

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.allanrodriguez.sudokusolver.R
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder

import com.allanrodriguez.sudokusolver.databinding.LicenseContentBinding
import com.allanrodriguez.sudokusolver.databinding.LicenseRowBinding
import com.allanrodriguez.sudokusolver.viewmodels.LicenseViewModel

class LicensesListAdapter(licenses: List<ExpandableGroup<LicenseViewModel>>)
    : ExpandableRecyclerViewAdapter<LicensesListAdapter.TitleViewHolder, LicensesListAdapter.ContentViewHolder>(licenses) {

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: LicenseRowBinding = LicenseRowBinding.inflate(inflater, parent, false)

        return TitleViewHolder(binding)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: LicenseContentBinding = LicenseContentBinding.inflate(inflater, parent, false)

        return ContentViewHolder(binding)
    }

    override fun onBindGroupViewHolder(holder: TitleViewHolder?, flatPosition: Int, group: ExpandableGroup<*>?) {
        if (group != null) {
            holder?.bind(group.items[0] as LicenseViewModel)
        }
    }

    override fun onBindChildViewHolder(holder: ContentViewHolder?, flatPosition: Int, group: ExpandableGroup<*>?, childIndex: Int) {
        if (group != null) {
            holder?.bind(group.items[0] as LicenseViewModel)
        }
    }

    class TitleViewHolder(private val binding: LicenseRowBinding) : GroupViewHolder(binding.root) {

        fun bind(viewModel: LicenseViewModel) {
            binding.licenseVM = viewModel
            binding.executePendingBindings()
        }

        override fun collapse() {
            super.collapse()
            binding.expandCollapseIndicator.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.rotate_180clockwise)
        }

        override fun expand() {
            super.expand()
            binding.expandCollapseIndicator.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.rotate_180counterclockwise)
        }
    }

    class ContentViewHolder(private val binding: LicenseContentBinding) : ChildViewHolder(binding.root) {

        fun bind(viewModel: LicenseViewModel) {
            binding.licenseVM = viewModel
            binding.executePendingBindings()
        }
    }
}