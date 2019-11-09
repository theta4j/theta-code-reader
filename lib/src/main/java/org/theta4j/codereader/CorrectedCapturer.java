package org.theta4j.codereader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * CorrectedCapturer controls THETA camera API.
 * Capture a dual-fisheye image, correct the distortion and pass it to the listener function.
 *
 * <table summary="Camera and Image parameters">
 * <tbody>
 * <tr><th>Format</th><td>Dual-fisheye (Distortion corrected)</td></tr>
 * <tr><th>Width</th><td>3840px</td></tr>
 * <tr><th>Height</th><td>1920px</td></tr>
 * <tr><th>Frames per second</th><td>8</td></tr>
 * <tr><th>Color Format</th><td>8bit Grayscale (default), or 32bit RGBA Grayscale.</td></tr>
 * </tbody>
 * </table>
 */
public final class CorrectedCapturer {
    private static final String TAG = CorrectedCapturer.class.getSimpleName();

    private final ImageConverter imageConverter;

    private Camera mCamera;

    private final int[] mTextureIds = new int[1];

    private SurfaceTexture mTexture;

    private Listener mListener;

    /**
     * Create new {@link CorrectedCapturer} with default color format {@link ColorFormat#GRAY_8}.
     *
     * @param context Android Context
     */
    public CorrectedCapturer(@NonNull final Context context) {
        this(context, ColorFormat.GRAY_8);
    }

    /**
     * Create new {@link CorrectedCapturer} with {@link ColorFormat}.
     *
     * @param context     Android Context
     * @param colorFormat Color format of the captured image data passed to the listener function.
     */
    public CorrectedCapturer(
            @NonNull final Context context,
            @NonNull final ColorFormat colorFormat
    ) {
        Objects.requireNonNull(context, "context can not be null.");
        Objects.requireNonNull(colorFormat, "colorFormat can not be null.");

        this.imageConverter = new ImageConverter(context);

        GLES20.glGenTextures(mTextureIds.length, mTextureIds, 0);
        mTexture = new SurfaceTexture(mTextureIds[0]);

        mCamera = Camera.open();
        final Camera.Parameters params = mCamera.getParameters();
        params.set("RIC_SHOOTING_MODE", "RicMoviePreview3840");
        params.set("RIC_PROC_STITCHING", "RicNonStitching"); // Dual-fisheye
        params.setPreviewSize(3840, 1920);
        params.setPreviewFpsRange(8000, 8000);
        mCamera.setParameters(params);

        try {
            mCamera.setPreviewTexture(mTexture);
        } catch (IOException e) {
            throw new RuntimeException("failed to set surface texture to camera.", e);
        }

        mCamera.setPreviewCallback((data, camera) -> {
            final Listener listener;
            synchronized (CorrectedCapturer.this) {
                if (mListener == null) {
                    return;
                }
                listener = mListener;
            }

            final Camera.Size size = camera.getParameters().getPreviewSize();
            final byte[] corrected;

            if (colorFormat == ColorFormat.GRAY_8) {
                corrected = new byte[size.width * size.height];
                imageConverter.correctDistortion(size.width, size.height, data, corrected);
            } else if (colorFormat == ColorFormat.GRAY_ARGB_8888) {
                corrected = new byte[size.width * size.height * 4];
                imageConverter.correctDistortionRGBA(size.width, size.height, data, corrected);
            } else {
                throw new AssertionError("unreachable");
            }

            listener.onFrame(corrected, size.width, size.height);
        });
    }

    /**
     * Start to capture.
     *
     * @throws IllegalStateException if {@link #release()} method already called.
     */
    public synchronized void start() {
        if (mCamera == null) {
            throw new IllegalStateException("release method already called.");
        }

        mCamera.startPreview();
    }

    /**
     * Stop to capture.
     */
    public synchronized void stop() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * Stop to capture, and release all resources.
     */
    public synchronized void release() {
        mListener = null;

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            try {
                mCamera.setPreviewTexture(null);
            } catch (IOException e) {
                Log.e(TAG, "failed to unset preview texture from camera", e);
                e.printStackTrace();
            }
            mCamera.release();
        }
        mCamera = null;

        if (mTexture != null) {
            mTexture.release();
            mTexture = null;
            GLES20.glDeleteTextures(mTextureIds.length, mTextureIds, 0);
        }
    }

    /**
     * Set listener object.
     *
     * @param listener listener object to set.
     */
    public synchronized void setListener(@Nullable final Listener listener) {
        mListener = listener;
    }

    /**
     * Color format of the captured image data passed to the listener function.
     */
    public enum ColorFormat {
        /**
         * 8 bit grayscale.
         */
        GRAY_8,

        /**
         * 32 bit color format, however actual data is grayscale. Alpha channel is 255.
         * This format is for creating {@link Bitmap} by {@link Bitmap#createBitmap(int, int, Bitmap.Config)}
         */
        GRAY_ARGB_8888,
    }

    /**
     * Listener interface used to supply image data from a camera.
     */
    public interface Listener {
        /**
         * Called when new captured and corrected data is available.
         * Color format is the format specified in the constructor. The default is {@link ColorFormat#GRAY_8}.
         *
         * @param data   Captured and corrected image data.
         * @param width  Width of the image in pixels.
         * @param height height of the image in pixels.
         */
        void onFrame(@NonNull byte[] data, int width, int height);
    }
}
