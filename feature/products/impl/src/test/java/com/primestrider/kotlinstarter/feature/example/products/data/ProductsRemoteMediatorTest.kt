package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProductsRemoteMediatorTest {
    private lateinit var database: ProductsDatabase
    private val api = FakeProductsApi(total = 30)
    private val state = PagingState<Int, ProductEntity>(emptyList(), null, PagingConfig(pageSize = 20, initialLoadSize = 20), 0)

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ProductsDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = database.close()

    private fun mediator() = ProductsRemoteMediator(api, database, Json)

    private suspend fun cachedIds() = database.productDao().all().map { it.id }

    @Test
    fun refresh_replaces_the_cache_with_the_first_page() =
        runTest {
            database.productDao().insertAll(listOf(productDto(99).toEntity(position = 0)))
            val result = mediator().load(LoadType.REFRESH, state)
            assertTrue(result is MediatorResult.Success)
            assertFalse((result as MediatorResult.Success).endOfPaginationReached)
            assertEquals((1L..20L).toList(), cachedIds())
        }

    @Test
    fun append_adds_the_next_page_and_reaches_the_end() =
        runTest {
            val mediator = mediator()
            mediator.load(LoadType.REFRESH, state)
            val result = mediator.load(LoadType.APPEND, state)
            assertTrue((result as MediatorResult.Success).endOfPaginationReached)
            assertEquals((1L..30L).toList(), cachedIds())
            assertEquals("products(20,20)", api.calls.last())
        }

    @Test
    fun offline_refresh_is_an_error_and_keeps_the_cache() =
        runTest {
            mediator().load(LoadType.REFRESH, state)
            api.offline = true
            val result = mediator().load(LoadType.REFRESH, state)
            assertTrue(result is MediatorResult.Error)
            assertEquals(20, cachedIds().size)
        }
}
