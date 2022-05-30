package com.banuba.sdk.example.ivs.amazon

import android.app.Application
import android.graphics.Color
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.amazonaws.ivs.broadcast.*
import com.banuba.sdk.example.ivs.STREAM_PARAMS_HEIGHT
import com.banuba.sdk.example.ivs.STREAM_PARAMS_WIDTH

class CustomSourceViewModel(application: Application) : AndroidViewModel(application) {

    var session: BroadcastSession? = null
    var paused = false

    val preview = MutableLiveData<ImagePreviewView>()
    val clearPreview = MutableLiveData<Boolean>()
    val indicatorColor = MutableLiveData<Int>()
    val errorHappened = MutableLiveData<Pair<String, String>>()
    val disconnectHappened = MutableLiveData<Boolean>()

    private val broadcastListener by lazy {
        (object : BroadcastSession.Listener() {

            override fun onAnalyticsEvent(name: String, properties: String) {
                super.onAnalyticsEvent(name, properties)
                Log.d(IVS_TAG, "Analytics $name - $properties")
            }

            override fun onStateChanged(state: BroadcastSession.State) {
                launchMain {
                    when (state) {
                        BroadcastSession.State.CONNECTED -> {
                            Log.d(IVS_TAG, "Connected state")
                            indicatorColor.value = Color.GREEN
                        }
                        BroadcastSession.State.DISCONNECTED -> {
                            Log.d(IVS_TAG, "Disconnected state")
                            indicatorColor.value = Color.GRAY
                            launchMain { disconnectHappened.value = !paused }
                        }
                        BroadcastSession.State.CONNECTING -> {
                            Log.d(IVS_TAG, "Connecting state")
                            indicatorColor.value = Color.YELLOW
                        }
                        BroadcastSession.State.ERROR -> {
                            Log.d(IVS_TAG, "Error state")
                            indicatorColor.value = Color.RED
                        }
                        BroadcastSession.State.INVALID -> {
                            Log.d(IVS_TAG, "Invalid state")
                            indicatorColor.value = Color.rgb(255, 165, 0) // ORANGE
                        }
                    }
                }
            }

            override fun onAudioStats(peak: Double, rms: Double) {
                super.onAudioStats(peak, rms)
                Log.d(IVS_TAG, "Audio stats received - peak ($peak), rms ($rms)")
            }

            override fun onDeviceRemoved(descriptor: Device.Descriptor) {
                super.onDeviceRemoved(descriptor)
                Log.d(IVS_TAG, "Device removed: ${descriptor.deviceId} - ${descriptor.type}")
            }

            override fun onDeviceAdded(descriptor: Device.Descriptor) {
                super.onDeviceAdded(descriptor)
                Log.d(
                    IVS_TAG,
                    "Device added: ${descriptor.urn} - ${descriptor.friendlyName} - ${descriptor.deviceId} - ${descriptor.position}"
                )
            }

            override fun onError(error: BroadcastException) {
                Log.d(
                    IVS_TAG,
                    "Error is: ${error.detail} Error code: ${error.code} Error source: ${error.source}"
                )
                error.printStackTrace()
                launchMain { errorHappened.value = Pair(error.code.toString(), error.detail) }
            }
        })
    }

    /**
     * Create and start new session
     */
    fun createSession(onReady: () -> Unit = {}) {
        session?.release()

        val config = BroadcastConfiguration().apply {
            // This slot will hold the custom audio and video.
            val slot = BroadcastConfiguration.Mixer.Slot.with {
                it.preferredVideoInput = Device.Descriptor.DeviceType.USER_IMAGE
                it.preferredAudioInput = Device.Descriptor.DeviceType.USER_AUDIO
                it.aspect = BroadcastConfiguration.AspectMode.FILL

                return@with it
            }

            this.mixer.slots = arrayOf(slot)

            this.video.size = BroadcastConfiguration.Vec2(
                STREAM_PARAMS_WIDTH.toFloat(),
                STREAM_PARAMS_HEIGHT.toFloat()
            )
        }

        BroadcastSession(getApplication(), broadcastListener, config, null).apply {
            session = this
            Log.d(IVS_TAG, "Broadcast session ready: $isReady")
            if (isReady) {
                onReady()
            } else {
                Log.d(IVS_TAG, "Broadcast session not ready")
                Toast.makeText(
                    getApplication(),
                    "Couldn't create broadcast session. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
    }

    fun endSession() {
        session?.stop()
        session?.release()
        session = null
    }

    /**
     * Display session's composite preview
     */
    fun displayPreview() {
        Log.d(IVS_TAG, "Displaying composite preview")
        session?.let {
            it.awaitDeviceChanges {
                it.previewView.run {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    clearPreview.value = true
                    preview.value = this
                }
            }
        }
    }
}
