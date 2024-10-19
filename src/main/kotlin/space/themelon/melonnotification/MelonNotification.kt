@file:Suppress("FunctionName", "unused")

package space.themelon.melonnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.appinventor.components.annotations.DesignerComponent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleObject
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.annotations.UsesPermissions
import com.google.appinventor.components.common.ComponentCategory
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.Image
import com.google.appinventor.components.runtime.errors.YailRuntimeError
import gnu.text.FilePath
import java.io.FileInputStream
import kotlin.random.Random

@UsesPermissions(permissionNames = "android.permission.POST_NOTIFICATIONS")
@DesignerComponent(
  version = 1,
  category = ComponentCategory.EXTENSION,
  nonVisible = true
)
@SimpleObject(external = true)
class MelonNotification(form: Form) : AndroidNonvisibleComponent(form) {

  companion object {
    private const val TAG = "MelonNotification"
  }

  private val manager = form.getSystemService(NotificationManager::class.java) as NotificationManager

  private var channel = "Default channel"

  private var configure: (NotificationCompat.Builder) -> Unit = { builder ->
    builder.setSmallIcon(android.R.drawable.ic_dialog_alert)
  }

  private var properties = ArrayList<(NotificationCompat.Builder) -> Unit>()

  @SimpleFunction
  fun CreateChannel(id: String, name: String, description: String?, importance: Int) {
    if (Build.VERSION.SDK_INT < 26) return // not applicable below oreo
    val channel = NotificationChannel(id, name, importance)
    channel.description = description
    manager.createNotificationChannel(channel)
  }

  @SimpleFunction
  fun Build(title: String, text: String, icon: String) {
    val filterIcon = icon.trim()

    configure = { builder ->
      builder.setContentTitle(title)
      builder.setContentText(text)

      when {
        icon.isEmpty() -> builder.setSmallIcon(android.R.drawable.ic_dialog_alert)
        icon.startsWith(':') -> {
          // dynamically access android.R.drawable. field
          val fieldName = filterIcon.substring(1).toLowerCase()
          val iconCode = android.R.drawable::class.java.getField(fieldName).get(null) as Int
          builder.setSmallIcon(iconCode)
        }
        else -> {
          val stream = if (icon.contains('/')) FileInputStream(icon) else form.openAsset(icon)
          stream.use {
            val bytes = it.readAllBytes()
            builder.setSmallIcon(IconCompat.createWithData(bytes, 0, bytes.size))
          }
        }
      }
    }
  }

  @SimpleProperty
  fun Channel(name: String) {
    name.trim().let {
      if (it.isEmpty()) {
        throw YailRuntimeError("Channel Id cannot be empty", TAG)
      }
      this.channel = name
    }
  }

  @SimpleProperty
  fun Subtext(subtext: String) {
    properties += { it.setSubText(subtext) }
  }

  @SimpleProperty
  fun LargeIcon(largeIcon: Any) {
    val bitmap: Bitmap = when (largeIcon) {
      is Image -> ((largeIcon.view as ImageView).drawable as BitmapDrawable).bitmap
      is FilePath, is String, is Uri -> {
        val path = largeIcon.toString()
        if (path.contains('/')) {
          BitmapFactory.decodeFile(path)
        } else {
          form.openAsset(path).use { BitmapFactory.decodeStream(it) }
        }
      }
      else -> throw YailRuntimeError("Expected a URI or a file path or an Image component for LargeIcon," +
          " but got ${largeIcon.javaClass.simpleName}", TAG)
    }
    properties += { it.setLargeIcon(bitmap) }
  }

  @SimpleProperty
  fun AutoCancel(bool: Boolean) {
    properties += { it.setAutoCancel(bool) }
  }

  private fun getChannelId(): String {
    val createdChannels = manager.notificationChannels
    if (createdChannels.isEmpty() || createdChannels.find { it.id == channel } == null) {
      // No channels created or the set channel ID is not created
      CreateChannel(channel, channel, null, NotificationManager.IMPORTANCE_DEFAULT)
    }
    return channel
  }

  private fun build(): Notification {
    val builder = NotificationCompat.Builder(form, getChannelId())
    configure(builder)
    properties.forEach { it(builder) }
    return builder.build()
  }

  @SimpleFunction
  fun Post(id: Int) {
    manager.notify(if (id == -1) Random.nextInt() else id, build())
  }
}