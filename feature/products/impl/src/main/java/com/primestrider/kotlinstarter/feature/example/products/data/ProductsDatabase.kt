package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json

/**
 * The cached product list (`products.db`), owned by this feature so deleting the feature deletes it. It is only a
 * cache that every refresh replaces, so a schema change may rebuild it (`fallbackToDestructiveMigration`) instead of
 * migrating; schemas are still exported to `feature/products/impl/schemas`.
 */
@Database(entities = [ProductEntity::class, RemoteKeyEntity::class], version = 1, exportSchema = true)
@TypeConverters(ProductConverters::class)
abstract class ProductsDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}

/** One cached product; [position] keeps the server's order. */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val position: Int,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String?,
    val thumbnail: String,
    val images: List<String>,
    val reviews: List<ReviewDto>,
)

/** Where the next page of the list called [label] starts; [nextOffset] is null once the end is reached. */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val label: String,
    val nextOffset: Int?,
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY position")
    fun pagingSource(): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products ORDER BY position")
    suspend fun all(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun product(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("SELECT * FROM remote_keys WHERE label = :label")
    suspend fun remoteKey(label: String): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRemoteKey(key: RemoteKeyEntity)
}

/** Room stores columns, not lists: the image URLs and reviews are kept as JSON text. */
class ProductConverters {
    @TypeConverter
    fun fromStrings(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStrings(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromReviews(value: List<ReviewDto>): String = Json.encodeToString(value)

    @TypeConverter
    fun toReviews(value: String): List<ReviewDto> = Json.decodeFromString(value)
}
