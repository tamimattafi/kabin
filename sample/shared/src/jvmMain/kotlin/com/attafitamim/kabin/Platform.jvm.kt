package com.attafitamim.kabin

class JVMPlatform: Platform {
    override val name: String = "JVM"
}

actual fun getPlatform(): Platform = JVMPlatform()
