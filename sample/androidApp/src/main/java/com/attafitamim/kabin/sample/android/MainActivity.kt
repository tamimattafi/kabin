package com.attafitamim.kabin.sample.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.attafitamim.kabin.core.database.KabinDatabaseConfiguration
import com.attafitamim.kabin.local.Playground

class MainActivity : AppCompatActivity() {

    private val playground: Playground by lazy {
        val configuration = KabinDatabaseConfiguration(
            context = this,
            name = "sample-database"
        )

        Playground(configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playground.start()
    }
}