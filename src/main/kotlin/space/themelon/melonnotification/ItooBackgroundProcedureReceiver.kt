package space.themelon.melonnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.appinventor.components.runtime.util.JsonUtil
import java.util.ArrayList

class ItooBackgroundProcedureReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    Log.d(MelonNotification.TAG, "onReceive() called")
    if (context == null || intent == null) {
      Log.d(MelonNotification.TAG, "onReceive() aborting due to nulls [context=$context, intent=$intent]")
      return
    }

    val procedure = intent.getStringExtra("procedure")
    if (procedure == null) {
      Log.d(MelonNotification.TAG, "onReceive() missing procedure name, aborting")
      return
    }

    val screen = intent.getStringExtra("screen")
    if (screen == null) {
      Log.d(MelonNotification.TAG, "onReceive() missing screen name, aborting")
      return
    }

    val yailArgs = intent.getStringExtra("arguments")
    if (yailArgs == null) {
      Log.d(MelonNotification.TAG, "onReceive() missing procedure arguments, aborting")
      return
    }

    val args = JsonUtil.getObjectFromJson(yailArgs, true) as ArrayList<*>
    FrameworkWrapper(context, screen).call(procedure, *args.toArray())

  }
}