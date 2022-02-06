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

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import com.theta360.pluginlibrary.activity.PluginActivity
import org.theta4j.codereader.CorrectedCapturer
import org.theta4j.codereader.CorrectedCapturer.ColorFormat
import org.theta4j.codereader.sample.databinding.ActivityPreviewBinding
import java.nio.ByteBuffer

class PreviewActivity : PluginActivity(), CorrectedCapturer.Listener {
    companion object {
        private val TAG = PreviewActivity::class.java.simpleName
    }

    private lateinit var binding: ActivityPreviewBinding

    private var capturer: CorrectedCapturer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        Thread.setDefaultUncaughtExceptionHandler { _, e -> Log.e(TAG, "Uncaught Exception", e) }

        notificationCameraClose()

        capturer = CorrectedCapturer(applicationContext, ColorFormat.GRAY_ARGB_8888).apply {
            setListener(this@PreviewActivity)
            start()
        }

        notificationAudioMovStart()
    }

    override fun onPause() {
        super.onPause()

        notificationAudioMovStop()

        capturer?.run {
            stop()
            release()
        }
        capturer = null

        notificationCameraOpen()

        Thread.setDefaultUncaughtExceptionHandler(null)
    }

    override fun onFrame(data: ByteArray, width: Int, height: Int) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        binding.preview.setImageBitmap(bmp)
    }
}
