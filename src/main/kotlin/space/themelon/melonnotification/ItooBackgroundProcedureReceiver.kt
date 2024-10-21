package space.themelon.melonnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.appinventor.components.runtime.util.YailList

class ItooBackgroundProcedureReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent == null) {
      Log.d(MelonNotification.TAG, "onReceive() aborting due to nulls [context=$context, intent=$intent]")
      return
    }

    var procedure = intent.getStringExtra("procedure")
    if (procedure == null) {
      Log.d(MelonNotification.TAG, "onReceive() missing procedure name, aborting")
      return
    }

    val screen = intent.getStringExtra("screen")
    if (screen == null) {
      Log.d(MelonNotification.TAG, "onReceive() missing screen name, aborting")
      return
    }

    procedure = procedure.substring(1) // trim off '@'
    val yailArgs: YailList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getSerializableExtra("arguments", YailList::class.java)!!
    } else intent.getSerializableExtra("arguments") as YailList

    val args = yailArgs.toArray()!!

    // TODO:
    //  after testing is done we have to stop printing the args to logs
    Log.d(MelonNotification.TAG, "Calling procedure '$procedure' with args ${args.contentToString()}")

    val result = FrameworkWrapper(context, screen).call(procedure, args)
    Log.d(
      MelonNotification.TAG, if (result == FrameworkWrapper.Result.BAD) {
        "Failed to call Itoo procedure in background"
      } else {
        "Successfully dispatched Itoo Background Procedure event"
      }
    )
  }
}