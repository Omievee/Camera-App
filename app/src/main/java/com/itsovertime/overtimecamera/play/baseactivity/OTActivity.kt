package com.itsovertime.overtimecamera.play.baseactivity

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.DaggerAppCompatActivity
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

abstract class OTActivity : DaggerAppCompatActivity(), HasSupportFragmentInjector {

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>


    @Inject
    lateinit var manager: WifiManager

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)


        println("intent?? ${intent?.getBooleanExtra("connected", false)}")

    }

}
