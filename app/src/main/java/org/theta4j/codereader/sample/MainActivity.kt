/*
 * Copyright (C) 2022 theta4j project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theta4j.codereader.sample

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.theta360.pluginlibrary.activity.PluginActivity
import com.theta360.pluginlibrary.callback.KeyCallback
import com.theta360.pluginlibrary.receiver.KeyReceiver
import org.theta4j.codereader.ThetaQRCodeReader
import org.theta4j.codereader.sample.databinding.ActivityMainBinding

class MainActivity : PluginActivity(), ThetaQRCodeReader.Listener {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var binding: ActivityMainBinding

    private var reader: ThetaQRCodeReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            runOnUiThread { binding.resultText.text = text }
            Log.d(TAG, "FOUND $text")
        } else {
            Log.d(TAG, "NOT FOUND")
        }
    }
}
