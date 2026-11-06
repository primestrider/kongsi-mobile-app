package com.primestrider.kotlinstarter.core.network

import javax.inject.Qualifier

/**
 * The `sha256/…` public-key pins of the [ApiBaseUrl] host, as a `Set<String>` provided by `:app` per flavour.
 * Empty means no pinning (the default). See `docs/architecture/networking.md`, "Certificate pinning".
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CertificatePins
