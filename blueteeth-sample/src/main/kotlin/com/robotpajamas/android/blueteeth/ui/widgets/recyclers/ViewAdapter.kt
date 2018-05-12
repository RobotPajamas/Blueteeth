package com.robotpajamas.android.blueteeth.ui.widgets.recyclers

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

abstract class ViewAdapter : RecyclerView.Adapter<ViewHolder>() {

    abstract var items: List<Any>

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getObj(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: ViewDataBinding = DataBindingUtil.inflate(layoutInflater, viewType, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position)
    }

    protected abstract fun getObj(position: Int): Any

    protected abstract fun getLayoutId(position: Int): Int

    abstract override fun getItemCount(): Int
}
