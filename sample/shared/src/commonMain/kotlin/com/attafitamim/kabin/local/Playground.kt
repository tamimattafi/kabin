package com.attafitamim.kabin.local

import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.local.dao.UserCompoundsDao
import com.attafitamim.kabin.local.dao.UserDao
import com.attafitamim.kabin.local.database.SampleDatabase
import com.attafitamim.kabin.local.database.newInstance
import com.attafitamim.kabin.local.entities.BankInfo
import com.attafitamim.kabin.local.entities.CarPurchase
import com.attafitamim.kabin.local.entities.UserEntity
import com.attafitamim.kabin.local.entities.UserWithSpouseCompound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object Playground {

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    suspend fun useSampleDatabase(driver: SqlDriver) {
        val database = SampleDatabase::class.newInstance(driver)

        var user = UserEntity(
            id = 123,
            phoneNumber = "+71234567890",
            gender = UserEntity.Gender.MALE,
            age = 18,
            name = "Jake",
            salary = 100.0f,
            isMarried = true,
            bankInfo = BankInfo(
                bankNumber = 123,
                cardNumber = "123",
                money = 123f,
                cardToken = "123",
                carPurchase = CarPurchase(
                    car = "Kia",
                    price = "213$",
                    tires = 4
                )
            ),
            spouseId = 124,
            secret = "Ignored Secret"
        )

        val spouse = UserEntity(
            id = 124,
            phoneNumber = "+71234567891",
            gender = UserEntity.Gender.FEMALE,
            age = 19,
            name = "Jaka",
            salary = 100.1f,
            isMarried = true,
            bankInfo = BankInfo(
                bankNumber = 124,
                cardNumber = "124",
                cardToken = "124",
                money = 124f,
                carPurchase = CarPurchase(
                    car = "Kia",
                    price = "214$",
                    tires = 4
                )
            ),
            spouseId = 123,
            secret = "Ignored Secret"
        )

        with(database) {
            // Start listening
            userDao.listenToEntitiesReactive()

            // Insert data
            userDao.insertEntity(user)
            userDao.insertEntity(spouse)

            // Read and update data
            user = userDao.readEntity(user)
            user = userDao.updateEntity(user.copy(salary = 300.0f))
            user = userDao.readEntity(user)

            // Read compound
            val compound = userCompoundsDao.readCompound(user)

            // Delete data
            userDao.deleteEntity(compound.mainEntity)
        }
    }

    private suspend fun UserCompoundsDao.readCompound(entity: UserEntity): UserWithSpouseCompound {
        val compound = getCompound(entity.age, entity.name)
        println("read compound $compound")
        return compound
    }

    private suspend fun UserDao.insertEntity(entity: UserEntity) {
        insertOrReplace(entity)
        println("write entity $entity")
    }

    private suspend fun UserDao.updateEntity(entity: UserEntity): UserEntity {
        update(entity)
        println("write entity $entity")
        return entity
    }

    private suspend fun UserDao.readEntity(entity: UserEntity): UserEntity {
        val readEntity = getEntity(entity.age, entity.name)
        println("read entity $readEntity")
        return readEntity
    }

    private suspend fun UserDao.listenToEntitiesReactive() {
        val readEntityFlow = getEntitiesReactive()
        println("listening to reactive entity $readEntityFlow")

        scope.launch {
            readEntityFlow.collect { readEntity ->
                println("got new reactive entity $readEntity")
            }
        }
    }

    private suspend fun UserDao.readEntityOrNull(entity: UserEntity): UserEntity? {
        val readEntity = getEntityOrNull(entity.age, entity.name)
        println("read entity or null $readEntity")
        return readEntity
    }

    private suspend fun UserDao.deleteEntity(entity: UserEntity) {
        delete(entity)
        println("deleted entity $entity")
    }
}
