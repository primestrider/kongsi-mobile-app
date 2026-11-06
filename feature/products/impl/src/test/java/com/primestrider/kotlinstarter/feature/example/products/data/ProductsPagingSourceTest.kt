package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.PagingSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ProductsPagingSourceTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer
    private lateinit var api: ProductsApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ProductsApi::class.java)
    }

    @After
    fun tearDown() {
        runCatching { server.close() }
    }

    private fun page(
        count: Int,
        total: Int,
        skip: Int,
    ): String {
        val products =
            (1..count).joinToString(",") { i ->
                val id = skip + i
                """{"id":$id,"title":"P$id","description":"d","category":"c","price":1.5,"discountPercentage":0.0,""" +
                    """"rating":4.5,"stock":3,"thumbnail":"https://x/$id.png"}"""
            }
        return """{"products":[$products],"total":$total,"skip":$skip,"limit":20}"""
    }

    private fun respond(
        code: Int,
        body: String,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build(),
    )

    private suspend fun load(
        query: String,
        key: Int?,
    ) = ProductsPagingSource(api, query, json).load(PagingSource.LoadParams.Refresh(key, PAGE_SIZE, false))

    @Test
    fun blank_query_lists_products_from_the_start() =
        runTest {
            respond(200, page(20, 45, 0))
            load("", null)
            val url = server.takeRequest().url
            assertEquals("/products", url.encodedPath)
            assertEquals("20", url.queryParameter("limit"))
            assertEquals("0", url.queryParameter("skip"))
        }

    @Test
    fun query_is_trimmed_and_searched() =
        runTest {
            respond(200, page(1, 1, 0))
            load("  phone ", null)
            val url = server.takeRequest().url
            assertEquals("/products/search", url.encodedPath)
            assertEquals("phone", url.queryParameter("q"))
            assertEquals("20", url.queryParameter("limit"))
            assertEquals("0", url.queryParameter("skip"))
        }

    @Test
    fun full_page_points_at_the_next_skip() =
        runTest {
            respond(200, page(20, 45, 0))
            val result = load("", null) as PagingSource.LoadResult.Page
            assertEquals(20, result.data.size)
            assertEquals(20, result.nextKey)
            assertNull(result.prevKey)
        }

    @Test
    fun last_short_page_ends_the_list() =
        runTest {
            respond(200, page(5, 45, 40))
            val result = load("", 40) as PagingSource.LoadResult.Page
            assertEquals("40", server.takeRequest().url.queryParameter("skip"))
            assertNull(result.nextKey)
        }

    @Test
    fun server_error_is_a_load_error() =
        runTest {
            respond(500, """{"message":"boom"}""")
            assertTrue(load("", null) is PagingSource.LoadResult.Error)
        }
}
