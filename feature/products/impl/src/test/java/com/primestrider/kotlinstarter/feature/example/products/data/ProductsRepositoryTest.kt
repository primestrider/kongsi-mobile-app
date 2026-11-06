package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProductsRepositoryTest {
    private lateinit var database: ProductsDatabase
    private val api = FakeProductsApi(total = 50)
    private lateinit var repository: DefaultProductsRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ProductsDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = DefaultProductsRepository(api, database, Json)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun cached_products_show_when_offline() =
        runTest {
            // Online: the mediator fills the cache before the snapshot settles. The snapshot itself can return before
            // Room emits the page, so the assertions below read the cache the list pages from.
            repository.pager("").asSnapshot()
            api.offline = true
            // Offline the refresh fails (ProductsRemoteMediatorTest), and the list keeps paging the rows cached above.
            val page =
                TestPager(PagingConfig(pageSize = PAGE_SIZE), database.productDao().pagingSource()).refresh()
                    as PagingSource.LoadResult.Page
            val cached = page.data.map { it.toProduct() }
            assertEquals((1L..20L).toList(), cached.take(20).map { it.id })
            assertEquals(listOf("Great"), cached.first().reviews.map { it.comment })
        }

    @Test
    fun search_stays_online() =
        runTest {
            repository.pager("phone").asSnapshot()
            assertEquals(listOf("search(phone)"), api.calls)
        }

    @Test
    fun detail_falls_back_to_cache_offline() =
        runTest {
            repository.pager("").asSnapshot()
            api.offline = true
            assertEquals("Phone 3", repository.product(3).title)
        }

    @Test(expected = IOException::class)
    fun uncached_detail_offline_still_fails() =
        runTest {
            api.offline = true
            repository.product(3)
        }
}
