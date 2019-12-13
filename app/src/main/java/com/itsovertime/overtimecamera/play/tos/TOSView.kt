package com.itsovertime.overtimecamera.play.tos

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import kotlinx.android.synthetic.main.fragment_tos.view.*


class TOSView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet), View.OnClickListener {
    override fun onClick(v: View?) {

    }

    init {
        View.inflate(context, R.layout.fragment_tos, this)
        webview.settings.javaScriptEnabled = true

        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                view?.loadUrl(
                    "javascript:(function() { " +
                            "var foot = document.getElementsByClassName('content')[0].style.display='none'; " +
                            "})()"
                )
            }
        }
        webview.loadUrl(context.getString(R.string.tos_url))
        webview.setOnTouchListener { v, event -> false }
    }
}