package com.attafitamim.kabin

class WasmJSPlatform: Platform {
    override val name: String = "WasmJS"
}

actual fun getPlatform(): Platform =
    WasmJSPlatform()
