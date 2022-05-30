package com.banuba.sdk.example.ivs

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.banuba.sdk.entity.RecordedVideoInfo
import com.banuba.sdk.manager.IEventCallback
import com.banuba.sdk.types.Data

class CameraEventCallback(private val context: Context) : IEventCallback {

    override fun onCameraOpenError(error: Throwable) {
        Toast.makeText(
            context,
            " CameraOpenError = " + error.message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCameraStatus(opened: Boolean) {
        if (opened) {
            Toast.makeText(
                context,
                " Camera Opened",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onScreenshotReady(bmp: Bitmap) {}

    override fun onHQPhotoReady(bmp: Bitmap) {}

    override fun onVideoRecordingFinished(info: RecordedVideoInfo) {}

    override fun onVideoRecordingStatusChange(p0: Boolean) {}

    override fun onImageProcessed(bmp: Bitmap) {}

    override fun onFrameRendered(data: Data, width: Int, height: Int) {}
}