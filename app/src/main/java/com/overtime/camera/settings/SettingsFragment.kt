package com.overtime.camera.settings


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import com.overtime.camera.R
import com.overtime.camera.itemsame.ItemSame
import com.overtime.camera.uploads.UploadsFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject


class SettingsFragment : BottomSheetDialogFragment(), SettingsImpl {

    @Inject
    lateinit var presenter: SettingsPresenter

    override fun onLogOut() {

    }

    override fun onTermsClicked() {

    }

    override fun onContactUs() {

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

    var adapter = SettingsAdapter(clickListener = clickListener)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        settingsRecycler.adapter = adapter
        adapter.data = adapter.data?.let {
            SettingsAdapter.createData(context ?: return, it)
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                SettingsFragment()
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

