package top.ltfan.notdeveloper.xposed

import android.content.ContentResolver
import android.provider.Settings
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import top.ltfan.notdeveloper.BuildConfig
import top.ltfan.notdeveloper.Item

import java.lang.Runtime
import android.app.Activity

// 必要的導入語句
import android.content.Intent
import android.os.IBinder
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import dalvik.system.DexFile

import android.os.Build
object SdkState {
    // 用於儲存全域 SDK_INT 狀態（初始化為原始值）
    //var currentSdkInt: Int = Build.VERSION.SDK_INT
    var currentSdkInt: Int = 32
}

@Keep
class Hook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        Log.i("開啟: ${lpparam.packageName}")
        if (lpparam.packageName.startsWith("com.android.vending")) {
            val targetClass = XposedHelpers.findClass("com.google.android.finsky.integrityservice.IntegrityService", lpparam.classLoader)
            val expectedReturnType = IBinder::class.java
            val expectedParamTypes = arrayOf(Intent::class.java) // Kotlin 陣列
            val candidateMethods = mutableListOf<Method>() // Kotlin 可變列表
            for (method in targetClass.declaredMethods) {
                if (method.returnType != expectedReturnType) {
                    continue
                }
                if (!method.parameterTypes.contentEquals(expectedParamTypes)) {
                    continue
                }
                val modifiers = method.modifiers
                if (!Modifier.isPublic(modifiers)) {
                    continue
                }
                if (!Modifier.isFinal(modifiers)) {
                    continue
                }
                //Log.i("動態讀取到了 IntegrityService");
                candidateMethods.add(method)
            }
            val targetClass1 =XposedHelpers.findClass("com.google.android.finsky.integrityservice.BackgroundIntegrityService",lpparam.classLoader)
            for (method in targetClass1.declaredMethods) {
                if (method.returnType != expectedReturnType) {
                    continue
                }
                if (!method.parameterTypes.contentEquals(expectedParamTypes)) {
                    continue
                }
                val modifiers = method.modifiers
                if (!Modifier.isPublic(modifiers)) {
                    continue
                }
                if (!Modifier.isFinal(modifiers)) {
                    continue
                }
                //Log.i("動態讀取到了 BackgroundIntegrityService")
                candidateMethods.add(method)
            }
            XposedHelpers.findAndHookMethod(
                "com.google.android.finsky.integrityservice.IntegrityService",
                lpparam.classLoader,
                candidateMethods[0].name,
                android.content.Intent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                        XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 32) // 偽裝為 Android 12
                        SdkState.currentSdkInt = 32
                        Log.i("暫時修改 SDK_INT 為 32")
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Thread {
                            Thread.sleep(3000) // 確保 caller thread 已經繼續
                            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 35)
                            SdkState.currentSdkInt = 35
                            Log.i("還原 SDK_INT 為 35")
                            Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop com.android.vending"))
                        }.start()
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                "com.google.android.finsky.integrityservice.BackgroundIntegrityService",
                lpparam.classLoader,
                candidateMethods[1].name,
                android.content.Intent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                        XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 32) // 偽裝為 Android 12
                        SdkState.currentSdkInt = 32
                        Log.i("暫時修改 SDK_INT 為 32")
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Thread {
                            Thread.sleep(3000) // 確保 caller thread 已經繼續
                            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 35)
                            Log.i("Background 還原 SDK_INT 為 35")
                            SdkState.currentSdkInt = 35
                            Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop com.android.vending"))
                        }.start()
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "onCreate",
                "android.os.Bundle",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as Activity
                        if (activity.packageName == "com.android.vending") {
                            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 35)
                            SdkState.currentSdkInt = 35
                            Log.i("使用者進入 Google Play SDK_INT 設為 35")
                        }
                    }
                }
            )
            /* XposedHelpers.findAndHookMethod(
                android.app.Activity::class.java,
                "onStop",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as Activity
                        if (activity.packageName == "com.android.vending") {
                            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 32) // 偽裝為 Android 12
                            Log.i("使用者離開 Google Play SDK_INT 改回 32")
                            SdkState.currentSdkInt = 32
                            Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop com.android.vending"))
                        }
                    }
                }
            ) */
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", SdkState.currentSdkInt) // 重新設定SDK_INT
            Log.i("重新設定SDK_INT 為: ${SdkState.currentSdkInt}")
        }
        if (!lpparam.packageName.startsWith("com.android.vending")) {
            XposedHelpers.findAndHookMethod(
                "com.google.android.play.core.integrity.IntegrityManagerImpl",
                lpparam.classLoader,
                "requestIntegrityToken",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop com.android.vending"))
                        val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                        XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 32) // 偽裝為 Android 12
                        SdkState.currentSdkInt = 32
                        Log.i("暫時修改 SDK_INT 為 32")
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Thread {
                            Thread.sleep(3000) // 確保 caller thread 已經繼續
                            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 35)
                            Log.i("Background 還原 SDK_INT 為 35")
                            SdkState.currentSdkInt = 35
                            Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop com.android.vending"))
                        }.start()
                    }
                }
            )
        }
        if (lpparam.packageName.startsWith("android") || lpparam.packageName.startsWith("com.android")) {
            return
        }
        //Log.i("processing package ${lpparam.packageName}")

        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            XposedHelpers.findAndHookMethod(
                "${BuildConfig.APPLICATION_ID}.xposed.ModuleStatusKt",
                lpparam.classLoader,
                "getStatusIsModuleActivated",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                },
            )
        }

        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID)

        val newApiCallback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                prefs.reload()
                changeResultToZero(
                    lpparam,
                    prefs,
                    param,
                    *(Item.oldApiItems.toTypedArray() + Item.newApiItems.toTypedArray())
                )
            }
        }

        val oldApiCallback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                prefs.reload()
                changeResultToZero(lpparam, prefs, param, *Item.oldApiItems.toTypedArray())
            }
        }

        XposedHelpers.findAndHookMethod(
            Settings.Global::class.java,
            "getInt",
            ContentResolver::class.java,
            String::class.java,
            Int::class.java,
            newApiCallback,
        )

        XposedHelpers.findAndHookMethod(
            Settings.Global::class.java,
            "getInt",
            ContentResolver::class.java,
            String::class.java,
            newApiCallback,
        )

        XposedHelpers.findAndHookMethod(
            Settings.Secure::class.java,
            "getInt",
            ContentResolver::class.java,
            String::class.java,
            Int::class.java,
            oldApiCallback,
        )

        XposedHelpers.findAndHookMethod(
            Settings.Secure::class.java,
            "getInt",
            ContentResolver::class.java,
            String::class.java,
            oldApiCallback,
        )
        processSystemProps(prefs, lpparam)
    }

    private fun processSystemProps(prefs: XSharedPreferences, lpparam: LoadPackageParam) {
        val clazz = XposedHelpers.findClassIfExists(
            "android.os.SystemProperties", lpparam.classLoader
        )

        if (clazz == null) {
            Log.w("cannot find SystemProperties class")
            return
        }

        val ffsReady = "sys.usb.ffs.ready"
        val usbState = "sys.usb.state"
        val usbConfig = "sys.usb.config"
        val rebootFunc = "persist.sys.usb.reboot.func"
        val svcAdbd = "init.svc.adbd"

        val methodGet = "get"
        val methodGetprop = "getprop"
        val methodGetBoolean = "getBoolean"
        val methodGetInt = "getInt"
        val methodGetLong = "getLong"

        val overrideAdb = "mtp"
        val overrideSvcAdbd = "stopped"

        listOf(methodGet, methodGetprop, methodGetBoolean, methodGetInt, methodGetLong).forEach {
            XposedBridge.hookAllMethods(
                clazz, it,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        prefs.reload()
                        if (!prefs.getBoolean(Item.AdbEnabled.key, true)) return

                        val arg = param.args[0] as String
                        //Log.i("processing ${param.method.name} from ${lpparam.packageName} with arg $arg")

                        if (param.method.name != methodGet && arg != ffsReady) {
                            Log.i("props processed ${param.method.name} from ${lpparam.packageName} receiving invalid arg $arg")
                            return
                        }

                        when (arg) {
                            ffsReady -> {
                                when (param.method.name) {
                                    methodGet -> param.result = "0"
                                    methodGetprop -> param.result = "0"
                                    methodGetBoolean -> param.result = false
                                    methodGetInt -> param.result = 0
                                    methodGetLong -> param.result = 0L
                                }
                            }

                            usbState -> param.result = overrideAdb
                            usbConfig -> param.result = overrideAdb
                            rebootFunc -> param.result = overrideAdb
                            svcAdbd -> param.result = overrideSvcAdbd
                        }

                        //Log.i("processed ${param.method.name}($arg): ${param.result}")
                    }
                }
            )
        }
    }

    private fun changeResultToZero(
        lpparam: LoadPackageParam,
        prefs: XSharedPreferences,
        param: MethodHookParam,
        vararg items: Item
    ) {
        val arg = param.args[1] as String
        //Log.i("processing ${param.method.name} from ${lpparam.packageName} with arg $arg")

        items.forEach { item ->
            val key = item.key
            if (prefs.getBoolean(key, true) && arg == key) {
                param.result = 0
                //Log.i("processed ${param.method.name}($arg): ${param.result}")
                return
            }
        }

        //Log.i("processed ${param.method.name}($arg) without changing result")
    }
}
