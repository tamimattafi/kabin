[![Kabin Release](https://img.shields.io/github/release/tamimattafi/kabin.svg?style=flat)]()
[![Kotlin](https://img.shields.io/github/languages/top/tamimattafi/kabin.svg?style=for-the-badge&color=blueviolet)](https://kotlinlang.org/)
[![License Apache 2.0](https://img.shields.io/github/license/tamimattafi/kabin.svg?style=for-the-badge&color=purple)](https://github.com/tamimattafi/kabin/blob/main/LICENSE)

<h1 align="center">
    <img height="150" src="./art/kabin.png"/>
    <br>
    <a>Kabin</a>: Multiplatform Database Library
</h1>

A Kotlin Multiplatform library for database storage, which aims to support all functionality offered by **Room**.
Kabin uses drivers from **SQLDelight**, offering a stable interaction with `SQL` on all targets supported by the latter.

> [!CAUTION]
> This library is still under development. Avoid using it in production

## Showcase
Using Kabin is straight forward. Annotations are identical to those in Room, which means usage is identical too.
Here's how you declare a simple database:

1. Create an `Entity`:
```kotlin
@Entity
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val name: String
)
```

2. Create a `Dao`:
```kotlin
@Dao
interface UserDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertOrReplace(entity: UserEntity)
}
```

3. Create a `Database`:
```kotlin
@Database(
    entities = [
        UserEntity::class
    ],
    version = 1
)
interface SampleDatabase : KabinDatabase {
    val userDao: UserDao

    companion object {
        const val NAME = "sample-database"
    }
}
```

Kabin will generate code for you and glue everything together.

Finally, create an **SQLDelight** `driver` using the generated `schema`, then pass it to the `newInstance` method, to initialize `SampleDatabase`
```kotlin
// Implement for every platform according to SQLDelight documentation
expect fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>)

val driver = createPlatformDriver(schema = SampleDatabase::class.schema)
val sampleDatabase = SampleDatabase::class.newInstance(driver = driver)
```

## Installation
Latest Kabin version: [![Kabin Release](https://img.shields.io/github/release/tamimattafi/kabin.svg?style=flat)]()
Latest SQLDelight version: [![SQLDelight Release](https://img.shields.io/github/release/cashapp/sqldelight.svg?style=flat)]()

Add `common` modules to your `sourceSet`:
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies { 
            // Kabin
            implementation("com.attafitamim.kabin:annotations:$kabin_version")
            implementation("com.attafitamim.kabin:core:$kabin_version")

            // SQLDelight Runtime
            implementation("app.cash.sqldelight:runtime:$sqldelight_version")
        }
        
        // Make generated code visible for commonMain
        commonMain.kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin/")
    }
}
```

Add `ksp` compiler:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
}

dependencies {
    add("kspCommonMainMetadata", "com.attafitamim.kabin:compiler:$kabin_version")
}

// Workaround for using KSP in common
tasks.withType<KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

afterEvaluate {
    tasks.filter { task ->
        task.name.contains("SourcesJar", true)
    }.forEach { task ->
        task.dependsOn("kspCommonMainKotlinMetadata")
    }
}
```

## Supported Features
This list shows Room features, which are supported by Kabin, or under development

### @Entity
- [x] Feature 1
- [ ] Feature 2
