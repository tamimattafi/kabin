package com.attafitamim.kabin

class JSPlatform: Platform {
    override val name: String = "JS"
}

actual fun getPlatform(): Platform = JSPlatform()
