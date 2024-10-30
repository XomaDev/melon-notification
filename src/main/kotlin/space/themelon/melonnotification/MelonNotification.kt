@file:Suppress("FunctionName", "unused")

package space.themelon.melonnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigPictureStyle
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.appinventor.components.annotations.DesignerComponent
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.PropertyCategory
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleObject
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.annotations.UsesBroadcastReceivers
import com.google.appinventor.components.annotations.UsesPermissions
import com.google.appinventor.components.annotations.androidmanifest.ReceiverElement
import com.google.appinventor.components.common.ComponentCategory
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.errors.YailRuntimeError
import com.google.appinventor.components.runtime.util.JsonUtil
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import space.themelon.melonnotification.ImageHelper.getBitmap
import kotlin.random.Random

@UsesBroadcastReceivers(
  receivers = [
    ReceiverElement(
      name = "space.themelon.melonnotification.ItooBackgroundProcedureReceiver",
      exported = "false"
    )
  ]
)
@UsesPermissions(
  permissionNames = "android.permission.POST_NOTIFICATIONS, " +
      "android.permission.SYSTEM_ALERT_WINDOW"
)
@DesignerComponent(
  version = 1,
  category = ComponentCategory.EXTENSION,
  nonVisible = true,
  iconName = "https://github.com/XomaDev/project_assets/blob/main/bell.png?raw=true"
)
@SimpleObject(external = true)
class MelonNotification(form: Form) : AndroidNonvisibleComponent(form) {

