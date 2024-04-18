package com.attafitamim.kabin.local

import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration
import com.attafitamim.kabin.core.migration.KabinMigrationStrategy
import com.attafitamim.kabin.local.dao.UserCompoundsDao
import com.attafitamim.kabin.local.dao.UserDao
import com.attafitamim.kabin.local.database.SampleDatabase
import com.attafitamim.kabin.local.database.newInstance
import com.attafitamim.kabin.local.entities.bank.BankEntity
import com.attafitamim.kabin.local.entities.user.UserEntity
import com.attafitamim.kabin.local.entities.user.UserWithBankCompound
import com.attafitamim.kabin.local.entities.bank.BankInfo
import com.attafitamim.kabin.local.entities.bank.CarPurchase
import com.attafitamim.kabin.local.entities.user.Gender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Playground(
    private val configuration: KabinDatabaseConfiguration
) {

    val scope = CoroutineScope(Dispatchers.Default)
    private val database: SampleDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SampleDatabase::class.newInstance(
            configuration = configuration,
            migrations = emptyList(),
            migrationStrategy = KabinMigrationStrategy.DESTRUCTIVE
        )
    }

    fun start() = scope.launch {
        var user = UserEntity(
            id = 123,
            phoneNumber = "+71234567890",
            gender = Gender.MALE,
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
            data = "SLS",
            secret = "Ignored Secret"
        )

        val spouse = UserEntity(
            id = 124,
            phoneNumber = "+71234567891",
            gender = Gender.FEMALE,
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
            data = "LSL",
            spouseId = 123,
            secret = "Ignored Secret"
        )

        with(database) {
            // Start listening
            //userCompoundsDao.listenToEntitiesReactive()
            userDao.insertBankEntity(BankEntity(number = 123, country = "SA", region = "LS", supportedCards = emptyList()))

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
            userDao.deleteEntity(compound.mainCompound.mainEntity)
        }
    }

    private suspend fun UserCompoundsDao.readCompound(entity: UserEntity): UserWithBankCompound {
        val compound = getBankCompound(entity.age, entity.name)
        println("read compound $compound")
        return compound
    }

    private suspend fun UserDao.insertEntity(entity: UserEntity) {
        insertOrReplace(entity)
        println("write entity $entity")
    }

    private suspend fun UserDao.insertBankEntity(entity: BankEntity) {
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

    private suspend fun UserCompoundsDao.listenToEntitiesReactive() {
        val readEntityFlow = getBankCompoundsReactive()
        println("listening to reactive entity $readEntityFlow")

        scope.launch {
            readEntityFlow?.collect { readEntity ->
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
