package com.itsovertime.overtimecamera.play.onboarding


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseactivity.CustomViewPageAdapter
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_on_boarding.*


class OnBoardingFragment : Fragment(), OnBoardingImpl, View.OnClickListener {
    override fun onClick(v: View?) {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_on_boarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onBoardingViewpager.adapter = CustomViewPageAdapter(childFragmentManager, false)
    }


    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

}
