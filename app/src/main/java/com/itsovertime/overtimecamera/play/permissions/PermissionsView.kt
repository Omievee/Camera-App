package com.itsovertime.overtimecamera.play.permissions

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
import com.itsovertime.overtimecamera.play.itemsame.ItemSame
import kotlinx.android.synthetic.main.permissions_view.view.*

class PermissionsView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {

    init {
        View.inflate(context, R.layout.permissions_view, this)

        permissionsRV.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val adapter = PermissionsAdapter()
        adapter.data = PermissionsAdapter.createData(adapter.data, resources)
        permissionsRV.adapter = adapter
    }
}

class PermissionsAdapter : RecyclerView.Adapter<BaseViewHolder>() {
    var data: PermissionData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(PermissionsItemView(parent.context))
    }

    override fun getItemCount(): Int {
        return data?.data?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        ((holder.itemView) as PermissionsItemView).bind(data?.data?.get(position) ?: return)
    }

    companion object {
        fun createData(last: PermissionData?, r: Resources): PermissionData {
            val old = last?.data ?: emptyList()
            val newData = mutableListOf(
                PermissionPresentation(
                    permissionTitle = r.getString(R.string.permissions_camera)
                ),
                PermissionPresentation(
                    permissionTitle = r.getString(R.string.permissions_mic)
                ),
                PermissionPresentation(
                    permissionTitle = r.getString(R.string.permissions_location)
                ),
                PermissionPresentation(
                    permissionTitle = r.getString(R.string.permissions_storage)
                )
            )
            return PermissionData(newData, DiffUtil.calculateDiff(BasicDiffCallback(old, newData)))
        }
    }
}

class PermissionData(
    val data: List<PermissionPresentation>,
    val diffResult: DiffUtil.DiffResult
)

data class PermissionPresentation(
    val permissionTitle: String,
    @DrawableRes var icon: Int? = null
) : ItemSame<PermissionPresentation> {
    override fun sameAs(same: PermissionPresentation): Boolean {
        return equals(same)
    }

    override fun contentsSameAs(same: PermissionPresentation): Boolean {
        return hashCode() == same.hashCode()
    }
}

