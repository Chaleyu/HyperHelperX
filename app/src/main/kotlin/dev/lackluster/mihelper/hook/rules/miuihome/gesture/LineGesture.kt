package dev.lackluster.mihelper.hook.rules.miuihome.gesture

import android.app.Application
import android.content.Intent
import android.widget.Toast
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import dev.lackluster.mihelper.data.Constants.ACTION_HOME
import dev.lackluster.mihelper.data.Constants.ACTION_NOTIFICATIONS
import dev.lackluster.mihelper.data.Constants.ACTION_QUICK_SETTINGS
import dev.lackluster.mihelper.data.Constants.ACTION_RECENTS
import dev.lackluster.mihelper.data.Constants.ACTION_SCREENSHOT
import dev.lackluster.mihelper.data.Constants.PER_MIUI_INTERNAL_API
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs

object LineGesture : YukiBaseHooker() {
    /*
    0 -> DISABLED;
    1 -> SYSTEM_ACTION_NOTIFICATIONS;
    2 -> SYSTEM_ACTION_QUICK_SETTINGS;
    3 -> SYSTEM_ACTION_LOCK_SCREEN;
    4 -> CAPTURE_SCREENSHOT;
    5 -> SYSTEM_ACTION_HOME;
    6 -> SYSTEM_ACTION_RECENTS;
    7 -> MI_AI_SCREEN;
    8 -> MI_AI_WAKE_UP;
    9 -> FLOATING_WINDOW;
     */
    private val actionLongPress = Prefs.getInt(Pref.Key.MiuiHome.LINE_GESTURE_LONG_PRESS, 0)
    private val actionDoubleTap = Prefs.getInt(Pref.Key.MiuiHome.LINE_GESTURE_DOUBLE_TAP, 0)
    private val applicationGetInstanceMethod by lazy {
        "com.miui.home.launcher.Application".toClass().method {
            name = "getInstance"
            modifiers { isStatic }
        }.get()
    }
    private val startSmallFreeformMethod by lazy {
        "android.util.MiuiMultiWindowUtils".toClassOrNull()?.method {
            name = "startSmallFreeformForControlCenter"
            param(ContextClass)
        }?.get()
    }

    override fun onHook() {
        if (actionLongPress != 0 || actionDoubleTap != 0) {
            "com.miui.home.recents.gesture.NavStubGestureEventManager".toClassOrNull()?.apply {
                if (actionLongPress != 0) {
                    method {
                        name = "handleLongPressEvent"
                    }.hook {
                        before {
                            val application = applicationGetInstanceMethod.invoke<Application>() ?: return@before
                            doAction(application, actionLongPress)
                            this.result = null
                        }
                    }
                }
                if (actionDoubleTap != 0) {
                    method {
                        name = "handleDoubleClickEvent"
                    }.hook {
                        before {
                            val application = applicationGetInstanceMethod.invoke<Application>() ?: return@before
                            doAction(application, actionDoubleTap)
                            this.result = null
                        }
                    }
                    method {
                        name = "checkDoubleClickTriggerApp"
                    }.ignored().hook {
                        before {
                            this.instance.current(true).field {
                                name = "isCanDoubleClickTriggerApp"
                            }.set(true)
                            this.result = null
                        }
                    }
                    method {
                        name = "updateIsCanDoubleClickTriggerApp"
                    }.ignored().hook {
                        intercept()
                    }
                }
            }
        }
    }

    private fun doAction(application: Application, action: Int) {
        when (action) {
            3 -> {
                application.sendBroadcast(
                    Intent("com.miui.app.ExtraStatusBarManager.action_TRIGGER_TOGGLE")
                        .putExtra("com.miui.app.ExtraStatusBarManager.extra_TOGGLE_ID", 10)
                )
            }
            4 -> {
                application.sendBroadcast(Intent(ACTION_SCREENSHOT))
            }
            7,8 -> {
                application.startForegroundService(
                    Intent("android.intent.action.ASSIST").apply {
                        setClassName("com.miui.voiceassist", "com.xiaomi.voiceassistant.VoiceService")
                        putExtra("triggerFrom", "MiuiHome")
                        putExtra(
                            "voice_assist_function_key",
                            if (action == 7) "start_screen_recognition"
                            else "wake_up_voice_assist"
                        )
                        putExtra(
                            "triggerType",
                            if (action == 7) "NavLongPress"
                            else "NavDoubleClick"
                        )
                        putExtra(
                            "voice_assist_start_from_key",
                            if (action == 7) "long_press_fullscreen_gesture_line"
                            else "double_click_fullscreen_gesture_line"
                        )
                    }
                )
            }
            9 -> {
                val context = application.applicationContext
                startSmallFreeformMethod?.string(context)?.let {
                    if (it.isNotBlank()) {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else -> {
                val intent = when(action) {
                    1 -> Intent(ACTION_NOTIFICATIONS)
                    2 -> Intent(ACTION_QUICK_SETTINGS)
                    5 -> Intent(ACTION_HOME)
                    6 -> Intent(ACTION_RECENTS)
                    else -> return
                }
                application.sendBroadcast(intent, PER_MIUI_INTERNAL_API)
            }
        }
    }
}