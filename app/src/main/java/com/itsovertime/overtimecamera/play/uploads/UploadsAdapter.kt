package com.itsovertime.overtimecamera.play.uploads

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder

class UploadsAdapter : RecyclerView.Adapter<BaseViewHolder>() {
    var holder: UploadsView? = null
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        this.holder = holder.itemView as UploadsView
        holder.itemView.bind(
            list?.get(position) ?: return, debug, isHD
        )
    }

    var data: UploadsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    fun updateProgress(id: String, prog: Int, hd: Boolean) {
        if (hd) {
            holder?.updateHighProgress(prog)
        } else holder?.updateMediumProgress(prog)
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

    var isHD: Boolean = false
    var list: List<SavedVideo>? = null
    var debug: Boolean = false
    override fun getItemCount(): Int {
        data?.data?.forEach {
            list = it.list
            debug = it.debug
            isHD = it.isHD
        }
        return list?.size ?: 0
    }
}