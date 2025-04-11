package space.themelon.melonnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.appinventor.components.runtime.util.JsonUtil
import java.util.ArrayList

class ItooBackgroundProcedureReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    Log.d(MelonNotification.TAG, "onReceive() called")
    val callOnMain = intent.getBooleanExtra("callOnMain", false)

    if (callOnMain) {
      // send all the payload to be dispatched on UI (if it's active)
      context.sendBroadcast(
        Intent(MelonNotification.PROCEDURE_DISPATCHER_ACTION)
          .also { it.putExtras(intent.extras!!) }
      )
    } else {
      val procedure = intent.getStringExtra("procedure")!!
      val screen = intent.getStringExtra("screen")!!
      val yailArgs = intent.getStringExtra("arguments")!!

      val args = JsonUtil.getObjectFromJson(yailArgs, true) as ArrayList<*>
      FrameworkWrapper(context, screen).call(procedure, *args.toArray())
    }
  }
}