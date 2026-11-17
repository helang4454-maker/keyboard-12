package com.glasstype.keyboard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent

class GlassCaptureService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: GlassCaptureService? = null

        @Volatile
        var snapshot: Bitmap? = null

        @Volatile
        private var lastTry = 0L

        fun capture(onDone: (Bitmap?) -> Unit) {
            val svc = instance
            if (svc == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                onDone(null)
                return
            }
            val now = System.currentTimeMillis()
            if (now - lastTry < 800) {
                onDone(null)
                return
            }
            lastTry = now
            svc.shoot(onDone)
        }
    }

    private fun shoot(onDone: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onDone(null)
            return
        }
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        var out: Bitmap? = null
                        try {
                            val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                            if (hw != null) {
                                val soft = hw.copy(Bitmap.Config.ARGB_8888, false)
                                hw.recycle()
                                if (soft != null) {
                                    val w = 360
                                    val h = (soft.height * (w.toFloat() / soft.width)).toInt().coerceAtLeast(2)
                                    out = Bitmap.createScaledBitmap(soft, w, h, true)
                                }
                            }
                        } catch (_: Throwable) {
                        } finally {
                            try {
                                result.hardwareBuffer.close()
                            } catch (_: Throwable) {
                            }
                        }
                        if (out != null) snapshot = out
                        onDone(out)
                    }

                    override fun onFailure(errorCode: Int) {
                        onDone(null)
                    }
                }
            )
        } catch (_: Throwable) {
            onDone(null)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        snapshot = null
        return super.onUnbind(intent)
    }
}
