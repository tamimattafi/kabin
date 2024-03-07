package com.attafitamim.kabin.sample.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.attafitamim.kabin.local.Playground
import com.attafitamim.kabin.local.database.SampleDatabase
import com.attafitamim.kabin.local.database.scheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Playground.scope.launch {
            val driver = AndroidSqliteDriver(
                SampleDatabase::class.scheme.synchronous(),
                this@MainActivity,
                SampleDatabase.NAME
            )

            Playground.useSampleDatabase(driver)
        }
    }
}