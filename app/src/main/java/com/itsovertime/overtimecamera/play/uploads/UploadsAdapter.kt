package com.itsovertime.overtimecamera.play.uploads

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder

class UploadsAdapter : RecyclerView.Adapter<BaseViewHolder>() {
    var uploadView: UploadsView? = null
    var debugView: UploadsDebugView? = null
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val view = holder.itemView

        when (view) {
            is UploadsView -> {
                this.uploadView = view
                view.bind(
                    list?.get(position) ?: return
                )
            }
            is UploadsDebugView -> {
                this.debugView = view
                view.bind(list?.get(position) ?: return)
            }
        }

    }

    var data: UploadsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    fun updateProgress(id: String, prog: Int, hd: Boolean) {
        if (hd) uploadView?.updateHighProgress(prog) else uploadView?.updateMediumProgress(prog)
    }

    override fun getItemViewType(position: Int): Int {
        val type = data?.data?.get(position)?.type?.ordinal!!
        println("TYPE ? $type")
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        println("UPDATING TYPE $viewType")
        return when (viewType) {
            UploadType.UserView.ordinal -> BaseViewHolder(UploadsView(parent.context).apply {
                layoutParams =
                    ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            })
            else -> BaseViewHolder(UploadsDebugView(parent.context).apply {
                ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    var list: List<SavedVideo>? = null
    var progress: ProgressData? = null
    override fun getItemCount(): Int {
        data?.data?.forEach {
            list = it.list
            progress = it.progressData
        }
        return list?.size ?: 0
    }
}