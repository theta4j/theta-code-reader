package org.theta4j.codereader.sample

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import com.theta360.pluginlibrary.activity.PluginActivity
import kotlinx.android.synthetic.main.activity_preview.*
import org.theta4j.codereader.CorrectedCapturer
import org.theta4j.codereader.CorrectedCapturer.ColorFormat
import java.nio.ByteBuffer

class PreviewActivity : PluginActivity(), CorrectedCapturer.Listener {
    companion object {
        private val TAG = PreviewActivity::class.java.simpleName
    }

    private var capturer: CorrectedCapturer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
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
        preview.setImageBitmap(bmp)
    }
}
