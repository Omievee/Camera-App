package com.overtime.camera.uploads_data

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.overtime.camera.model.SavedVideo

class UploadsAdapter(
    val savedVideos: List<SavedVideo>?
) : RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(UploadsView(parent.context).apply {
            layoutParams =
                ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
        })
    }
    override fun getItemCount(): Int {
        return savedVideos?.size ?: 0
    }
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        ((holder.itemView as UploadsView).bind(savedVideos?.get(position)))
    }
}