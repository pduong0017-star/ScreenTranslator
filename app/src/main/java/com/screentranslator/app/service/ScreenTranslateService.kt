package com.screentranslator.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.screentranslator.app.ocr.OcrLanguage
import com.screentranslator.app.ocr.OcrManager
import com.screentranslator.app.ocr.TextBlockItem
import com.screentranslator.app.translate.SmartAiTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenTranslateService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager

    // Floating Views
    private var floatingBubbleView: ImageView? = null
    private var resultOverlayView: ComicOverlayView? = null

    // Media Projection
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenDensity = 0
    private var screenWidth = 1080
    private var screenHeight = 2400

    private val ocrManager = OcrManager()
    private var translator = SmartAiTranslator()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        setupScreenMetrics()
        showFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        val apiKey = intent?.getStringExtra(EXTRA_API_KEY) ?: ""

        if (apiKey.isNotBlank()) {
            translator = SmartAiTranslator(apiKey = apiKey)
        }

        if (resultCode != 0 && resultData != null && mediaProjection == null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            setupImageReader()
        }

        return START_STICKY
    }

    private fun setupScreenMetrics() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun showFloatingBubble() {
        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setBackgroundColor(0xCC2196F3.toInt()) // Semi-transparent blue
            setPadding(24, 24, 24, 24)
        }

        val layoutParams = WindowManager.LayoutParams(
            140,
            140,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Drag and click handling
        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(bubble, layoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            onBubbleClicked()
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingBubbleView = bubble
        windowManager.addView(bubble, layoutParams)
    }

    private fun onBubbleClicked() {
        if (resultOverlayView != null) {
            // Dismiss active translation overlay if already visible
            removeOverlay()
            return
        }

        Toast.makeText(this, "Đang chụp màn hình & dịch...", Toast.LENGTH_SHORT).show()

        serviceScope.launch {
            val bitmap = captureScreenshot()
            if (bitmap == null) {
                Toast.makeText(this@ScreenTranslateService, "Không thể chụp màn hình", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Step 1: OCR
            val blocks = ocrManager.recognizeText(bitmap, OcrLanguage.LATIN)
            if (blocks.isEmpty()) {
                Toast.makeText(this@ScreenTranslateService, "Không tìm thấy văn bản nào", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Step 2: Smart Translation
            val originalTexts = blocks.map { it.originalText }
            val translatedTexts = translator.translateBatch(originalTexts, targetLang = "Vietnamese", isComicMode = true)

            blocks.forEachIndexed { i, block ->
                if (i < translatedTexts.size) {
                    block.translatedText = translatedTexts[i]
                }
            }

            // Step 3: Draw in-place comic translation overlay
            showComicOverlay(blocks)
        }
    }

    private suspend fun captureScreenshot(): Bitmap? = withContext(Dispatchers.IO) {
        val image = imageReader?.acquireLatestImage() ?: return@withContext null
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            image.close()
        }
    }

    private fun showComicOverlay(items: List<TextBlockItem>) {
        removeOverlay()

        val overlay = ComicOverlayView(this, items) {
            removeOverlay()
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        resultOverlayView = overlay
        windowManager.addView(overlay, layoutParams)
    }

    private fun removeOverlay() {
        resultOverlayView?.let {
            windowManager.removeView(it)
            resultOverlayView = null
        }
    }

    private fun startForegroundNotification() {
        val channelId = "screen_trans_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Translator Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screen Translator")
            .setContentText("Bong bóng dịch đang sẵn sàng")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()

        startForeground(101, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        floatingBubbleView?.let { windowManager.removeView(it) }
        removeOverlay()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_API_KEY = "extra_api_key"
    }
}

/**
 * Comic View: covers original speech bubbles with a clean white/dark box
 * and renders translated Vietnamese text directly on top.
 */
class ComicOverlayView(
    context: Context,
    private val items: List<TextBlockItem>,
    private val onDismiss: () -> Unit
) : View(context) {

    private val bgPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 240
    }

    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 34f
        isAntiAlias = true
        isFakeBoldText = true
    }

    init {
        setOnClickListener {
            onDismiss()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (item in items) {
            val box = item.boundingBox
            // Draw clean background bubble over original text
            canvas.drawRoundRect(
                box.left.toFloat(),
                box.top.toFloat(),
                box.right.toFloat(),
                box.bottom.toFloat(),
                12f,
                12f,
                bgPaint
            )
            canvas.drawRoundRect(
                box.left.toFloat(),
                box.top.toFloat(),
                box.right.toFloat(),
                box.bottom.toFloat(),
                12f,
                12f,
                borderPaint
            )

            // Render translated text
            val text = item.translatedText.ifEmpty { item.originalText }
            canvas.drawText(
                text,
                (box.left + 10).toFloat(),
                (box.top + box.height() / 2 + 10).toFloat(),
                textPaint
            )
        }
    }
}
