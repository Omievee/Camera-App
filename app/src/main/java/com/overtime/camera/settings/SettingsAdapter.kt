package com.overtime.camera.settings

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.calculateDiff
import androidx.recyclerview.widget.RecyclerView
import com.overtime.camera.R
import com.overtime.camera.itemsame.BasicDiffCallback
import com.overtime.camera.uploads_data.BaseViewHolder
import io.fabric.sdk.android.services.settings.SettingsData

class SettingsAdapter(
    val clickListener: SettingsClickListener
) : RecyclerView.Adapter<BaseViewHolder>() {

    var data: SettingsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(SettingsView(parent.context))
    }

    override fun getItemCount(): Int {
        return data?.data?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        (holder.itemView as SettingsView).bind(data?.data?.get(position))
        holder.itemView.settingsClickListener = clickListener
    }


    companion object {
        fun createData(context: Context, last: SettingsViewData): SettingsViewData {
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
            return SettingsViewData(newData, calculateDiff(BasicDiffCallback(last.data, newData)))
        }
    }
}