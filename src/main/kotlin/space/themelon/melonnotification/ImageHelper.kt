package space.themelon.melonnotification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.errors.YailRuntimeError
import gnu.text.FilePath
import java.net.URL
import java.util.concurrent.CompletableFuture

object ImageHelper {

  var keepCache = true
  private val bitmapCache = LruCache<String, Bitmap>(20)

  fun getBitmap(
    function: String,
    resource: Any,
    optional: Boolean,
  ): CompletableFuture<Bitmap> {
    when (resource) {
      is FilePath, is String, is Uri -> {
        val asString = resource.toString()
        if (keepCache) {
          bitmapCache[asString]?.let {
            return CompletableFuture.completedFuture(it)
          }
        }
        when {
          asString.isEmpty() -> {
            if (optional) return CompletableFuture.completedFuture(null)
            else throw YailRuntimeError(
              "'$function' expected a valid image resource but got empty string",
              MelonNotification.TAG
            )
          }

          asString.contains('/') -> return CompletableFuture.supplyAsync {
            val bitmap = BitmapFactory.decodeFile(asString)
            bitmapCache.put(asString, bitmap)
            bitmap
          }
          asString.startsWith("url:") -> return CompletableFuture.supplyAsync {
            val bitmap = BitmapFactory.decodeStream(URL(asString).openStream())
            bitmapCache.put(asString, bitmap)
            bitmap
          }
          else -> return CompletableFuture.supplyAsync {
            val bitmap = BitmapFactory.decodeStream(Form.getActiveForm().openAsset(asString))
            bitmapCache.put(asString, bitmap)
            bitmap
          }
        }
      }
      else -> throw YailRuntimeError(
        "'$function' expected a valid iamge resource but got $resource",
        MelonNotification.TAG
      )
    }
  }

}