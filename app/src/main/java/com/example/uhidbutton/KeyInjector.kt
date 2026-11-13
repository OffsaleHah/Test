package com.example.uhidbutton

object KeyInjector {
    @Volatile var binder: IKeyInjector? = null
    @Volatile var ready = false
}
