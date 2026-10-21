package com.screentranslator.app.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recognized text block with its bounding box on screen.
 */
data class TextBlockItem(
    val originalText: String,
    val boundingBox: Rect,
    var translatedText: String = ""
)

enum class OcrLanguage {
    LATIN,     // English, Vietnamese, etc.
    CHINESE,   // Manhua
    JAPANESE,  // Manga
    KOREAN     // Manhwa
}

class OcrManager {

    private val latinRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val chineseRecognizer by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    private val japaneseRecognizer by lazy { TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()) }
    private val koreanRecognizer by lazy { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }

    suspend fun recognizeText(bitmap: Bitmap, language: OcrLanguage = OcrLanguage.LATIN): List<TextBlockItem> =
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = when (language) {
                OcrLanguage.LATIN -> latinRecognizer
                OcrLanguage.CHINESE -> chineseRecognizer
                OcrLanguage.JAPANESE -> japaneseRecognizer
                OcrLanguage.KOREAN -> koreanRecognizer
            }

            try {
                val task: Task<Text> = recognizer.process(image)
                val textResult = Tasks.await(task)

                val items = mutableListOf<TextBlockItem>()
                for (block in textResult.textBlocks) {
                    val box = block.boundingBox ?: continue
                    val text = block.text.trim()
                    if (text.isNotEmpty()) {
                        items.add(TextBlockItem(originalText = text, boundingBox = box))
                    }
                }
                items
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
}
