// Copyright (C) 2023 Kumaraswamy B G
// GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007
// See LICENSE for full details
// Taken from: https://github.com/theItoO/itoo-x/blob/main/itoox-wrapper/src/xyz/kumaraswamy/itoox/wrapper/FrameworkWrapper.java
package space.themelon.melonnotification

import android.content.Context
import com.google.appinventor.components.runtime.Form
import java.lang.reflect.Method

class FrameworkWrapper(context: Context, screen: String) {
  private var success = false

  private var framework: Any? = null
  private var callProcedureMethod: Method? = null

  init {
    if (isItooXPresent) {
      try {
        success = safeInit(context, screen)
      } catch (e: ReflectiveOperationException) {
        e.printStackTrace()
        success = false
      }
    } else {
      success = false
    }
  }

  @Throws(ReflectiveOperationException::class)
  private fun safeInit(context: Context, screen: String): Boolean {
    val success: Boolean
    val clazz = Class.forName(FRAMEWORK_CLASS)
    val result = clazz.getMethod("get", Context::class.java, String::class.java) // static method invocation
      // get(Context, String)
      .invoke(null, context, screen)
    val resultClazz: Class<*> = result.javaClass
    // success() method
    success = resultClazz.getMethod("success").invoke(result) as Boolean
    if (success) {
      framework = resultClazz.getMethod("getFramework").invoke(result)
      callProcedureMethod = clazz.getMethod("call", String::class.java, Array<Any>::class.java)
    }
    return success
  }

  fun call(procedure: String, vararg args: Any) {
    callProcedureMethod!!.invoke(framework, procedure, args)
  }

  fun close(): Boolean {
    try {
      return framework!!.javaClass.getMethod("close").invoke(framework) as Boolean
    } catch (e: ReflectiveOperationException) {
      e.printStackTrace()
      return false
    }
  }

  companion object {
    private const val FRAMEWORK_CLASS = "xyz.kumaraswamy.itoox.Framework"

    val isItooXPresent: Boolean
      get() {
        try {
          Class.forName(FRAMEWORK_CLASS)
          return true
        } catch (e: ClassNotFoundException) {
          return false
        }
      }

    @Throws(ReflectiveOperationException::class)
    fun activateItooX() {
      if (!isItooXPresent) {
        return
      }
      val form = Form.getActiveForm() ?: return
      val refScreen = form.javaClass.simpleName

      val itooInt = Class.forName("xyz.kumaraswamy.itoox.ItooInt")
      val method = itooInt.getMethod(
        "saveIntStuff",
        Form::class.java,
        String::class.java
      )

      // static invoke saveIntStuff(Form, String)
      method.invoke(null, form, refScreen)
    }
  }
}
