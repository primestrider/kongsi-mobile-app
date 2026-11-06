# Storage: DataStore and Room

Two local stores, for two kinds of data.

| Data | Store | Where in this app |
| --- | --- | --- |
| A few keyed values: theme, flags | DataStore Preferences | `core/datastore/` (module, keys, `DataStoreFiles`, `ThemePreferenceRepository`), `feature/example/auth/DemoTokenSettings` |
| Secrets | DataStore + Keystore AES/GCM | `core/auth/` (`SessionRepository`, see `auth-session.md`) |
| Structured records you list, filter, or query | Room (SQLite) | `feature/todos/impl` (`ExampleDatabase`, `example.db`), `feature/devices/impl` (`DevicesDatabase`, `devices.db`) |

Rule of thumb: if you would ever write `WHERE` or `ORDER BY`, it belongs in Room.

## Where a database lives

A `@Database` class lists its entities, so it belongs in the feature module that owns them. Each example feature that stores records has its own database: Todos keeps `ExampleDatabase` (`example.db`) in `feature/todos/impl`, and the Device APIs SQLite demo keeps `DevicesDatabase` (`devices.db`) in `feature/devices/impl`. Removing a feature removes its database. Each module exports its schemas to `<module>/schemas/` (`ksp { arg("room.schemaLocation", ...) }` in its build file).

The two databases used to be one: `ExampleDatabase` v2 also held the `transactions` table. Version 3 drops it (`MIGRATION_2_3`), so an upgraded install keeps its todos and starts the transactions demo empty.

For your own app, either:

- one database per feature module, when features do not share tables (the default); or
- a shared core module with one database listing entities from several features, when they join across tables.

Provide the database as a `@Singleton` from a Hilt module and the DAOs from it (`TodosModule`).

## Pieces

```
TodoEntity (@Entity)  →  TodoDao (@Dao)  →  ExampleDatabase (@Database)  →  TodosRepository  →  TodosViewModel
```

- DAO queries that observe return `Flow<List<…>>`; Room re-emits after every write to the table, so the UI never reloads by hand.
- Writes are `suspend` functions; Room runs them off the main thread.
- The repository adds app rules (trimming, ids, timestamps) and hides the DAO from ViewModels.
- The ViewModel combines the DAO flow with UI state. Start the DAO flow with `onStart { emit(null) }` so input stays responsive before the first query answers, and use `null` to tell "loading" from "empty" (no empty-state flash on open).

## Migrations

- `exportSchema = true` and the KSP argument `room.schemaLocation` (in the feature module's build file) write each version's schema to `<module>/schemas/<database class>/<version>.json`. Commit these files.
- Changing an entity means bumping `version` and adding a `Migration(from, to)` to the builder (`.addMigrations(...)`), or an `@AutoMigration` when Room can infer it from the two exported schemas.
- Never ship `fallbackToDestructiveMigration()` for user data: it silently deletes everything on upgrade.

## Testing

Use an in-memory database under Robolectric:

```kotlin
val database = Room.inMemoryDatabaseBuilder(context, ExampleDatabase::class.java)
    .allowMainThreadQueries()
    .build()
```

- DAO tests assert the SQL (ordering, counts) against it (`TodoDaoTest`).
- ViewModel tests use the real repository on the same in-memory database and a fake clock (`TodosViewModelTest`); wait for the state with `state.first { it.loaded && … }`, because Room answers on its own executor.
- DataStore tests use `InMemoryPreferencesDataStore` from `core:testing` (file-backed DataStore is unreliable in JVM tests on Windows).

## Adding a table: the v1→v2 migration and its test

`ExampleDatabase` went from v1 (`todos`) to v2 by adding the `transactions` table for the Room transactions demo (v3 later moved that table to `DevicesDatabase`). The steps, in order:

1. Add the `@Entity` and a DAO, list the entity in `@Database(entities = …, version = 2)` and expose `abstract fun transactionDao()`. Building once writes `<module>/schemas/<database class>/2.json`; commit it next to `1.json`.
2. Write the migration with the SQL copied from the new schema's `createSql` (the table must match the entity exactly, or Room throws on open):

   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, …)")
       }
   }
   ```

3. Register it: `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2)`. Existing rows in other tables are untouched.
4. Test the upgrade with `MigrationTestHelper` (`room-testing`), which replays the exported `1.json` (`ExampleDatabaseMigrationTest`): create a v1 database, insert a todo, run `runMigrationsAndValidate(2, listOf(MIGRATION_1_2))`, and assert the todo is still there and the new table exists. `validate` also compares the result with `2.json`, so a SQL/entity mismatch fails the test.

Under Robolectric the helper needs two things: the schema JSON files as assets (the module's build file adds `$projectDir/schemas` to the `debug` source set, because unit tests only see the variant's merged assets), and the driver-based constructor (`MigrationTestHelper(instrumentation, file, AndroidSQLiteDriver(), ExampleDatabase::class, { ExampleDatabase_Impl() })`), since the older name-based constructor trips over Robolectric's database paths. `./gradlew test` runs `testDebugUnitTest` only (AGP 9 creates debug unit-test tasks only), where the assets exist; the test still guards itself with `Assume.assumeTrue(BuildConfig.DEBUG)` for any release unit-test run.
