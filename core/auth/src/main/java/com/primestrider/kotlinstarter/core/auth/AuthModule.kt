package com.primestrider.kotlinstarter.core.auth

import com.primestrider.kotlinstarter.core.network.AppInterceptors
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Authenticator
import okhttp3.Interceptor
import java.util.Optional
import javax.inject.Qualifier

/** Qualifier a feature puts on its `@Binds` of the backend [AuthAdapter] (e.g. DummyJSON in the account example). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthBackend

/**
 * Plugs auth into the app client of `core:network` (bearer interceptor + 401 refresh) and resolves the
 * [AuthAdapter]: the feature's `@AuthBackend` one, or [NotConfiguredAuthAdapter] when no feature binds one.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @IntoSet
    @AppInterceptors
    abstract fun bindAuthInterceptor(impl: AuthInterceptor): Interceptor

    @Binds
    abstract fun bindAuthenticator(impl: TokenAuthenticator): Authenticator

    @BindsOptionalOf
    @AuthBackend
    abstract fun authBackend(): AuthAdapter

    companion object {
        @Provides
        fun authAdapter(
            @AuthBackend backend: Optional<AuthAdapter>,
        ): AuthAdapter = backend.orElse(NotConfiguredAuthAdapter)
    }
}
