package com.primestrider.kongsi.feature.bill.data

import android.content.Context
import androidx.room.Room
import com.primestrider.kongsi.feature.bill.contacts.ContactsRepository
import com.primestrider.kongsi.feature.bill.contacts.DefaultContactsRepository
import com.primestrider.kongsi.feature.bill.ocr.MlKitReceiptScanner
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanner
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

    @Binds
    abstract fun bindBillRepository(impl: DefaultBillRepository): BillRepository

    @Binds
    abstract fun bindFrequentPeopleRepository(impl: DefaultFrequentPeopleRepository): FrequentPeopleRepository

    @Binds
    abstract fun bindContactsRepository(impl: DefaultContactsRepository): ContactsRepository

    @Binds
    abstract fun bindReceiptScanner(impl: MlKitReceiptScanner): ReceiptScanner

    companion object {
        @Provides
        @Singleton
        fun kongsiDatabase(
            @ApplicationContext context: Context,
        ): KongsiDatabase = Room.databaseBuilder(context, KongsiDatabase::class.java, "kongsi.db").build()

        @Provides
        fun paymentAccountDao(database: KongsiDatabase): PaymentAccountDao = database.paymentAccountDao()

        @Provides
        fun billDao(database: KongsiDatabase): BillDao = database.billDao()

        @Provides
        fun frequentPersonDao(database: KongsiDatabase): FrequentPersonDao = database.frequentPersonDao()
    }
}
