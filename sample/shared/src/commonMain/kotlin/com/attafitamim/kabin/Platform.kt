package com.attafitamim.kabin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform