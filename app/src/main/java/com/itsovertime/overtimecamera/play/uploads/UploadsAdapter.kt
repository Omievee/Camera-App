package com.itsovertime.overtimecamera.play.uploads

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder
import kotlinx.android.synthetic.main.upload_item_view.view.*

class UploadsAdapter : RecyclerView.Adapter<BaseViewHolder>() {
    var holder: BaseViewHolder? = null
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        (holder.itemView as UploadsView).bind(list?.get(position) ?: return, debug, isHD)
        this.holder = holder
    }

    var data: UploadsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    var prog: Int = 0
    fun updateProgress(index: Int, prog: Int) {
        this.prog += prog
//        (holder?.itemView as UploadsView).getChildAt(index)
//            .medQProgressBar.setProgress(
//            60,
//            true
//        )
//        notifyItemChanged(index)
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