  init {
    FrameworkWrapper.activateItooX()

    val procedureDispatcher = ProcedureDispatchReceiver()
    var registered = false
    form.registerForOnResume {
      // listen to procedure dispatch requests, the user wants it this way
      if (!registered) {
        ContextCompat.registerReceiver(
          form,
          procedureDispatcher,
          IntentFilter(PROCEDURE_DISPATCHER_ACTION),
          ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registered = true
      }
    }

    form.registerForOnPause {
      if (registered) {
        form.unregisterReceiver(procedureDispatcher)
        registered = false
      }
    }
  }

  private val manager =
    form.getSystemService(NotificationManager::class.java) as NotificationManager

  private var channel = "Default channel"

  private var configure: (NotificationCompat.Builder) -> Unit = { builder ->
    builder.setSmallIcon(android.R.drawable.ic_dialog_alert)
  }

  private var dynamicConfig = ArrayList<(NotificationCompat.Builder) -> Unit>()
  private var extras = Bundle()

  @DesignerProperty(
    editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
    defaultValue = "False"
  )
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  fun CacheImages(bool: Boolean) {
    ImageHelper.keepCache = bool
  }

  @SimpleFunction(
    description = "Check if the notification permission is granted. " +
        "Always true for devices below Tiramisu."
  )
  fun PermissionGranted(): Boolean {
    return if (ABOVE_TIRAMISU) {
      ContextCompat.checkSelfPermission(
        form, NOTIFICATIONS_PERMISSION
      ) == PackageManager.PERMISSION_GRANTED
    } else true
  }

  @SimpleFunction(description = "Ask for the notifications permission")
  fun AskPermission() {
    if (ABOVE_TIRAMISU) {
      ActivityCompat.requestPermissions(
        form,
        arrayOf(NOTIFICATIONS_PERMISSION),
        4080
      )
    }
  }

  @SimpleFunction
  fun CreateChannel(id: String, name: String, description: String?, importance: Int) {
    if (Build.VERSION.SDK_INT < 26) return // not applicable below oreo
    val channel = NotificationChannel(id, name, importance)
    channel.description = description
    manager.createNotificationChannel(channel)
  }

  @SimpleFunction
  fun Build(title: String, text: String, icon: String) {
    val iconBitmap = getBitmap("Build", icon, false)
    configure = { builder ->
      builder.setContentTitle(title)
      builder.setContentText(text)

      builder.setSmallIcon(IconCompat.createWithBitmap(iconBitmap.get()))
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

  @SimpleProperty
  fun Intent(intent: Any?) {
    if (intent !is PendingIntent) {
      throw YailRuntimeError(
        "Please use the `CreateIntent` block to set the `Intent` property",
        TAG
      )
    }
    dynamicConfig += { it.setContentIntent(intent) }
  }

  @SimpleFunction
  fun CreateIntent(
    name: String,
    startValue: String
  ): Any {
    val className = if (name.contains('.')) name else form.packageName + name
    extras.putString("class", className)
    extras.putString("value", startValue)

    val clazz = Class.forName(className)
    val intent = Intent(form, clazz).apply {
      putExtra("name", name) // preserve original name
      putExtra("startValue", startValue)
      putExtra("APP_INVENTOR_START", startValue)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return if (BroadcastReceiver::class.java.isAssignableFrom(clazz)) {
      PendingIntent.getBroadcast(
        form,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        form,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE
      )
    }
  }

  @SimpleFunction(
    description = "Create an procedure intent that will be called once a notification action is performed. " +
        "Itoo must be present to use this feature. If `callOnMain` is true, procedure " +
        "is always called normally, and application must be running to receive it."
  )
  fun CreateItooIntent(
    screen: String,
    procedure: String,
    arguments: YailList,
    alwaysOnMain: Boolean,
  ): Any {
    if (!FrameworkWrapper.isItooXPresent) {
      throw YailRuntimeError(
        "Please add Itoo extension to your project for " +
            "`CreateItooIntent` block to work", TAG
      )
    }
    val intent = Intent(form, ItooBackgroundProcedureReceiver::class.java).apply {
      putExtra("screen", screen)
      putExtra("procedure", procedure)
      putExtra("arguments", JsonUtil.getJsonRepresentation(arguments))
      putExtra("callOnMain", alwaysOnMain)
    }
    return PendingIntent.getBroadcast(
      form,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_IMMUTABLE
    )
  }

  /**
   * TODO:
   * We would have to actually handle the user's input text fields when the action is triggered,
   * possibly we may have to use Itoo with it to process operation it in background
   */
  @SimpleFunction
  fun AddAction(
    icon: Any,
    title: String,
    intent: Any?,
    isContextual: Boolean,
    authRequired: Boolean,
    allowGeneratedReplies: Boolean,
    showUserInterface: Boolean,
  ) {
    val iconBitmapFuture = getBitmap("AddAction", icon, true)
    if (intent !is PendingIntent) {
      throw YailRuntimeError(
        "Please use the `CreateIntent` block to set the " +
            "`intent` property for `AddAction`", TAG
      )
    }
    dynamicConfig += {
      val iconBitmap = iconBitmapFuture.get()
      it.addAction(
        NotificationCompat.Action.Builder(
          if (iconBitmap != null) IconCompat.createWithBitmap(iconBitmap) else null,
          title,
          intent
        ).apply {
          setShowsUserInterface(showUserInterface)
          setAllowGeneratedReplies(allowGeneratedReplies)
          setContextual(isContextual)
          setAuthenticationRequired(authRequired)
        }.build()
      )
    }
  }

  @SimpleFunction
  fun BigTextStyle(
    text: String,
    contentTitle: String,
    summaryText: String
  ) {
    dynamicConfig += {
      it.setStyle(
        BigTextStyle()
          .bigText(text)
          .setBigContentTitle(contentTitle)
          .setSummaryText(summaryText)
      )
    }
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
          .bigLargeIcon(largeIconBitmap.get())
          .bigPicture(bigPictureBitmap.get())
          .setContentDescription(contentDescription)
          .setBigContentTitle(contentTitle)
          .setSummaryText(summaryText)
          .showBigPictureWhenCollapsed(showBigPictureWhenCollapsed)
      )
    }
  }

  @SimpleFunction
  fun InboxStyle(
    lines: YailList,
    title: String,
    summaryText: String
  ) {
    dynamicConfig += {
      it.setStyle(NotificationCompat.InboxStyle().also { style ->
        lines.toStringArray().forEach { line ->
          style.addLine(line)
        }
        style.setBigContentTitle(title)
        style.setSummaryText(summaryText)
      })
    }
  }

  @SimpleFunction
  fun CreatePerson(
    bot: Boolean,
    personIcon: Any,
    important: Boolean,
    personId: String,
    personName: String,
    uri: String,
  ): Any {
    val personBitmap = getBitmap("MessagingStyle", personIcon, false)
    return Person.Builder()
      .setBot(bot)
      .setIcon(IconCompat.createWithBitmap(personBitmap.get()))
      .setImportant(important)
      .setKey(personId)
      .setName(personName)
      .setUri(uri.trim().ifEmpty { null })
      .build()
  }

  @SimpleFunction
  fun CreateMessage(
    person: Any,
    message: String,
    timestamp: Long,
    historic: Boolean
  ): Any {
    if (person !is Person) {
      throw YailRuntimeError("Please use CreatePerson block for the person parameter", TAG)
    }
    return MessagingStyle.Message(message, timestamp, person).also {
      it.extras.putBoolean("historic", historic)
    }
  }

  @SimpleFunction
  fun MessagingStyle(
    person: Any,
    messages: YailList,
    conversationTitle: String,
    groupConversation: Boolean,
  ) {
    if (person !is Person) {
      throw YailRuntimeError("Please use CreatePerson block for the person parameter", TAG)
    }
    dynamicConfig += {
      it.setStyle(
        MessagingStyle(person)
          .setConversationTitle(conversationTitle)
          .setGroupConversation(groupConversation).also { style ->
            messages.toArray().forEach { message ->
              if (message !is MessagingStyle.Message) {
                throw YailRuntimeError("Please use the CreateMessage block to supply " +
                    "a list of messages for the messages parameter", TAG)
              }
              val historic = message.extras.getBoolean("historic")
              if (historic) style.addHistoricMessage(message)
              else style.addMessage(message)
            }
          }
      )
    }
  }

  @SimpleProperty
  fun LargeIcon(largeIcon: Any) {
    val bitmap = getBitmap("LargeIcon", largeIcon, false).get()
    dynamicConfig += { it.setLargeIcon(bitmap) }
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
    dynamicConfig.clear()
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
  fun ActiveNotificationsIds(): YailList =
    YailList.makeList(manager.activeNotifications.map { it.id })

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

  companion object {
    const val TAG = "MelonNotification"
    const val PROCEDURE_DISPATCHER_ACTION = "MelonNotificationProcedureDispatch"

    private val ABOVE_TIRAMISU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private const val NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
  }
}