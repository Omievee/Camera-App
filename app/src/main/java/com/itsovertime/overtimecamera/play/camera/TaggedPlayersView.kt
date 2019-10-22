package com.itsovertime.overtimecamera.play.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
import com.itsovertime.overtimecamera.play.itemsame.ItemSame
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.settings.Settings
import kotlinx.android.synthetic.main.tagged_list_view.view.*

class TaggedPlayersView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {

    init {
        View.inflate(context, R.layout.tagged_players_view, this)
    }
}

class TaggedAthletesList(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {

    init {
        View.inflate(context, R.layout.tagged_list_view, this)
    }

    fun bind(user: User) {
        name.text = user.name
    }
}


class TaggedPlayersAdapter(
) : RecyclerView.Adapter<BaseViewHolder>() {

    var data: TaggedPlayersData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(TaggedAthletesList(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
    }

    override fun getItemCount(): Int {
        return data?.data?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        println("data?: ${data?.data?.get(position)}")
        (holder.itemView as TaggedAthletesList).bind(data?.data?.get(position)?.user ?: return)
        //  holder.itemView.settingsClickListener = clickListener
    }


}

class TaggedPlayersData(
    val data: List<TaggedPlayersPresentation>,
    val diffResult: DiffUtil.DiffResult
)

data class TaggedPlayersPresentation(
    val user: User
) : ItemSame<TaggedPlayersPresentation> {

    override fun sameAs(same: TaggedPlayersPresentation): Boolean {
        return equals(same)
    }

    override fun contentsSameAs(same: TaggedPlayersPresentation): Boolean {
        return hashCode() == same.hashCode()
    }
}