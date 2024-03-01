package com.attafitamim.kabin.local

import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.local.dao.SampleDao
import com.attafitamim.kabin.local.database.SampleDatabase
import com.attafitamim.kabin.local.database.newInstance
import com.attafitamim.kabin.local.entities.SampleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job

object Playground {

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    suspend fun useSampleDatabase(driver: SqlDriver) {
        val database = SampleDatabase::class.newInstance(driver)

        var currentEntity = SampleEntity(
            id = 123,
            phoneNumber = "+71234567890",
            age = 18,
            name = "Jake",
            salary = 100.0f,
            isMarried = true,
            secret = "Ignored Secret"
        )

        with(database.sampleDao) {
            insertEntity(currentEntity)
            currentEntity = readEntity(currentEntity)
            currentEntity = updateEntity(currentEntity.copy(salary = 300.0f))
            currentEntity = readEntity(currentEntity)
            deleteEntity(currentEntity)
            currentEntity = readEntity(currentEntity)
        }
    }

    private suspend fun SampleDao.insertEntity(entity: SampleEntity) {
        insertOrReplace(entity)
        println("write entity $entity")
    }

    private suspend fun SampleDao.updateEntity(entity: SampleEntity): SampleEntity {
        update(entity)
        println("write entity $entity")
        return entity
    }

    private suspend fun SampleDao.readEntity(entity: SampleEntity): SampleEntity {
        val readEntity = getEntity(entity.age, entity.name)
        println("read entity $readEntity")
        return entity
    }

    private suspend fun SampleDao.readEntityOrNull(entity: SampleEntity): SampleEntity {
        val readEntity = getEntityOrNull(entity.age, entity.name)
        println("read entity $readEntity")
        return entity
    }

    private suspend fun SampleDao.deleteEntity(entity: SampleEntity) {
        delete(entity)
        println("deleted entity $entity")
    }
}
