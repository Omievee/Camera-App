package com.itsovertime.overtimecamera.play.settings

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.calculateDiff
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback

class SettingsAdapter(
        val clickListener: SettingsClickListener
) : RecyclerView.Adapter<BaseViewHolder>() {

    var data: SettingsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(SettingsView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
    }

    override fun getItemCount(): Int {
        return data?.data?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        println("data?: ${data?.data?.get(position)}")
        (holder.itemView as SettingsView).bind(data?.data?.get(position))
        holder.itemView.settingsClickListener = clickListener
    }


    companion object {
        fun createData(context: Context, last: SettingsViewData?): SettingsViewData {
            val old = last?.data ?: emptyList()
            val newData = mutableListOf(
                    SettingsPresentation(
                            type = Settings.CONTACT_OVERTIME,
                            title = context.getString(R.string.settings_view_contact)
                    ), SettingsPresentation(
                    type = Settings.TERMS_OF_SERVICE,
                    title = context.getString(R.string.settings_view_terms)
            ), SettingsPresentation(
                    type = Settings.LOGOUT,
                    title = context.getString(R.string.settings_view_logout)
            )
            )
            return SettingsViewData(newData, calculateDiff(BasicDiffCallback(old, newData)))
        }
    }
}