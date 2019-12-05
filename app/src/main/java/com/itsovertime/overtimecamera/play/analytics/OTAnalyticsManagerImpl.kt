package com.itsovertime.overtimecamera.play.analytics

import android.content.Context
import android.os.PowerManager
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.deviceid.DeviceId
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.network.Api
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

class OTAnalyticsManagerImpl(val context: OTApplication, val api: Api) : OTAnalyticsManager {

    var mixpanelKey: String? = ""
    var mixpanelAPI: MixpanelAPI? = null


    override fun initMixpanel(cntx: Context, userId: String?) {
        mixpanelKey = when (BuildConfig.DEBUG) {
            true -> context.getString(R.string.MXP_TOKEN_BETA)
            else -> context.getString(R.string.MXP_TOKEN)
        }

        mixpanelAPI = MixpanelAPI.getInstance(cntx, mixpanelKey)
        val props = JSONObject()
        props.put("token", mixpanelKey)
        props.put("distinct_id", userId)
        MixpanelAPI.getInstance(context, mixpanelKey).registerSuperPropertiesOnce(props)
        MixpanelAPI.getInstance(context, mixpanelKey).people.identify(userId)
    }

    override fun onTrackDeviceThermalStatus(cntx: Context) {

//        val thermalProps = JSONObject()
//
////        thermalProps.put("Thermal_State")
////        thermalProps.put()
    }

    override fun onDestroyMixpanel() {
        mixpanelAPI?.flush()
    }

    override fun onTrackSelectedEvent(event: Event?) {
        val properties = JSONObject()
        properties.put("event_name", event?.name ?: "N/A")
        properties.put("event_id", event?.id ?: "N/A")
        mixpanelAPI?.track("Selected Event", properties)
    }

    override fun onTrackFirstAppOpen() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTrackCameraRecording() {

        mixpanelAPI?.track("Started Recording")
    }

    override fun onTrackFailedToCreateFile() {
        mixpanelAPI?.track("Captured video without file")
    }

    override fun onTrackVideoFileCreated(savedVideo: SavedVideo?) {
        val properties = JSONObject()
        properties.put("video.client_id", savedVideo?.clientId)
        properties.put("video.is_selfie", savedVideo?.is_selfie)
        mixpanelAPI?.track("Created Video", properties)
    }

    override fun onTrackUploadEvent(uploadEvent: String, uploadProperties: Array<String>) {
        val properties = JSONObject()
        properties.put(uploadEvent, uploadProperties)
        mixpanelAPI?.track(uploadEvent, properties)
    }

    override fun onTrackTrim(properties: Array<String>) {

    }
}

class UploadProperties(
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
