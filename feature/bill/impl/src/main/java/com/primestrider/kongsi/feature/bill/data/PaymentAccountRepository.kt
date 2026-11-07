package com.primestrider.kongsi.feature.bill.data

import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The bill creator's payment accounts as [PaymentAccount] models. */
interface PaymentAccountRepository {
    /** Every account in the order they were added. */
    val accounts: Flow<List<PaymentAccount>>

    suspend fun get(id: Long): PaymentAccount?

    /**
     * Adds [account] when its id is 0 (stamping `createdAt`), otherwise replaces the stored one, keeping the
     * `createdAt` it carries (so restoring a deleted account puts it back in its place). Returns the id.
     */
    suspend fun save(account: PaymentAccount): Long

    /** Deletes the account and returns it (for undo), or null when it did not exist. */
    suspend fun delete(id: Long): PaymentAccount?
}

/** [PaymentAccountRepository] on Room; [clock] stamps `createdAt` and is replaceable in tests. */
class DefaultPaymentAccountRepository(
    private val dao: PaymentAccountDao,
    private val clock: () -> Long,
) : PaymentAccountRepository {
    @Inject
    constructor(dao: PaymentAccountDao) : this(dao, System::currentTimeMillis)

    override val accounts: Flow<List<PaymentAccount>> =
        dao.observeAll().map { rows -> rows.map { it.toPaymentAccount() } }

    override suspend fun get(id: Long): PaymentAccount? = dao.get(id)?.toPaymentAccount()

    override suspend fun save(account: PaymentAccount): Long =
        if (account.id == 0L) {
            dao.upsert(account.copy(createdAt = clock()).toEntity())
        } else {
            dao.upsert(account.toEntity())
            account.id
        }

    override suspend fun delete(id: Long): PaymentAccount? = get(id)?.also { dao.delete(id) }
}

fun PaymentAccountEntity.toPaymentAccount(): PaymentAccount =
    PaymentAccount(
        id = id,
        type = PaymentAccountType.valueOf(type),
        provider = provider,
        number = number,
        holderName = holderName,
        isDefault = isDefault,
        createdAt = createdAt,
    )

fun PaymentAccount.toEntity(): PaymentAccountEntity =
    PaymentAccountEntity(
        id = id,
        type = type.name,
        provider = provider,
        number = number,
        holderName = holderName,
        isDefault = isDefault,
        createdAt = createdAt,
    )
