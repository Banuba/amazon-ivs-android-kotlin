package com.banuba.sdk.example.ivs

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.ivs.broadcast.AudioDevice
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.ImagePreviewView
import com.banuba.sdk.example.ivs.amazon.*
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.manager.BanubaSdkTouchListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MASK_NAME = "TrollGrandma"

        private const val REQUEST_CODE_PERMISSIONS = 1001

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val bnbSdkManager by lazy(LazyThreadSafetyMode.NONE) {
        BanubaSdkManager(applicationContext)
    }

    private val bnbTouchListener by lazy {
        BanubaSdkTouchListener(this, bnbSdkManager.effectPlayer)
    }

    private val bnbSdkEventCallback = CameraEventCallback(this)

    private lateinit var viewModel: CustomSourceViewModel

    private var imagePreviewView: ImagePreviewView? = null
    private var audioRecorder: AudioRecorder? = null


    private val maskUri by lazy(LazyThreadSafetyMode.NONE) {
        Uri.parse(BanubaSdkManager.getResourcesBase())
            .buildUpon()
            .appendPath("effects")
            .appendPath(MASK_NAME)
            .build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bnbSdkManager.apply {
            effectManager.loadAsync(maskUri.toString())
            setCallback(bnbSdkEventCallback)
        }

        viewModel =
            ViewModelProvider(this).get(CustomSourceViewModel::class.java)

        viewModel.preview.observe(this) {
            Log.d(IVS_TAG, "Texture view changed: $it")
            previewView.addView(it)
            imagePreviewView = it
        }

        viewModel.clearPreview.observe(this) { clear ->
            Log.d(IVS_TAG, "Texture view cleared")
            if (clear) {
                previewView.removeAllViews()
                previewView.setOnTouchListener(null)
            }
        }

        viewModel.indicatorColor.observe(this) { color ->
            Log.d(IVS_TAG, "Indicator color changed")
            statusIndicator.background.colorFilter =
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }

        viewModel.errorHappened.observe(this) { error ->
            Log.d(IVS_TAG, "Error dialog is shown: ${error.first}, ${error.second}")
            showErrorDialog(error.first, error.second)
        }

        viewModel.disconnectHappened.observe(this) {
            Log.d(IVS_TAG, "Disconnect happened")
            endSession()
        }

        btnStart.setOnClickListener {
            createSessionWithUISync()
        }

        btnEnd.setOnClickListener {
            endSessionWithUISync()
        }

        previewView.setOnTouchListener(bnbTouchListener)

        btnStart.enable(checkAllPermissionsGranted())
    }

    private fun createSessionWithUISync() {
        bnbSdkManager.openCamera()
        btnStart.hide()
        statusIndicator.show()
        btnEnd.show()
        createSession {
            startSession()
        }
    }


    private fun endSessionWithUISync() {
        viewModel.session?.stop()
        btnStart.show()
        statusIndicator.hide()
        btnEnd.hide()
    }

    private fun createSession(onReady: () -> Unit) {
        viewModel.createSession {
            onReady()
        }
    }


    private fun startSession() {
        try {

            viewModel.session?.start(AMAZON_IV_ENDPOINT, AMAZON_IV_KEY)
            attachCustomSources()
            viewModel.displayPreview()
        } catch (e: BroadcastException) {
            e.printStackTrace()
            launchMain {
                Log.d(IVS_TAG, "Error dialog is shown: ${e.code}, ${e.detail}")
                showErrorDialog(e.code.toString(), e.detail)
            }
            endSession()
        }
    }

    private fun attachCustomSources() {
        Log.d(IVS_TAG, "Attaching custom sources")

        viewModel.session?.createImageInputSource()?.let { surfaceSource ->

            bnbSdkManager.apply {
                attachSurface(surfaceSource.inputSurface)
                onSurfaceCreated()
                onSurfaceChanged(0, STREAM_PARAMS_WIDTH, STREAM_PARAMS_HEIGHT)
            }
        }
        attachCustomMicrophone()
    }

    private fun attachCustomMicrophone() {
        // Most of the logic for appending audio data from the custom microphone to the
        // broadcast session is in the AudioRecorder class, created below.
        AudioRecorder(applicationContext).apply {
            audioRecorder = this

            val sampleRate = when (this.sampleRate) {
                8000 -> BroadcastConfiguration.AudioSampleRate.RATE_8000
                16000 -> BroadcastConfiguration.AudioSampleRate.RATE_16000
                22050 -> BroadcastConfiguration.AudioSampleRate.RATE_22050
                44100 -> BroadcastConfiguration.AudioSampleRate.RATE_44100
                48000 -> BroadcastConfiguration.AudioSampleRate.RATE_48000
                else -> BroadcastConfiguration.AudioSampleRate.RATE_44100
            }

            val format = when (this.bitDepth) {
                16 -> AudioDevice.Format.INT16
                32 -> AudioDevice.Format.FLOAT32
                else -> AudioDevice.Format.INT16
            }

            // Create a AudioDevice to receive the custom audio, using the configurations determined above.
            // In this case, configuration is hardcoded in the AudioRecorder class.
            viewModel.session?.createAudioInputSource(this.channels, sampleRate, format)
                ?.let { audioDevice ->
                    // Start streaming data from the microphone to the AudioDevice.
                    this.start(audioDevice)
                }
        }
    }


    override fun onStart() {
        super.onStart()
        if (!checkAllPermissionsGranted()) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        bnbSdkManager.effectPlayer.apply {
            effectManager()?.setEffectVolume(0F)
            playbackPlay()
        }
    }

    override fun onPause() {
        super.onPause()
        bnbSdkManager.effectPlayer.playbackPause()
    }

    override fun onStop() {
        super.onStop()
        endSession()
        bnbSdkManager.releaseSurface()
    }


    override fun onDestroy() {
        super.onDestroy()
        previewView.setOnTouchListener(null)
    }

    private fun endSession() {
        Log.d(IVS_TAG, "Session ended")
        audioRecorder?.release()
        previewView.removeAllViews()
        bnbSdkManager.clearSurface()
        bnbSdkManager.closeCamera()

        imagePreviewView = null
        viewModel.endSession()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (checkAllPermissionsGranted()) {
            btnStart.enable()
        } else {
            Toast.makeText(
                applicationContext,
                "Please grant camera/audio permission.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun checkAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

}