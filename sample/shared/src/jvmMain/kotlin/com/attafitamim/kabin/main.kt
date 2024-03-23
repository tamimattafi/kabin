package com.attafitamim.kabin

import com.attafitamim.kabin.local.Playground
import com.attafitamim.kabin.local.PlaygroundConfiguration
import kotlinx.coroutines.isActive

fun main() {
    val playground = Playground(PlaygroundConfiguration())

    playground.start()
    while (playground.scope.isActive) {
        Thread.sleep(10000)
    }
}