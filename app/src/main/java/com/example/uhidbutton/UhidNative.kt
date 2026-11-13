package com.example.uhidbutton

object UhidNative {
    init { System.loadLibrary("uhidbutton") }

    external fun nativeCreateUHid(): Boolean
    external fun nativeCloseUHid(): Boolean
    external fun nativeTapKey(hidCode: Int)
    external fun nativeKeyDown(hidCode: Int)
    external fun nativeKeyUp(hidCode: Int)

    // HID usage page 0x07
    const val HID_SPACE = 0x2C
    const val HID_ENTER = 0x28
    const val HID_ESC   = 0x29
    const val HID_W     = 0x1A
    const val HID_A     = 0x04
    const val HID_S     = 0x16
    const val HID_D     = 0x07
    const val HID_E     = 0x08
    const val HID_SHIFT_L = 0xE1.toInt()  // модификатор
}
