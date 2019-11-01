package com.itsovertime.overtimecamera.play.uploads

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseactivity.OTActivity
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
import com.itsovertime.overtimecamera.play.itemsame.ItemSame
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.settings.SettingsFragment
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_uploads.*
import kotlinx.android.synthetic.main.uploads_view_toolbar.*
import javax.inject.Inject


class UploadsActivity : OTActivity(), UploadsInt, View.OnClickListener,
    CompoundButton.OnCheckedChangeListener,
    SwipeRefreshLayout.OnRefreshListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uploads)

        presenter.onCreate()
        settingsButton.setOnClickListener(this)
        back.setOnClickListener(this)
        debug.setOnClickListener(this)
        switchHD.setOnCheckedChangeListener(this)
        swipe2refresh.setOnRefreshListener(this)

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }
        swipe2refresh.setColorSchemeResources(
            R.color.OT_Orange,
            R.color.OT_White,
            android.R.color.black
        )

        uploadsRecycler.layoutManager =
            CustomLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        presenter.onRefresh()
        uploadsRecycler.adapter = adapter

        val itemDecorator = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(
            this, R.drawable.divider
        )?.let {
            itemDecorator.setDrawable(
                it
            )
        }
        uploadsRecycler.addItemDecoration(itemDecorator)

    }

    override fun setUploadingHdVideo() {
        uploadsMessage.text = "Uploading High Quality Videos.."
        uploadsIcon.setImageResource(R.drawable.upload)
    }

    override fun setUploadingMedVideo() {
        uploadsMessage.text = "Uploading Medium Quality Videos.."
        uploadsIcon.setImageResource(R.drawable.upload)
    }

    override fun onNotifyOfPendingHDUploads() {
        uploadsMessage.text =
            "HD videos are ready for upload. Turn on HD uploading. (WiFi Recommended)"
        uploadsIcon.setImageResource(R.drawable.warning)
    }


    var isHD: Boolean = false
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        isHD = true
        presenter.hdSwitchWasChecked(isChecked)
    }

    var prog: Int = 0
    var hd: Boolean = false
    var id: String = ""
    override fun updateProgressBar(id: String, progress: Int, hd: Boolean) {
        this.prog = progress
        this.hd = hd
        this.id = id
        adapter.updateProgress(id, progress, hd)
    }

    override fun displayNoNetworkConnection() {
        uploadsIcon.visibility = View.INVISIBLE
        // uploadsMessage.text = "No active connection..."
    }

    override fun displayWifiReady() {
        uploadsIcon.visibility = View.VISIBLE
        uploadsMessage.text = getString(R.string.upload_queue_uploading_hd)
    }

    override fun onRefresh() {
        presenter.onRefresh()
    }

    override fun swipe2RefreshIsTrue() {
        swipe2refresh.isRefreshing = true
    }

    override fun swipe2RefreshIsFalse() {
        swipe2refresh.isRefreshing = false
    }

    override fun displaySettings() {
        SettingsFragment.newInstance().show(supportFragmentManager, "tag")
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.settingsButton -> {
                presenter.displayBottomSheetSettings()
            }
            R.id.back -> onBackPressed()
            R.id.debug -> {
                presenter.updateAdapterForDebug()
            }
        }
    }

    var adapter: UploadsAdapter = UploadsAdapter()
    val old = adapter.data?.data ?: emptyList()
    var newD = mutableListOf<UploadsPresentation>()
    var debugBool: Boolean = false
    override fun updateAdapter(videos: List<SavedVideo>, dBug: Boolean, isHD: Boolean) {
        this.debugBool = dBug
        this.isHD = isHD
        when (debugBool) {
            true -> debug.setImageResource(R.drawable.debug_click)
            false -> debug.setImageResource(R.drawable.tool)
        }
        if (!videos.isNullOrEmpty()) {
            newD.add(UploadsPresentation(list = videos, debug = dBug, isHD = isHD))
        }
        adapter.data = UploadsViewData(
            newD, DiffUtil.calculateDiff(
                BasicDiffCallback(
                    newD, old
                )
            )
        )
        adapter.updateProgress(id, prog, hd)
        uploadsRecycler.adapter = adapter
        adapter.notifyDataSetChanged()
        (uploadsRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true
    }

    @Inject
    lateinit var presenter: UploadsPresenter

}


class UploadsViewData(
    val data: List<UploadsPresentation>,
    val diffResult: DiffUtil.DiffResult
)

data class UploadsPresentation(
    val list: List<SavedVideo>,
    val debug: Boolean,
    val isHD: Boolean
) : ItemSame<UploadsPresentation> {
    override fun sameAs(same: UploadsPresentation): Boolean {
        return equals(same)
    }

    override fun contentsSameAs(same: UploadsPresentation): Boolean {
        return hashCode() == same.hashCode()
    }
}



