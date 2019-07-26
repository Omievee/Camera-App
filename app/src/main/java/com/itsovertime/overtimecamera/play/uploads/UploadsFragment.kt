package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.settings.SettingsFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_uploads.*
import kotlinx.android.synthetic.main.uploads_view_toolbar.*
import javax.inject.Inject

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UploadsFragment : Fragment(), UploadsInt, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

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
        transaction?.replace(R.id.fragContainer, SettingsFragment.newInstance("", ""))?.addToBackStack("settings")
            .commit()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.settingsButton -> {
                presenter.displayBottomSheetSettings()
            }
        }
    }

    private var param1: String? = null

    override fun updateAdapter(videos: List<SavedVideo>) {
        println("list size... ${videos.size}")
        val adapter = UploadsAdapter(videos)
        uploadsRecycler.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private var param2: String? = null

    @Inject
    lateinit var presenter: UploadsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        swipe2refresh.setColorSchemeResources(
            R.color.OT_Orange,
            R.color.OT_White,
            android.R.color.black
        )
        uploadsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
    //https://admin.itsovertime.com/videos/

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UploadsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
