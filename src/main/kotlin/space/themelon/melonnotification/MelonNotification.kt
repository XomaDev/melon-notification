@file:Suppress("FunctionName", "unused")

package space.themelon.melonnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigPictureStyle
import androidx.core.graphics.drawable.IconCompat
import com.google.appinventor.components.annotations.*
import com.google.appinventor.components.common.ComponentCategory
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.Image
import com.google.appinventor.components.runtime.errors.YailRuntimeError
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import gnu.text.FilePath
import java.io.FileInputStream
import kotlin.random.Random

@UsesPermissions(
  permissionNames = "android.permission.POST_NOTIFICATIONS, " +
      "android.permission.SYSTEM_ALERT_WINDOW"
)
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

  private var dynamicConfig = ArrayList<(NotificationCompat.Builder) -> Unit>()
  private var extras = Bundle()

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
    dynamicConfig += { it.setSubText(subtext) }
  }

  @SimpleProperty
  fun ShowTimestamp(bool: Boolean) {
    dynamicConfig += { it.setShowWhen(bool) }
  }

  @SimpleFunction
  fun StartValue(screen: String, value: String) {
    val clazz = if (screen.contains('.')) screen else form.packageName + screen
    val intent = Intent(clazz).apply {
      putExtra("APP_INVENTOR_START", value)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val pd = PendingIntent.getActivity(
      form,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_IMMUTABLE
    )
    extras.putString("clazz", clazz)
    extras.putString("value", value)

    dynamicConfig += { it.setContentIntent(pd) }
  }

  @SimpleFunction
  fun BigPictureStyle(
    largeIcon: Any,
    bigPicture: Any,
    contentDescription: String,
    contentTitle: String,
    summaryText: String,
    showBigPictureWhenCollapsed: Boolean
  ) {
    val largeIconBitmap = getBitmap("BigPictureStyle[.largeIcon]", largeIcon, true)
    val bigPictureBitmap = getBitmap("BigPictureStyle[.bigPicture]", bigPicture, true)
    dynamicConfig += {
      it.setStyle(
        BigPictureStyle()
          .bigLargeIcon(largeIconBitmap)
          .bigPicture(bigPictureBitmap)
          .setContentDescription(contentDescription)
          .setBigContentTitle(contentTitle)
          .setSummaryText(summaryText)
          .showBigPictureWhenCollapsed(showBigPictureWhenCollapsed)
      )
    }
  }

  @SimpleProperty
  fun LargeIcon(largeIcon: Any) {
    val bitmap = getBitmap("LargeIcon", largeIcon, false)
    dynamicConfig += { it.setLargeIcon(bitmap) }
  }

  private fun getBitmap(function: String, resource: Any, nullable: Boolean): Bitmap? = when (resource) {
    is Image -> ((resource.view as ImageView).drawable as BitmapDrawable).bitmap
    is FilePath, is String, is Uri -> {
      val path = resource.toString()
      if (path.isEmpty()) {
        if (nullable) null
        else throw YailRuntimeError("Empty image resource provided for '$function'", TAG)
      } else if (path.contains('/')) {
        BitmapFactory.decodeFile(path)
      } else {
        form.openAsset(path).use { BitmapFactory.decodeStream(it) }
      }
    }

    else -> throw YailRuntimeError(
      "Expected a URI or a file path or an Image component for LargeIcon," +
          " but got ${resource.javaClass.simpleName} for '$function'", TAG
    )
  }

  @SimpleProperty
  fun AutoCancel(bool: Boolean) {
    dynamicConfig += { it.setAutoCancel(bool) }
  }

  @SimpleProperty
  fun AlertOnce(bool: Boolean) {
    dynamicConfig += { it.setOnlyAlertOnce(bool) }
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
    dynamicConfig.forEach { it(builder) }
    builder.setExtras(extras)
    extras = Bundle()
    return builder.build()
  }

  @SimpleFunction
  fun Post(id: Int) {
    manager.notify(if (0 >= id) Random.nextInt() else id, build())
  }

  @SimpleFunction
  fun Cancel(id: Int) {
    manager.cancel(id)
  }

  @SimpleFunction
  fun CancelAll() {
    manager.cancelAll()
  }

  @SimpleProperty
  fun ActiveNotificationsIds(): YailList = YailList.makeList(manager.activeNotifications.map { it.id })

  @SimpleFunction
  fun NotificationInfo(id: Int, ifNotFound: Any): Any {
    val info = manager.activeNotifications.find { it.id == id } ?: return ifNotFound
    val table = HashMap<Any, Any?>().apply {
      put("postTime", info.postTime)
      val extras = info.notification.extras
      put("class", extras.getString("clazz"))
      put("value", extras.getString("value"))
    }
    return YailDictionary.makeDictionary(table)
  }
}