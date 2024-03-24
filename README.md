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
}
```

Kabin will generate code for you and glue everything together.

4. Finally, create a platform configuration, then pass it to the `newInstance` method, to initialize `SampleDatabase`:
```kotlin
// Create configuration for every platform, here's an example for android
val configuration = KabinDatabaseConfiguration(
    context = this,
    name = "sample-database"
)

val sampleDatabase = SampleDatabase::class.newInstance(
    configuration,
    migrations = emptyList(),
    migrationStrategy = KabinMigrationStrategy.DESTRUCTIVE
)
```

For more advanced topics, read [Room](https://developer.android.com/training/data-storage/room) documentation and tutorials, and apply the same logic using Kabin.

## Installation
Latest Kabin version

[![Kabin Release](https://img.shields.io/github/release/tamimattafi/kabin.svg?style=for-the-badge&color=green)]()

Add `common` modules to your `sourceSet`:
```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kabin
                implementation("com.attafitamim.kabin:core:$kabin_version")
            }

            // Make generated code visible for commonMain
            kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin/")
        }
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

## Optional
Configure `ksp` processor to generate more suitable code for you
```kotlin
ksp {
    // Use this prefix for fts tables to keep the old room scheme
    arg("FTS_TRIGGER_NAME_PREFIX", "room_fts_content_sync")
}
```

Available keys, with their default values
```kotlin
TABLE_SUFFIX("KabinTable")
ENTITY_MAPPER_SUFFIX("KabinMapper")
DATABASE_SUFFIX("KabinDatabase")
DAO_SUFFIX("KabinDao")
DAO_QUERIES_SUFFIX("KabinQueries")
INDEX_NAME_PREFIX("index")
FTS_TRIGGER_NAME_PREFIX("kabin_fts_content_sync")
BEFORE_UPDATE_TRIGGER_NAME_SUFFIX("BEFORE_UPDATE")
AFTER_UPDATE_TRIGGER_NAME_SUFFIX("AFTER_UPDATE")
BEFORE_DELETE_TRIGGER_NAME_SUFFIX("BEFORE_DELETE")
AFTER_INSERT_TRIGGER_NAME_SUFFIX("AFTER_INSERT")
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
- [x] `entity`
- [x] `parentColumn`
- [x] `entityColumn`
- [x] `associateBy`
- [ ] `projection`
- [x] Detect entity from property type
- [x] Detect entity from list property type
- [x] Insert entities automatically when inserting Compound classes with `@Embedded` entities

### @Junction
- [x] `value`
- [x] `parentColumn`
- [x] `entityColumn`
- [x] Retrieve data using `@Junction` table
- [x] Create and insert `@Junction` entities automatically when inserting classes with `@Relation`

### @Fts4
- [x] `contentEntity`
- [ ] `tokenizerArgs`
- [ ] `languageId`
- [ ] `notIndexed`
- [ ] `prefix`
- [ ] `order`
- [x] Create virtual table with triggers

### @Dao
- [x] Use coroutines and `suspend` functions
- [x] Support `Collection` and `Flow` return types
- [x] Execute operations on `Dispatcher.IO`
- [x] Interfaces annotated with `@Dao`
- [ ] Abstract classes annotated with `@Dao`

### @Insert
- [x] `entity`
- [x] `onConflict`
- [x] Insert single entity, multiple entities as distinct parameters or lists of entities
- [x] Insert Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`

### @Delete
- [x] `entity`
- [x] Delete single entity, multiple entities as distinct parameters or lists of entities
- [x] Delete Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`

### @Update
- [x] `entity`
- [x] `onConflict`
- [x] Update single entity, multiple entities as distinct parameters or lists of entities
- [x] Update Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`

### @Upsert
> [!CAUTION]
> This annotation is currently treated as @Insert with REPLACE strategy
- [x] `entity`
- [ ] Use Upsert logic instead of simple insert with REPLACE strategy
- [x] Upsert single entity, multiple entities as distinct parameters or lists of entities
- [x] Upsert Compound classes with `@Embedded` entities including their `@Relation` and `@Junction`

### @RawQuery
- [x] `observedEntities`
- [x] Detect observed entities by return type

### @Query
- [x] `value`
- [x] Detect observed entities by return type
- [x] Detect observed entities by queried tables
- [x] Named parameters declared as `:parameter`
- [x] Nullable parameters
- [x] List and nullable list parameters
- [ ] Parameters declared as `?`
- [ ] Highlight SQL Syntax
- [ ] Validate SQL Syntax
- [ ] Auto complete SQL Syntax and named parameters

### @Transaction
- [x] Functions with `@Transaction` annotation
- [x] Functions working with multiple entity parameters, collections and compounds

### @Database
- [x] `entities`
- [ ] `views`
- [x] `version`
- [ ] `exportSchema`
- [ ] `autoMigrations`
- [x] Interfaces annotated with `@Database`
- [ ] Abstract classes annotated with `@Database`
- [x] Generate adapters for primitive and enum classes
- [x] Manual migration
- [x] Destructive migration
- [ ] Validate Schema

### @TypeConverters
> [!CAUTION]
> This annotation can only accept converter `object` that implement `app.cash.sqldelight.ColumnAdapter`
- [x] `value`
- [ ] `builtInTypeConverters`

### @BuiltInTypeConverters
- [ ] `enums` (Enums are supported by default)
- [ ] `uuid`

### @AutoMigration
- [ ] `from`
- [ ] `to`
- [ ] `spec`
- [ ] Support auto migration functionality

## Additional Features
### @Mappers
- Used to map results returned by a dao to data classes that are not entities or primitives
- This annotation is meant to be used with `Database` class
- `value` accepts `object` that implements `KabinMapper<T>`

### Compound
- Classes that use `@Embedded` and `@Relation` annotations can be used with `@Insert`, `@Upsert`, `@Delete` and `@Update`
- `@Junction` inside a compound is automatically created and inserted as well

## Plans and Priorities
1. [ ] Clean and refactor `compiler` and `processor` logic, make it more flexible amd maintainable
2. [ ] Generate more optimized code
3. [ ] Fix bugs and issues
4. [ ] Implement more **Room** features, especially the essential ones for basic and simple apps
5. [ ] Add more features to make working with SQL easier and more interesting
6. [ ] Add multiplatform sample with UI