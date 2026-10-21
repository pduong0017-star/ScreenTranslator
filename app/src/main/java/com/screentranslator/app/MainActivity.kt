package com.screentranslator.app

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.screentranslator.app.service.ScreenTranslateService
import com.screentranslator.app.ui.LiquidGlassScreen

class MainActivity : ComponentActivity() {

    private var geminiApiKey by mutableStateOf("")
    private var selectedMode by mutableStateOf("Truyện Tranh 📖")
    private var isServiceActive by mutableStateOf(false)

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenTranslateService::class.java).apply {
                putExtra(ScreenTranslateService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenTranslateService.EXTRA_RESULT_DATA, result.data)
                putExtra(ScreenTranslateService.EXTRA_API_KEY, geminiApiKey.trim())
                putExtra(ScreenTranslateService.EXTRA_IS_COMIC_MODE, selectedMode.contains("Truyện"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isServiceActive = true
            Toast.makeText(this, "Aura Translate: Đã sẵn sàng!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Cần chọn 'Toàn bộ màn hình' để dịch truyện", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("translator_prefs", Context.MODE_PRIVATE)
        geminiApiKey = prefs.getString("api_key", "") ?: ""

        setContent {
            LiquidGlassScreen(
                apiKey = geminiApiKey,
                isServiceRunning = isServiceActive,
                selectedMode = selectedMode,
                onModeChanged = { selectedMode = it },
                onApiKeyChanged = {
                    geminiApiKey = it
                    prefs.edit().putString("api_key", it).apply()
                },
                onStartService = { checkPermissionsAndStart() },
                onStopService = {
                    stopService(Intent(this, ScreenTranslateService::class.java))
                    isServiceActive = false
                    Toast.makeText(this, "Đã tắt Aura Translate", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác'", Toast.LENGTH_LONG).show()
            return
        }

        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
