package space.themelon.melonnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.errors.YailRuntimeError
import com.google.appinventor.components.runtime.util.JsonUtil
import gnu.expr.ModuleMethod
import gnu.lists.LList
import gnu.mapping.Symbol

class ProcedureDispatchReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    // we are dispatching the procedure normally, not through Itoo!
    val form = Form.getActiveForm()

    val procedure = intent.getStringExtra("procedure")!!
    val screen = intent.getStringExtra("screen")!!

    // ensure we are on the same screen where the procedure lies
    if (form::class.java.simpleName != screen) {
      throw YailRuntimeError("Cannot dispatch procedure event because application is not on " +
          "screen ${screen} for procedure $procedure to be called", MelonNotification.TAG)
    }

    val argsString = intent.getStringExtra("arguments")!!
    val args = JsonUtil.getObjectFromJson(argsString, true) as java.util.ArrayList<*>
    callProcedure(procedure, args.toArray())
  }

  private fun callProcedure(procedureName: String, args: Array<Any>) {
    val form = Form.getActiveForm()
    val field = form.javaClass.getField("global\$Mnvars\$Mnto\$Mncreate")
    val variables = field[form] as LList

    var method: ModuleMethod? = null
    for (variable in variables) {
      if (LList.Empty == variable) {
        continue
      }
      val asPair = variable as LList
      val name = (asPair[0] as Symbol).name
      if (name == procedureName) {
        method = (asPair[1] as ModuleMethod).apply0() as ModuleMethod
        break
      }
    }
    method ?: throw YailRuntimeError("Could not find procedure '$procedureName'", MelonNotification.TAG)
    method.applyN(args)
  }
}