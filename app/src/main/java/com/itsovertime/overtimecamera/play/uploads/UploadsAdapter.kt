package com.itsovertime.overtimecamera.play.uploads

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder

class UploadsAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    var data: UploadsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }


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
        return data?.data?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        data?.data?.get(position)?.let {
            (holder.itemView as UploadsView).bind(it.list[position], it.progressData)
        }

    }
}