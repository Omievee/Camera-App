package com.itsovertime.overtimecamera.play.itemsame;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class BasicDiffCallback<T extends ItemSame> extends DiffUtil.Callback {

    private List<T> old;
    private List<T> newList;

    public BasicDiffCallback(List<T> old, List<T> newList) {
        this.old = old;
        this.newList = newList;
    }

    @Override public int getOldListSize() {
        return old.size();
    }

    @Override public int getNewListSize() {
        return newList.size();
    }

    @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        T oldItem = old.get(oldItemPosition);
        T newItem = newList.get(newItemPosition);
        return oldItem.sameAs(newItem);
    }

    @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        T oldItem = old.get(oldItemPosition);
        T newItem = newList.get(newItemPosition);
        return oldItem.contentsSameAs(newItem);
    }
}