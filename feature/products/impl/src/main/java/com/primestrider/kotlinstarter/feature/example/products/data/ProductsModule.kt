package com.primestrider.kotlinstarter.feature.example.products.data

import android.content.Context
import androidx.room.Room
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.example.ProductsRoute
import com.primestrider.kotlinstarter.feature.products.impl.R
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductsModule {
    @Binds
    abstract fun bindRepository(impl: DefaultProductsRepository): ProductsRepository

    companion object {
        @Provides
        @Singleton
        fun productsApi(retrofit: Retrofit): ProductsApi = retrofit.create(ProductsApi::class.java)

        /** The cached product list; only a cache, so a schema change rebuilds it instead of migrating. */
        @Provides
        @Singleton
        fun productsDatabase(
            @ApplicationContext context: Context,
        ): ProductsDatabase =
            Room
                .databaseBuilder(context, ProductsDatabase::class.java, "products.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /** The Products row on Home. */
        @Provides
        @IntoSet
        fun homeEntry(): CatalogEntry =
            CatalogEntry(R.string.products_title, R.string.products_subtitle, ProductsRoute, CatalogSections.FEATURES, order = 10)
    }
}
