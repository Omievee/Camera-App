//package com.itsovertime.overtimecamera.play.uploads
//
//import android.content.Context
//import android.content.res.Resources
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.CompoundButton
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.DiffUtil.calculateDiff
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
//import com.itsovertime.overtimecamera.play.R
//import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
//import com.itsovertime.overtimecamera.play.itemsame.ItemSame
//import com.itsovertime.overtimecamera.play.model.SavedVideo
//import com.itsovertime.overtimecamera.play.settings.SettingsFragment
//import dagger.android.support.AndroidSupportInjection
//import kotlinx.android.synthetic.main.fragment_uploads.*
//import kotlinx.android.synthetic.main.uploads_view_toolbar.*
//import javax.inject.Inject
//
//class UploadsFragment : Fragment(), UploadsInt, View.OnClickListener,
//    CompoundButton.OnCheckedChangeListener,
//    SwipeRefreshLayout.OnRefreshListener {
//    override fun setUploadingHdVideo() {
//        uploadsMessage.text = "Uploading High Quality Videos.."
//        uploadsIcon.setImageResource(R.drawable.upload)
//    }
//
//    override fun setUploadingMedVideo() {
//        uploadsMessage.text = "Uploading Medium Quality Videos.."
//        uploadsIcon.setImageResource(R.drawable.upload)
//    }
//
//    override fun onNotifyOfPendingHDUploads() {
//        uploadsMessage.text =
//            "HD videos are ready for upload. Turn on HD uploading. (WiFi Recommended)"
//        uploadsIcon.setImageResource(R.drawable.warning)
//    }
//
//    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
//        presenter.hdSwitchWasChecked(isChecked)
//    }
//
//    override fun updateProgressBar(start: Int, end: Int, highQuality: Boolean, clientId: String) {
//        progressData = ProgressData(start, end, highQuality, clientId)
//    }
//
//    override fun displayNoNetworkConnection() {
//        uploadsIcon.visibility = View.INVISIBLE
//        // uploadsMessage.text = "No active connection..."
//    }
//
//    override fun displayWifiReady() {
//        uploadsIcon.visibility = View.VISIBLE
//        uploadsMessage.text = context?.getString(R.string.upload_queue_uploading_hd)
//    }
//
//    override fun onRefresh() {
//        presenter.onRefresh()
//    }
//
//    override fun swipe2RefreshIsTrue() {
//        swipe2refresh.isRefreshing = true
//    }
//
//    override fun swipe2RefreshIsFalse() {
//        swipe2refresh.isRefreshing = false
//    }
//
//    override fun displaySettings() {
//        SettingsFragment.newInstance("", "")
//    }
//
//    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
//        super.setUserVisibleHint(isVisibleToUser)
//        if (isVisibleToUser && view != null) {
//            presenter.onRefresh()
//            adapter.data = UploadsViewData(newD, calculateDiff(BasicDiffCallback(old, newD)))
//            uploadsRecycler.adapter = adapter
//
//            //presenter.onRefresh()
//        }
//    }
//
//    override fun onClick(v: View?) {
//        when (v?.id) {
//            R.id.settingsButton -> {
////                presenter.displayBottomSheetSettings()
//            }
//            R.id.back -> activity?.onBackPressed()
//        }
//    }
//
//    var progressData = ProgressData()
//    var adapter: UploadsAdapter = UploadsAdapter()
//    val old = adapter.data?.data ?: emptyList()
//    var newD = mutableListOf<UploadsPresentation>()
//    override fun updateAdapter(videos: List<SavedVideo>, data: ProgressData?) {
//        if (!videos.isNullOrEmpty()) {
//            newD.add(UploadsPresentation(list = videos, progressData = data ?: ProgressData()))
//        }
//    }
//
//    @Inject
//    lateinit var presenter: UploadsPresenter
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_uploads, container, false)
//    }
//
//    override fun onAttach(context: Context) {
//        AndroidSupportInjection.inject(this)
//        super.onAttach(context)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        presenter.onResume()
//    }
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        presenter.onCreate()
//        settingsButton.setOnClickListener(this)
//        back.setOnClickListener(this)
//        debug.setOnClickListener(this)
//        switchHD.setOnCheckedChangeListener(this)
//        swipe2refresh.setOnRefreshListener(this)
//
//
//        swipe2refresh.setColorSchemeResources(
//            R.color.OT_Orange,
//            R.color.OT_White,
//            android.R.color.black
//        )
//
//        context?.let {
//            uploadsRecycler.layoutManager =
//                CustomLayoutManager(it, LinearLayoutManager.VERTICAL, false)
//        }
//    }
//
//
//    companion object {
//        @JvmStatic
//        fun newInstance() =
//            UploadsFragment()
//
//    }
//}
//
//class UploadsViewData(
//    val data: List<UploadsPresentation>,
//    val diffResult: DiffUtil.DiffResult
//)
//
//data class ProgressData(
//    val start: Int = 0,
//    val end: Int = 0,
//    val isHighQuality: Boolean = false,
//    val id: String = ""
//)
//
//data class UploadsPresentation(
//    val list: List<SavedVideo>,
//    val progressData: ProgressData
//) : ItemSame<UploadsPresentation> {
//    override fun sameAs(same: UploadsPresentation): Boolean {
//        return equals(same)
//    }
//
//    override fun contentsSameAs(same: UploadsPresentation): Boolean {
//        return hashCode() == same.hashCode()
//    }
//}
