package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
import com.itsovertime.overtimecamera.play.itemsame.ItemSame
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.settings.SettingsFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_uploads.*
import kotlinx.android.synthetic.main.uploads_view_toolbar.*
import javax.inject.Inject

class UploadsFragment : Fragment(), UploadsInt, View.OnClickListener,
    SwipeRefreshLayout.OnRefreshListener {

    override fun updateProgressBar(start: Int, end: Int, highQuality: Boolean, clientId: String) {
        progressData = ProgressData(start, end, highQuality, clientId)
    }


    override fun displayNoNetworkConnection() {
        uploadsIcon.visibility = View.INVISIBLE
        uploadsMessage.text = "No active connection..."
    }

    override fun displayWifiReady() {
        uploadsIcon.visibility = View.VISIBLE
        uploadsMessage.text = context?.getString(R.string.upload_queue_uploading_hd)
    }

    override fun onRefresh() {
        presenter.onResume()
    }

    override fun swipe2RefreshIsTrue() {
        swipe2refresh.isRefreshing = true
    }

    override fun swipe2RefreshIsFalse() {
        swipe2refresh.isRefreshing = false
    }

    override fun displaySettings() {
        val manager = childFragmentManager
        val transaction = manager?.beginTransaction()
        transaction?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction?.setCustomAnimations(R.anim.slide_up, R.anim.slide_out)
        transaction?.replace(R.id.fragContainer, SettingsFragment.newInstance("", ""))
            ?.addToBackStack("settings")
            .commit()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && view != null) {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.settingsButton -> {
                presenter.displayBottomSheetSettings()
            }
        }
    }

    var progressData = ProgressData()
    var adapter: UploadsAdapter = UploadsAdapter()
    override fun updateAdapter(videos: List<SavedVideo>, data: ProgressData?) {
        println("-------------------------------------------------------UPDATING ADAPTER-------------------------------------------------------")
        val old = adapter.data?.data ?: emptyList()
        val newD = mutableListOf<UploadsPresentation>()
        if (!videos.isNullOrEmpty()) {
            newD.add(UploadsPresentation(list = videos, progressData = data ?: ProgressData()))
        }

        adapter.data = UploadsViewData(newD, DiffUtil.calculateDiff(BasicDiffCallback(old, newD)))

    }

    @Inject
    lateinit var presenter: UploadsPresenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_uploads, container, false)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onCreate()
        settingsButton.setOnClickListener(this)
        debug.setOnClickListener(this)
        swipe2refresh.setOnRefreshListener(this)
        uploadsRecycler.adapter = adapter
        swipe2refresh.setColorSchemeResources(
            R.color.OT_Orange,
            R.color.OT_White,
            android.R.color.black
        )
        uploadsRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            UploadsFragment()

    }
}

class UploadsViewData(
    val data: List<UploadsPresentation>,
    val diffResult: DiffUtil.DiffResult
)

data class ProgressData(
    val start: Int = 0,
    val end: Int = 0,
    val isHighQuality: Boolean = false,
    val id: String = ""
)

data class UploadsPresentation(
    val list: List<SavedVideo>,
    val progressData: ProgressData
) : ItemSame<UploadsPresentation> {
    override fun sameAs(same: UploadsPresentation): Boolean {
        return equals(same)
    }

    override fun contentsSameAs(same: UploadsPresentation): Boolean {
        return hashCode() == same.hashCode()
    }
}
