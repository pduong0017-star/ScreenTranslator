package com.screentranslator.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Liquid Glass Palette
val GlassBg = Color(0x2AFFFFFF)
val GlassBorder = Color(0x55FFFFFF)
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFF9D00FF)
val NeonPink = Color(0xFFFF007F)
val DarkNavy = Color(0xFF0D0E1A)
val DeepIndigo = Color(0xFF13152C)

@Composable
fun LiquidGlassScreen(
    apiKey: String,
    isServiceRunning: Boolean,
    selectedMode: String,
    onModeChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var isApiKeyVisible by remember { mutableStateOf(false) }

    // Dynamic background subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkNavy, DeepIndigo, Color(0xFF090A10))
                )
            )
    ) {
        // Ambient background glowing orbs (Liquid aesthetic)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-40).dp)
                .blur(100.dp)
                .background(NeonPurple.copy(alpha = glowAlpha * 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(90.dp)
                .background(NeonCyan.copy(alpha = glowAlpha * 0.35f), CircleShape)
        )

        // Main Glass Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .shadow(elevation = if (isServiceRunning) 8.dp else 0.dp, shape = CircleShape)
                            .background(if (isServiceRunning) NeonCyan else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "ACTIVE OVERLAY" else "STANDBY",
                        color = if (isServiceRunning) NeonCyan else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "AURA TRANSLATE",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Dịch Truyện Tranh & Tin Nhắn Thế Hệ Mới",
                    fontSize = 13.sp,
                    color = Color(0xFFAEB2D5)
                )
            }

            // Mode Selector Pill (Liquid Glass)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val modes = listOf("Truyện Tranh 📖", "Tin Nhắn 💬")
                modes.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val itemModifier = if (isSelected) {
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.8f), NeonPurple.copy(alpha = 0.8f))), RoundedCornerShape(20.dp))
                            .clickable { onModeChanged(mode) }
                            .padding(vertical = 12.dp)
                    } else {
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onModeChanged(mode) }
                            .padding(vertical = 12.dp)
                    }
                    Box(
                        modifier = itemModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color.White else Color(0xFFC4C8E8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Glass Card for API Configuration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(GlassBg)
                    .border(1.5.dp, GlassBorder, RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🧠 Gemini AI Engine",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Dịch thoát ý, tự nhiên theo ngữ cảnh thoại truyện. Để trống sẽ dùng bộ dịch nhanh chuẩn.",
                        color = Color(0xFF9EA3C7),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChanged,
                        placeholder = { Text("Nhập hoặc dán Gemini API Key...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (isApiKeyVisible) "Ẩn" else "Hiện",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable { isApiKeyVisible = !isApiKeyVisible }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x44FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x1A000000),
                            unfocusedContainerColor = Color(0x1A000000)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Neon Glowing Start Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonCyan, NeonPurple, NeonPink)
                            )
                        )
                        .clickable { onStartService() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨ KHỞI ĐỘNG BONG BÓNG",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stop Glass Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1AFF3366))
                        .border(1.dp, Color(0x44FF3366), RoundedCornerShape(20.dp))
                        .clickable { onStopService() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tắt bong bóng dịch",
                        color = Color(0xFFFF6B8B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
