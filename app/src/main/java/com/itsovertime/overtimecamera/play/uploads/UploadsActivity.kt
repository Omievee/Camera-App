package com.itsovertime.overtimecamera.play.uploads

import android.app.AlertDialog
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_uploads.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.fragment_camera.navSpace
import kotlinx.android.synthetic.main.fragment_uploads.*
import kotlinx.android.synthetic.main.fragment_uploads.swipe2refresh
import kotlinx.android.synthetic.main.fragment_uploads.uploadsRecycler
import kotlinx.android.synthetic.main.upload_item_view.*
import kotlinx.android.synthetic.main.upload_item_view.view.*
import kotlinx.android.synthetic.main.uploads_view_toolbar.*
import javax.inject.Inject


class UploadsActivity : OTActivity(), UploadsInt, View.OnClickListener,
    CompoundButton.OnCheckedChangeListener,
    SwipeRefreshLayout.OnRefreshListener {


    override fun updateMsg() {
        //  uploadsMessage.text = ""
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val displayCutout =
            window.decorView.rootWindowInsets.displayCutout
        if (displayCutout != null) {
            val params = notchSpace.layoutParams as ConstraintLayout.LayoutParams
            params.height = 65
            notchSpace.layoutParams = params
        }
    }

    override fun noVideos() {
        uploadsMessage.text = getString(R.string.uploads_finished)
        uploadsIcon.visibility = View.GONE
    }

    override fun setNoVideosMsg() {
        uploadedText.text = ""
    }

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
        switchHD.isChecked = UserPreference.isChecked
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
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
        determineNavigationSpacing()


    }

    private fun determineNavigationSpacing() {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        val params = navSpace.layoutParams as ConstraintLayout.LayoutParams
        if (resourceId > 0) {
            params.height = resources.getDimensionPixelSize(resourceId)
        } else {
            params.height = 0
        }
        navSpace.layoutParams = params
    }

    override fun setUploadingHdVideo() {
        uploadsMessage.text = getString(R.string.upload_queue_uploading_hd)
        uploadsIcon.setImageResource(R.drawable.upload)
        uploadsIcon.visibility = View.VISIBLE
    }

    override fun setUploadingMedVideo() {
        uploadsMessage.text = getString(R.string.upload_queue_uploading_med)
        uploadsIcon.setImageResource(R.drawable.upload)
        uploadsIcon.visibility = View.VISIBLE
    }

    override fun onNotifyOfPendingHDUploads() {
        uploadsIcon.setImageResource(R.drawable.warning)
        uploadsIcon.visibility = View.VISIBLE
        uploadsMessage.text =
            getString(R.string.hd_videos_ready)

    }


    var isHD: Boolean = false
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

        if (!UserPreference.isChecked) {
            val high = this.list?.find {
                !it.highUploaded
            }
            if (high != null) {
                AlertDialog.Builder(this, R.style.CUSTOM_ALERT).apply {
                    setTitle(getString(R.string.hd_uploads_title))
                    setMessage(getString(R.string.hd_uploads_msg))
                    setCancelable(false)
                }.setPositiveButton(getString(R.string.hd_uploads_button)) { _, _ ->
                    isHD = true
                    UserPreference.isChecked = true
                    presenter.hdSwitchWasChecked(isChecked)
                }.setNegativeButton(getString(R.string.hd_uploads_cancel_button)) { d, i ->
                    switchHD.isChecked = false
                    d.dismiss()
                }.create().show()
            } else {
                switchHD.isChecked = false
                showToast(getString(R.string.no_hd_videos_msg))
            }
        } else {
            switchHD.isChecked = false
            UserPreference.isChecked = false
        }
    }

    private fun showToast(msg: String) {
        val toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        val view = toast.view
        view.background.setColorFilter(resources.getColor(R.color.black, null), PorterDuff.Mode.SRC_IN);
        val text = view.findViewById<TextView>(android.R.id.message)
        text.setTextColor(resources.getColor(R.color.OT_White, null));
        toast.show()

    }

    var prog: Int = 0
    var hd: Boolean = false
    var id: String = ""
    override fun updateProgressBar(id: String, progress: Int, hd: Boolean) {
        this.prog += progress
        this.hd = hd
        this.id = id
        val vid = list?.find {
            it.clientId == this.id
        }
        println("progress prog:: $progress")
        val index = list?.indexOf(vid) ?: 0

        (adapter.holder?.itemView as UploadsView).getChildAt(index)
            .medQProgressBar.setProgress(this.prog, true)
        adapter.notifyItemChanged(index)
    }

    override fun displayNoNetworkConnection() {
        uploadsIcon.setImageResource(R.drawable.warning)
        uploadsMessage.text = "Check network connection..."
    }

    override fun displayWifiReady() {
        uploadsIcon.visibility = View.VISIBLE
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
            R.id.settingsButton -> presenter.displayBottomSheetSettings()
            R.id.back -> onBackPressed()
            R.id.debug -> presenter.updateAdapterForDebug()
        }
    }

    var adapter: UploadsAdapter = UploadsAdapter()
    val old = adapter.data?.data ?: emptyList()
    var newD = mutableListOf<UploadsPresentation>()
    var debugBool: Boolean = false
    var list: List<SavedVideo>? = null
    override fun updateAdapter(videos: List<SavedVideo>, dBug: Boolean, isHD: Boolean) {
        this.list = videos
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

        uploadsRecycler.adapter = adapter
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



