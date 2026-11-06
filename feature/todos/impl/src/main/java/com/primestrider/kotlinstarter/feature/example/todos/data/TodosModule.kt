package com.primestrider.kotlinstarter.feature.example.todos.data

import android.content.Context
import androidx.room.Room
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.example.TodosRoute
import com.primestrider.kotlinstarter.feature.example.todos.widget.GlanceTodosWidgetUpdater
import com.primestrider.kotlinstarter.feature.example.todos.widget.TodosWidgetUpdater
import com.primestrider.kotlinstarter.feature.todos.impl.R
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TodosModule {
    @Binds
    abstract fun bindRepository(impl: DefaultTodosRepository): TodosRepository

    @Binds
    abstract fun bindWidgetUpdater(impl: GlanceTodosWidgetUpdater): TodosWidgetUpdater

    companion object {
        @Provides
        @Singleton
        fun exampleDatabase(
            @ApplicationContext context: Context,
        ): ExampleDatabase =
            Room
                .databaseBuilder(context, ExampleDatabase::class.java, "example.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        @Provides
        fun todoDao(database: ExampleDatabase): TodoDao = database.todoDao()

        /** The Todos row on Home. */
        @Provides
        @IntoSet
        fun homeEntry(): CatalogEntry =
            CatalogEntry(R.string.todos_title, R.string.todos_subtitle, TodosRoute, CatalogSections.FEATURES, order = 20)
    }
}
