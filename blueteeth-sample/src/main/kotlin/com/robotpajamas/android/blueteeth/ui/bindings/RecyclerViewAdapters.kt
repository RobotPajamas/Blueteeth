package com.robotpajamas.android.blueteeth.ui.bindings

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.robotpajamas.android.blueteeth.ui.widgets.recyclers.ViewAdapter

class RecyclerViewAdapters {

    companion object {
        @JvmStatic
        @BindingAdapter("items")
        fun setItems(view: RecyclerView, items: List<Any>) {
            val adapter = view.adapter as ViewAdapter
            adapter.items = items
            adapter.notifyDataSetChanged()
        }
    }
}
