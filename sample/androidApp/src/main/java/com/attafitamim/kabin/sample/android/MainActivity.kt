package com.attafitamim.kabin.sample.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.attafitamim.kabin.local.Playground
import com.attafitamim.kabin.local.PlaygroundConfiguration

class MainActivity : AppCompatActivity() {

    private val playground: Playground by lazy {
        Playground(PlaygroundConfiguration(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playground.start()
    }
}