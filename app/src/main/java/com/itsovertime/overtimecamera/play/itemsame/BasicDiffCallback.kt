package com.itsovertime.overtimecamera.play.itemsame

import androidx.recyclerview.widget.DiffUtil


class BasicDiffCallback<T : ItemSame<*>>(private val old: List<T>, private val newList: List<T>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return old.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = old[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.sameAs(newItem as Nothing)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = old[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.contentsSameAs(newItem as Nothing)
    }
}