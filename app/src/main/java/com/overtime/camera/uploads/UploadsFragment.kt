package com.overtime.camera.uploads

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.overtime.camera.R
import com.overtime.camera.model.SavedVideo
import com.overtime.camera.settings.SettingsFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_uploads.*
import kotlinx.android.synthetic.main.uploads_view_toolbar.*
import javax.inject.Inject

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UploadsFragment : Fragment(), UploadsInt, View.OnClickListener {
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.settingsButton -> {
                val manager = childFragmentManager
                val transaction = manager?.beginTransaction()
                transaction?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction?.setCustomAnimations(R.anim.slide_up, R.anim.slide_out)
                transaction?.replace(R.id.fragContainer, SettingsFragment.newInstance("", ""))?.addToBackStack("settings").commit()
            }
        }
    }

    private var param1: String? = null

    override fun updateAdapter(videos: List<SavedVideo>) {
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
        uploadsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
    //https://admin.itsovertime.com/videos/Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjp7ImlkIjoiOTQ4OS5BIiwicm9sZXMiOltdLCJ1c2VybmFtZSI6InByZXNlbnQtc3F1aXJyZWwifSwiaWF0IjoxNTU3NzU2NzA0fQ.bG1818-QymNw6cTssZtbYo8E6jIHLxD42WpRYbx9vng

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
