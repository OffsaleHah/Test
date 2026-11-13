package com.example.uhidbutton

import android.content.Intent
import android.os.IBinder
import android.util.Log

object UhidProcess {
    private const val TAG = "UhidProcess"
    private const val ACTION = "intent.uhidbutton.sendBinder"

    @JvmStatic
    fun main(args: Array<String>) {
        val uid = android.os.Process.myUid()
        if (uid != 2000 && uid != 0) {
            System.err.println("Need shell (2000) or root (0), got $uid")
            System.exit(1)
        }
        Log.i(TAG, "Starting UhidProcess, uid=$uid")

        if (!UhidNative.nativeCreateUHid()) {
            Log.e(TAG, "UHID create failed")
            System.exit(2)
        }
        Log.i(TAG, "UHID keyboard created")

        val binder: IBinder = object : IKeyInjector.Stub() {
            override fun tapSpace() = UhidNative.nativeTapKey(UhidNative.HID_SPACE)
            override fun keyDown(hid: Int) = UhidNative.nativeKeyDown(hid)
            override fun keyUp(hid: Int) = UhidNative.nativeKeyUp(hid)
            override fun close() { UhidNative.nativeCloseUHid() }
        }

        sendBinderByStickyBroadcast(binder)

        // Держим процесс живым, пока не убьют
        try {
            while (true) Thread.sleep(60_000)
        } catch (_: InterruptedException) {
        } finally {
            UhidNative.nativeCloseUHid()
        }
    }

    private fun sendBinderByStickyBroadcast(binder: IBinder) {
        try {
            val intent = Intent(ACTION).apply {
                putExtra("binder", BinderContainer(binder))
            }

            val activityBinder = Class.forName("android.os.ServiceManager")
                .getDeclaredMethod("getService", String::class.java)
                .invoke(null, "activity") as IBinder

            val iActivityManager = Class.forName("android.app.IActivityManager\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, activityBinder)
                ?: throw IllegalStateException("IActivityManager is null")

            // Ищем broadcastIntent по имени и числу параметров (13),
            // без ссылок на скрытые IApplicationThread / IIntentReceiver.
            val broadcastIntent = iActivityManager.javaClass.methods.firstOrNull {
                it.name == "broadcastIntent" && it.parameterCount == 13
            } ?: throw NoSuchMethodException("broadcastIntent/13 not found")

            broadcastIntent.isAccessible = true
            broadcastIntent.invoke(
                iActivityManager,
                null, intent, null, null, -1, null, null, null,
                0, null, false, /*sticky=*/ true, -1
            )
            Log.i(TAG, "Binder broadcast sent")
        } catch (e: Exception) {
            Log.e(TAG, "Broadcast failed", e)
            System.exit(-1)
        }
    }
}
