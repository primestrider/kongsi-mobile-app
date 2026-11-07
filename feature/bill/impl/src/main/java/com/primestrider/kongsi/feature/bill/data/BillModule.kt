package com.primestrider.kongsi.feature.bill.data

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillModule {
    @Binds
    abstract fun bindPaymentAccountRepository(impl: DefaultPaymentAccountRepository): PaymentAccountRepository

    companion object {
        @Provides
        @Singleton
        fun kongsiDatabase(
            @ApplicationContext context: Context,
        ): KongsiDatabase = Room.databaseBuilder(context, KongsiDatabase::class.java, "kongsi.db").build()

        @Provides
        fun paymentAccountDao(database: KongsiDatabase): PaymentAccountDao = database.paymentAccountDao()
    }
}
