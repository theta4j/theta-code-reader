package org.theta4j.codereader.sample

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.theta360.pluginlibrary.activity.PluginActivity
import com.theta360.pluginlibrary.callback.KeyCallback
import com.theta360.pluginlibrary.receiver.KeyReceiver
import kotlinx.android.synthetic.main.activity_main.*
import org.theta4j.codereader.ThetaQRCodeReader

class MainActivity : PluginActivity(), ThetaQRCodeReader.Listener {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private var reader: ThetaQRCodeReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        Thread.setDefaultUncaughtExceptionHandler { _, e -> Log.e(TAG, "Uncaught Exception", e) }

        // Important!!
        // See https://api.ricoh/docs/theta-plugin-reference/broadcast-intent/#notifying-camera-device-control
        notificationCameraClose()

        reader = ThetaQRCodeReader(applicationContext).apply {
            setListener(this@MainActivity)
            start()
        }
        notificationAudioMovStart()

        setKeyCallback(object : KeyCallback {
            override fun onKeyDown(keyCode: Int, keyEvent: KeyEvent?) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    reader?.run {
                        if (isCapturing) {
                            stop()
                            notificationAudioMovStop()
                        } else {
                            start()
                            notificationAudioMovStart()
                        }
                    }
                }
            }

            override fun onKeyUp(keyCode: Int, keyEvent: KeyEvent?) {
            }

            override fun onKeyLongPress(keyCode: Int, keyEvent: KeyEvent?) {
            }
        })
    }

    override fun onPause() {
        super.onPause()

        reader?.run {
            stop()
            release()
        }
        reader = null

        // Important!!
        // See https://api.ricoh/docs/theta-plugin-reference/broadcast-intent/#notifying-camera-device-control
        notificationCameraOpen()

        Thread.setDefaultUncaughtExceptionHandler(null)
    }

    override fun onResult(cameraDirection: ThetaQRCodeReader.CameraDirection, text: String?) {
        if (text != null) {
            notificationAudioMovStop()
            runOnUiThread { result_text.text = text }
            Log.d(TAG, "FOUND $text")
        } else {
            Log.d(TAG, "NOT FOUND")
        }
    }
}
