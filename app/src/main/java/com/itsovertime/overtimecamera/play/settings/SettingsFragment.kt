package com.itsovertime.overtimecamera.play.settings


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseactivity.BaseActivity
import com.itsovertime.overtimecamera.play.itemsame.ItemSame
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject


class SettingsFragment : BottomSheetDialogFragment(), SettingsImpl {

    @Inject
    lateinit var presenter: SettingsPresenter

    override fun onLogOut() {
        startActivity(Intent(context, BaseActivity::class.java))
        activity?.finish()
    }

    override fun onTermsClicked(urlIntent: Intent) {
        startActivity(urlIntent)
    }

    override fun onContactUs(emailIntent: Intent) {
        startActivity(emailIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }


    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }


    private val clickListener = object : SettingsClickListener {
        override fun onClick(pre: SettingsPresentation) {
            when (pre.type) {
                Settings.CONTACT_OVERTIME -> presenter.clickedContactUs()
                Settings.TERMS_OF_SERVICE -> presenter.clickedTerms()
                Settings.LOGOUT -> presenter.clickedLogOut()
            }
        }
    }
    val adapter = SettingsAdapter(clickListener = clickListener)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("View craeted...")
        settingsRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter.data = SettingsAdapter.createData(context ?: return, adapter.data)
        settingsRecycler.adapter = adapter
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(
            this.context
                ?: return, R.drawable.divider
        )?.let {
            itemDecorator.setDrawable(
                it
            )
        }
        settingsRecycler.addItemDecoration(itemDecorator)
    }

    interface SettingsInterface {
        fun onSettingsOptionClicked()
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}


class SettingsViewData(
    val data: List<SettingsPresentation>,
    val diffResult: DiffUtil.DiffResult
)

data class SettingsPresentation(
    val type: Settings,
    val title: String
) : ItemSame<SettingsPresentation> {

    override fun sameAs(same: SettingsPresentation): Boolean {
        return equals(same)
    }

    override fun contentsSameAs(same: SettingsPresentation): Boolean {
        return hashCode() == same.hashCode()
    }
}

