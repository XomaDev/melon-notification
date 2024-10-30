package space.themelon.melonnotification

import android.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.LruCache
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.errors.YailRuntimeError
import com.google.appinventor.components.runtime.util.MediaUtil
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

          asString.startsWith("url:") -> return CompletableFuture.supplyAsync {
            val bitmap = BitmapFactory.decodeStream(URL(asString).openStream())
            bitmapCache.put(asString, bitmap)
            bitmap
          }

          asString.startsWith(":") -> return CompletableFuture.supplyAsync {
            val fieldName = asString.trim().substring(1).toLowerCase()
            val iconCode = R.drawable::class.java.getField(fieldName).get(null) as Int
            val drawable = Form.getActiveForm().getDrawable(iconCode)
              ?: throw YailRuntimeError("Cannot find resource icon '$fieldName'", MelonNotification.TAG)
            val bitmap = Bitmap.createBitmap(
              drawable.intrinsicWidth,
              drawable.intrinsicHeight,
              Bitmap.Config.ARGB_8888
            )
            Canvas(bitmap).apply {
              drawable.setBounds(0, 0, this.width, this.height)
              drawable.draw(this)
            }
            bitmapCache.put(asString, bitmap)
            bitmap
          }

          else -> return CompletableFuture.supplyAsync {
            val bitmapDrawable = MediaUtil.getBitmapDrawable(Form.getActiveForm(), asString)
            val bitmap = bitmapDrawable.bitmap
            bitmapCache.put(asString, bitmap)
            bitmap
          }
        }
      }

      else -> throw YailRuntimeError(
        "'$function' expected a valid image resource but got $resource",
        MelonNotification.TAG
      )
    }
  }

}