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

        val target = createImageFile(context, prefix, extension)

        return try {
            resolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            uriForFile(context, target).toString()
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    fun createImageFile(context: Context, prefix: String, extension: String = "jpg"): File {
        val imagesDir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        val filename = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        return File(imagesDir, filename)
    }

    fun uriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
