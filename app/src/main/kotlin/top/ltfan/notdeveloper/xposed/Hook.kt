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
//import de.robv.android.xposed.IXposedHookLoadPackage
//import de.robv.android.xposed.XC_MethodHook
//import de.robv.android.xposed.XposedBridge
//import de.robv.android.xposed.XposedHelpers
//import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam // 確保 LoadPackageParam 被正確導入
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import android.os.Build
object SdkState {
    // 用於儲存全域 SDK_INT 狀態（初始化為原始值）
    var currentSdkInt: Int = Build.VERSION.SDK_INT
}

@Keep
class Hook : IXposedHookLoadPackage {
    // 將這些常量定義在類級別或者 companion object 中
    companion object {
        private const val TARGET_PACKAGE_NAME = "com.android.vending" // Google Play 商店包名
        private const val TARGET_CLASS_NAME = "com.google.android.finsky.integrityservice.BackgroundIntegrityService"
    }
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        Log.i("開啟: ${lpparam.packageName}")
        if (lpparam.packageName.startsWith("com.android.vending")) {
            Log.i("Attempting to dynamically hook method in " + TARGET_CLASS_NAME);
            try {
                val targetClass = XposedHelpers.findClass(TARGET_CLASS_NAME, lpparam.classLoader)

                // 定義目標方法的簽名特徵
                val expectedReturnType = IBinder::class.java
                val expectedParamTypes = arrayOf(Intent::class.java) // Kotlin 陣列
                val candidateMethods = mutableListOf<Method>() // Kotlin 可變列表

                // 遍歷類的所有聲明方法
                for (method in targetClass.declaredMethods) {
                    // 1. 檢查返回類型
                    if (method.returnType != expectedReturnType) {
                        continue
                    }

                    // 2. 檢查參數類型和數量
                    // 在 Kotlin 中比較陣列內容使用 contentEquals
                    if (!method.parameterTypes.contentEquals(expectedParamTypes)) {
                        continue
                    }

                    // 3. 檢查修飾符 (可選，但更精確)
                    val modifiers = method.modifiers
                    if (!Modifier.isPublic(modifiers)) { // 必須是 public
                        continue
                    }
                    if (!Modifier.isFinal(modifiers)) { // 必須是 final
                        // 如果 final 不是絕對必要條件，可以註解掉這行檢查
                        continue
                    }

                    // 如果所有條件都符合，則將此方法視為候選方法
                    candidateMethods.add(method)
                    Log.i("IAmNotADeveloper: Found potential method: ${method.name} with signature ${method.toGenericString()}")
                }

                when {
                    candidateMethods.size == 1 -> {
                        val methodToHook = candidateMethods[0]
                        val foundMethodName = methodToHook.name // 動態獲取到的方法名
                        Log.i("IAmNotADeveloper: Dynamically found method to hook: $foundMethodName")

                        // 使用獲取到的 Method 物件進行 Hook
                        XposedBridge.hookMethod(methodToHook, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val intent = param.args[0] as Intent // 類型轉換
                                Log.i("IAmNotADeveloper: [$foundMethodName] Hooked before! Intent: $intent")
                                // 在這裡可以添加你的邏輯
                                // 例如：intent.putExtra("hooked_by_dynamic_finder", true)
                            }

                            override fun afterHookedMethod(param: MethodHookParam) {
                                val binder = param.result as IBinder? // 結果可能為 null，使用安全轉換
                                Log.i("IAmNotADeveloper: [$foundMethodName] Hooked after! IBinder: $binder")
                                // 在這裡可以添加你的邏輯
                            }
                        })
                        Log.i("IAmNotADeveloper: Successfully hooked $TARGET_CLASS_NAME.$foundMethodName")

                    }
                    candidateMethods.isEmpty() -> {
                        Log.i("IAmNotADeveloper: Error: No method found matching the signature in $TARGET_CLASS_NAME")
                    }
                    else -> {
                        Log.i("IAmNotADeveloper: Error: Multiple methods found matching the signature in $TARGET_CLASS_NAME. Needs further refinement.")
                        for (m in candidateMethods) {
                            Log.i("  - Candidate: ${m.name} (${m.toGenericString()})")
                        }
                    }
                }

            } catch (e: XposedHelpers.ClassNotFoundError) {
                Log.i("IAmNotADeveloper: Error: Target class $TARGET_CLASS_NAME not found.")
                Log.i(e) // XposedBridge 可以直接記錄 Throwable
            } catch (t: Throwable) { // 捕獲所有其他異常
                Log.i("IAmNotADeveloper: An unexpected error occurred during dynamic hook setup.")
                Log.i(t)
            }
            XposedHelpers.findAndHookMethod(
                "com.google.android.finsky.integrityservice.IntegrityService",
                lpparam.classLoader,
                "ml",
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
                "ml",
                android.content.Intent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                        XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", 32) // 偽裝為 Android 12
                        SdkState.currentSdkInt = 32
                        Log.i("Background 修改 SDK_INT 為 32")
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
            XposedHelpers.findAndHookMethod(
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
            )
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SDK_INT", SdkState.currentSdkInt) // 重新設定SDK_INT
            Log.i("重新設定SDK_INT 為: ${SdkState.currentSdkInt}")
        }
        if (lpparam.packageName.startsWith("com.google.android.gms")) {
            XposedHelpers.findAndHookMethod(
                "com.google.android.play.core.integrity.IntegrityManagerImpl",
                lpparam.classLoader,
                "requestIntegrityToken",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val sdkInt = Build.VERSION.SDK_INT
                        val requestParam = param.args[0] as String
                        Log.i("SDK_INT: $sdkInt, 請求 Param: $requestParam")
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result
                        Log.i("請求: $result")
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
