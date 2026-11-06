package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

const val DEVICES_DATABASE_NAME = "devices.db"

/**
 * The Device APIs feature's own database (the SQLite demo's transactions), so it is deleted with the feature.
 * Bump [version] together with a `Migration`; schemas are exported next to the module's build file.
 */
@Database(entities = [TransactionEntity::class], version = 1, exportSchema = true)
abstract class DevicesDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

@Module
@InstallIn(SingletonComponent::class)
object DevicesDatabaseModule {
    @Provides
    @Singleton
    fun devicesDatabase(
        @ApplicationContext context: Context,
    ): DevicesDatabase = Room.databaseBuilder(context, DevicesDatabase::class.java, DEVICES_DATABASE_NAME).build()

    @Provides
    fun transactionDao(database: DevicesDatabase): TransactionDao = database.transactionDao()
}
