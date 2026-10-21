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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screentranslator.app.service.ScreenTranslateService

class MainActivity : ComponentActivity() {

    private var geminiApiKey by mutableStateOf("")

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenTranslateService::class.java).apply {
                putExtra(ScreenTranslateService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenTranslateService.EXTRA_RESULT_DATA, result.data)
                putExtra(ScreenTranslateService.EXTRA_API_KEY, geminiApiKey.trim())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Đã khởi động bong bóng dịch!", Toast.LENGTH_SHORT).show()
            finish() // Minimize app to background so user can read comics/chats
        } else {
            Toast.makeText(this, "Cần cấp quyền chụp màn hình để dịch", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved API Key from preferences
        val prefs = getSharedPreferences("translator_prefs", Context.MODE_PRIVATE)
        geminiApiKey = prefs.getString("api_key", "") ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        apiKey = geminiApiKey,
                        onApiKeyChanged = {
                            geminiApiKey = it
                            prefs.edit().putString("api_key", it).apply()
                        },
                        onStartService = { checkPermissionsAndStart() },
                        onStopService = {
                            stopService(Intent(this, ScreenTranslateService::class.java))
                            Toast.makeText(this, "Đã tắt dịch màn hình", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        // 1. Check Overlay Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác'", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Request MediaProjection Permission
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}

@Composable
fun MainScreen(
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Screen Translator",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Dịch truyện tranh & tin nhắn thông minh theo ngữ cảnh",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChanged,
            label = { Text("Gemini / AI API Key (Để trống dùng dịch tự do)") },
            placeholder = { Text("AIzaSy...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartService,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "BẬT BONG BÓNG DỊCH", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onStopService,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Tắt bong bóng", color = MaterialTheme.colorScheme.error)
        }
    }
}
