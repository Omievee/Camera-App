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

//    override fun getHeaderId(position: Int): Long {
//        //  val element = (data?.data?.get(position)) ?: return 0
//        return data?.data?.size?.toLong() ?: 0L
//    }
//
//    override fun onCreateHeaderViewHolder(parent: ViewGroup): BaseViewHolder {
//        return BaseViewHolder(EventHeader(parent?.context))
//    }
//
//    override fun onBindHeaderViewHolder(holder: BaseViewHolder, p1: Int) {
//        println("ELEMENTS? ${data?.data?.get(p1)}")
//        println("ELEMENTS? ${data?.data?.get(p1)?.list?.size}")
//        val view = holder?.itemView
//        when (view) {
//            is EventHeader -> view.bind(
//                list?.get(p1)?.eventName.toString()
//            )
//        }
//        list?.get(p1)?.eventName?.let { (holder?.itemView as EventHeader).bind(it) }
//    }

    companion object {
        const val EVENT = 0
        const val UNE = 1
    }
}