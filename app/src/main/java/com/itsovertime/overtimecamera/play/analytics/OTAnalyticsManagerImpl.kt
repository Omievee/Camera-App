package com.itsovertime.overtimecamera.play.analytics

import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.network.Api
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

class OTAnalyticsManagerImpl(val context: OTApplication, val api: Api) : OTAnalyticsManager {

    var mixpanelKey: String? = ""

    var mixpanelAPI: MixpanelAPI =
        MixpanelAPI.getInstance(context, mixpanelKey)

    //
    override fun initMixpanel() {
        mixpanelKey = when (BuildConfig.DEBUG) {
            true -> context.getString(R.string.MXP_TOKEN_BETA)
            else -> context.getString(R.string.MXP_TOKEN)
        }

    }

    override fun onTrackDeviceThermalStatus() {
        //val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
//        val thermalProps = JSONObject()
//
////        thermalProps.put("Thermal_State")
////        thermalProps.put()
    }

    override fun onDestroyMixpanel() {
        mixpanelAPI.flush()
    }

    override fun onTrackUploadEvent(uploadEvent: String, analyticsProperties: AnalyticsProperties) {
        val properties = JSONObject()
        properties.put(uploadEvent, analyticsProperties)
        mixpanelAPI.track(uploadEvent, properties)
    }
}

class AnalyticsProperties(
    val client_id: String? = null,
    val upload_id: String? = null,
    val s3_bucket: String? = null,
    val s3_key: String? = null,
    val upload_quality: String? = null,
    val part_index: Int? = null,
    val part_offset: Int? = null,
    val chunkSize: Int? = null,
    val part_size: Int? = null,
    val total_size: Int? = null,
    val progress: Int? = null,
    val upload_rate: Double? = null,
    val path: String? = null,
    val failed_response: String? = "Unknown Error"
)
