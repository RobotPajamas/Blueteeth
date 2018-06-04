package com.robotpajamas.android.blueteeth.ui.widgets.recyclers

import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import com.robotpajamas.android.blueteeth.BR

class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(obj: Any) {
        binding.setVariable(BR.obj, obj)
        binding.executePendingBindings()
    }
}
