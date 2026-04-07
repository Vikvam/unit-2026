package cz.aaa.unit2026.ui.util

import androidx.compose.ui.graphics.ImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    // Web targets don't have app icons
    return null
}
