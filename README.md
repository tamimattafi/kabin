[![Kabin Release](https://img.shields.io/github/release/tamimattafi/kabin.svg?style=for-the-badge&color=green)]()
[![Kotlin](https://img.shields.io/github/languages/top/tamimattafi/kabin.svg?style=for-the-badge&color=blueviolet)](https://kotlinlang.org/)
[![License Apache 2.0](https://img.shields.io/github/license/tamimattafi/kabin.svg?style=for-the-badge&color=purple)](https://github.com/tamimattafi/kabin/blob/main/LICENSE)

<h1 align="center">
    <img height="150" src="./art/kabin.png"/>
    <br>
    <a>Kabin</a>: Multiplatform Database Library
</h1>

A Kotlin Multiplatform library for database storage, which aims to support all functionality offered by [Room](https://developer.android.com/training/data-storage/room).

Kabin uses drivers from **SQLDelight**, offering a stable interaction with `SQL` on all targets supported by the latter.

> [!CAUTION]
> This library is still under development. Avoid using it in production.

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

Finally, create an **SQLDelight** `driver` using the generated `schema`, then pass it to the `newInstance` method, to initialize `SampleDatabase`:
```kotlin
// Implement for every platform according to SQLDelight documentation
expect fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>)

val driver = createPlatformDriver(schema = SampleDatabase::class.schema)
val sampleDatabase = SampleDatabase::class.newInstance(driver = driver)
```

For more advanced topics, read [Room](https://developer.android.com/training/data-storage/room) documentation and tutorials, and apply the same logic using Kabin.

## Installation
Latest Kabin version: [![Kabin Release](https://img.shields.io/github/release/tamimattafi/kabin.svg?style=for-the-badge&color=green)]()

Latest SQLDelight version: [![SQLDelight Release](https://img.shields.io/github/release/cashapp/sqldelight.svg?style=for-the-badge&color=blue)]()

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

## Supported Room Features
This list shows Room features, which are already supported by Kabin, or under development

### @Entity
- [x] `tableName`
- [x] `indices`
- [ ] `inheritSuperIndices`
- [x] `primaryKeys`
- [x] `foreignKeys`
- [x] `ignoredColumns`

### @PrimaryKey
- [x] `autoGenerate`
- [x] Use `@PrimaryKey` on multiple columns
- [x] Use `@PrimaryKey` on `@Embedded` columns

### @Embedded
- [x] `prefix`
- [x] Nested `@Embedded` (`@Embedded` inside an `@Embedded`)
- [x] Compound (`@Embedded` entity inside a class for working with `@Relations`)
- [x] `@Embedded` columns as primary keys using `@PrimaryKey`

### @ColumnInfo
- [x] `name`
- [x] `typeAffinity`
- [x] `index`
- [ ] `collate`
- [x] `defaultValue`

### @Ignore
- [x] Skip columns annotated with `@Ignore`

### @ForeignKey
- [x] `entity`
- [x] `parentColumns`
- [x] `childColumns`
- [x] `onDelete`
- [x] `onUpdate`
- [x] `deferred`

### @Index
- [x] `columns`
- [x] `orders`
- [x] `name`
- [x] `unique`

### @Relation
- [x] Detect entity from property type
- [x] Detect entity from list property type
- [x] Insert entities automatically when inserting Compound classes with `@Embedded` entities
- [x] `entity`
- [x] `parentColumn`
- [x] `entityColumn`
- [x] `associateBy`
- [ ] `projection`

### @Junction
- [x] Retrieve data using `@Junction` table
- [x] Create and insert `@Junction` entities automatically when inserting classes with `@Relation`
- [x] `value`
- [x] `parentColumn`
- [x] `entityColumn`

### @Fts4
- [x] Create virtual table with triggers
- [x] `contentEntity`
- [ ] `tokenizerArgs`
- [ ] `languageId`
- [ ] `notIndexed`
- [ ] `prefix`
- [ ] `order`

### @Dao
- [x] Use coroutines and `suspend` functions
- [x] Support `Collection` and `Flow` return types
- [x] Execute operations on `Dispatcher.IO`
- [x] Interfaces annotated with `@Dao`
- [ ] Abstract classes annotated with `@Dao`

### @Insert
- [x] Insert single entity, multiple entities as distinct parameters or lists of entities
- [x] Insert Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`
- [x] `entity`
- [x] `onConflict`

### @Delete
- [x] Delete single entity, multiple entities as distinct parameters or lists of entities
- [x] Delete Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`
- [x] `entity`

### @Update
- [x] Update single entity, multiple entities as distinct parameters or lists of entities
- [x] Update Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`
- [x] `entity`
- [x] `onConflict`

### @Upsert
> [!CAUTION]
> This annotation is currently treated as @Insert with REPLACE strategy
- [ ] Use Upsert logic instead of simple insert with REPLACE strategy
- [x] Upsert single entity, multiple entities as distinct parameters or lists of entities
- [x] Upsert Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`
- [x] `entity`

### @RawQuery
- [x] Detect observed entities by return type
- [x] `observedEntities`

### @Query
- [x] Detect observed entities by return type
- [x] Detect observed entities by queried tables
- [x] Named parameters declared as `:parameter`
- [x] Nullable parameters
- [x] List and nullable list parameters
- [ ] Parameters declared as `?`
- [ ] Highlight SQL Syntax
- [ ] Validate SQL Syntax
- [ ] Auto complete SQL Syntax and named parameters
- [x] `value`

### @Transaction
> [!CAUTION]
> This annotation is temporarily disabled due blocking issues
- [ ] Support operations with dao annotations
- [ ] Support functions without dao annotations

### @Database
- [x] Interfaces annotated with `@Database`
- [ ] Abstract classes annotated with `@Database`
- [x] Generate adapters for primitive and enum classes
- [x] `entities`
- [ ] `views`
- [x] `version`
- [ ] `exportSchema`
- [ ] `autoMigrations`

### @TypeConverters
> [!CAUTION]
> This annotation can only accept converter `object` that implement `app.cash.sqldelight.ColumnAdapter`
- [x] `value`
- [ ] `builtInTypeConverters`

### @BuiltInTypeConverters
- [ ] `enums`
- [ ] `uuid`

### @AutoMigration
- [ ] Support auto migration functionality
- [ ] `from`
- [ ] `to`
- [ ] `spec`

## New Features
### @Mappers
- Used to map results returned by a dao to data classes that are not entities
- This annotation is meant to be used with `Database` class
- `value` accepts `object` that implement `KabinMapper<T>`

### Compound
- Classes that use `@Embedded` and `@Relation` annotations can be inserted by dao just like any entity
- `@Junction` inside a compound is automatically created and inserted as well
