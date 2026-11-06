package com.primestrider.kotlinstarter.feature.example.auth

import com.primestrider.kotlinstarter.core.auth.AuthAdapter
import com.primestrider.kotlinstarter.core.auth.AuthBackend
import com.primestrider.kotlinstarter.core.network.RefreshRetrofit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExampleAuthModule {
    @Binds
    @AuthBackend
    abstract fun bindAuthAdapter(impl: DummyJsonAuthAdapter): AuthAdapter

    companion object {
        @Provides
        @Singleton
        fun dummyJsonApi(retrofit: Retrofit): DummyJsonApi = retrofit.create(DummyJsonApi::class.java)

        @Provides
        @Singleton
        fun dummyJsonRefreshApi(
            @RefreshRetrofit retrofit: Retrofit,
        ): DummyJsonRefreshApi = retrofit.create(DummyJsonRefreshApi::class.java)
    }
}
