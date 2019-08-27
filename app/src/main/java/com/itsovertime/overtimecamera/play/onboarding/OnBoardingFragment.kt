package com.itsovertime.overtimecamera.play.onboarding


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseactivity.CustomViewPageAdapter
import com.itsovertime.overtimecamera.play.baseactivity.OnboardData
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_on_boarding.*


class OnBoardingFragment : Fragment(), OnBoardingImpl, View.OnClickListener {
    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.button -> {
                when (UserPreference.isSignUpComplete) {
                    false -> {
                        when (fieldsVisible) {
                            true -> {
                                callback?.onButtonClicked(
                                    nameInput.text.toString(),
                                    locInput.text.toString()
                                )
                                fieldsVisible = false
                            }
                            false -> {
                                callback?.onButtonClicked()
                            }
                        }
                    }
                    true -> {
                        println("True..")
                        callback?.checkStatus()
                    }
                }
            }
        }
    }


    var fieldsVisible: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_on_boarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val section = arguments?.getParcelable<OnboardData>(ARG_SECTION_NUMBER) ?: return

        header.setText(section.header)
        body.setText(section.body)
        button.setText(section.buttonText)

        when (section.displayEditTextFields) {
            true -> {
                fieldsVisible = true
                nameInput.visibility = View.VISIBLE
                locInput.visibility = View.VISIBLE
            }
            else -> {
                nameInput.visibility = View.GONE
                locInput.visibility = View.GONE
            }
        }

        when (section.displayBottomText) {
            true -> footer.setText(section.bottomText ?: 0)
            else -> {
            }
        }

        button.setOnClickListener(this)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    private var callback: NextPageClick? = null
    fun setUploadsClickListener(listener: NextPageClick) {
        this.callback = listener
    }


    interface NextPageClick {
        fun onButtonClicked(name: String? = null, city: String? = null)
        fun checkStatus()
    }


    companion object {
        private val ARG_SECTION_NUMBER = "section_number"


        fun newInstance(sectionNumber: OnboardData): OnBoardingFragment {
            return OnBoardingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

}
