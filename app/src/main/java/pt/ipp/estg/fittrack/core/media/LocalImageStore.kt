package pt.ipp.estg.fittrack.core.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID

object LocalImageStore {
    private const val IMAGE_DIR = "images"

    fun copyToLocal(context: Context, source: Uri?, prefix: String): String? {
        if (source == null) return null

        val resolver = context.contentResolver
        val extension = resolver.getType(source)?.let { type ->
            MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        } ?: "jpg"

        val imagesDir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        val filename = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        val target = File(imagesDir, filename)

        return try {
            resolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                target
            ).toString()
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
