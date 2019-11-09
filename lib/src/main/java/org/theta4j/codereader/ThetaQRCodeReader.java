package org.theta4j.codereader;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ThetaQRCodeReader} controls THETA camera API, and scans QR code.
 */
public class ThetaQRCodeReader {
    private final Reader reader = new QRCodeReader();

    private final Map<DecodeHintType, Object> decodeHints = new HashMap<>();

    private CorrectedCapturer mCorrectedCapturer;

    private Listener mListener;

    private boolean mIsCapturing = false;

    /**
     * Create new {@link ThetaQRCodeReader}
     *
     * @param context Android Context
     */
    public ThetaQRCodeReader(@NonNull final Context context) {
        mCorrectedCapturer = new CorrectedCapturer(context);
        mCorrectedCapturer.setListener((data, width, height) -> {
            tryDecode(CameraDirection.BACK, data, width, height, width / 2, 0, width / 2, height);
            tryDecode(CameraDirection.FRONT, data, width, height, 0, 0, width / 2, height);
        });
    }

    /**
     * Start to capture.
     *
     * @throws IllegalStateException if {@link #release()} method already called.
     */
    public synchronized void start() {
        if (mCorrectedCapturer == null) {
            throw new IllegalStateException("release method already called.");
        }

        if (mIsCapturing) {
            return;
        }

        mIsCapturing = true;
        mCorrectedCapturer.start();
    }

    /**
     * Stop to capture.
     */
    public synchronized void stop() {
        if (!mIsCapturing) {
            return;
        }

        if (mCorrectedCapturer != null) {
            mCorrectedCapturer.stop();
        }

        mIsCapturing = false;
    }


    /**
     * Stop to capture, and release all resources.
     */
    public synchronized void release() {
        if (mCorrectedCapturer != null) {
            mCorrectedCapturer.release();
        }
        mCorrectedCapturer = null;
    }

    /**
     * Return true if capturing.
     *
     * @return true if capturing.
     */
    public synchronized boolean isCapturing() {
        return mIsCapturing;
    }

    /**
     * Set listener object.
     *
     * @param listener listener object to set.
     */
    public synchronized void setListener(final Listener listener) {
        mListener = listener;
    }

    /**
     * Camera direction.
     */
    public enum CameraDirection {
        /**
         * The camera on the side without buttons or LEDs.
         */
        BACK,

        /**
         * The camera on the side with buttons and LEDs.
         */
        FRONT,
    }

    /**
     * Listener interface used to supply scan result.
     */
    public interface Listener {
        /**
         * Called when scan success.
         *
         * @param cameraDirection With which camera the scan was successful
         * @param text            Scan result.
         */
        void onResult(@NonNull CameraDirection cameraDirection, @Nullable String text);
    }

    private void tryDecode(
            final CameraDirection cameraDirection,
            final byte[] data,
            final int dataWidth, final int dataHeight,
            final int left, final int top, final int width, final int height
    ) {
        final Listener listener;
        synchronized (this) {
            if (!mIsCapturing || mListener == null) {
                return;
            }
            listener = mListener;
        }

        final LuminanceSource src = new PlanarYUVLuminanceSource(data, dataWidth, dataHeight, left, top, width, height, false);
        final BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(src));

        try {
            final Result result = reader.decode(bmp, decodeHints);
            stop();
            listener.onResult(cameraDirection, result.getText());
        } catch (final NotFoundException | FormatException | ChecksumException e) {
            listener.onResult(cameraDirection, null);
        }
    }
}
