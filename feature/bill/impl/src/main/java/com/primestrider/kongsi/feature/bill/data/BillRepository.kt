package com.primestrider.kongsi.feature.bill.data

import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Bills as [Bill] models. Room is the source of truth: a draft saved here survives leaving the flow or the app. */
interface BillRepository {
    /** Every bill, newest first. */
    val bills: Flow<List<Bill>>

    fun observe(id: Long): Flow<Bill?>

    suspend fun get(id: Long): Bill?

    /**
     * Adds [bill] when its id is 0 (stamping `createdAt`), otherwise replaces the stored one and everything in it,
     * keeping the `createdAt` it carries. People and items with id 0 are added. An item's `sharedBy` refers to people
     * by id, so people must be saved (and have an id) before items can be shared between them. Returns the bill's id.
     */
    suspend fun save(bill: Bill): Long

    /** Deletes the bill with its items and people and returns it (for undo), or null when it did not exist. */
    suspend fun delete(id: Long): Bill?
}

/** [BillRepository] on Room; [clock] stamps `createdAt` and is replaceable in tests. */
class DefaultBillRepository(
    private val dao: BillDao,
    private val clock: () -> Long,
) : BillRepository {
    @Inject
    constructor(dao: BillDao) : this(dao, System::currentTimeMillis)

    override val bills: Flow<List<Bill>> = dao.observeAll().map { rows -> rows.map { it.toBill() } }

    override fun observe(id: Long): Flow<Bill?> = dao.observe(id).map { it?.toBill() }

    override suspend fun get(id: Long): Bill? = dao.get(id)?.toBill()

    override suspend fun save(bill: Bill): Long {
        val stamped = if (bill.id == 0L) bill.copy(createdAt = clock()) else bill
        return dao.replace(
            bill = stamped.toEntity(),
            people = stamped.people.mapIndexed { index, person -> person.toEntity(stamped.id, index) },
            items = stamped.items.mapIndexed { index, item -> item.toEntity(stamped.id, index) },
            sharers = stamped.items.map { it.sharedBy },
        )
    }

    override suspend fun delete(id: Long): Bill? = get(id)?.also { dao.delete(id) }
}

fun BillWithParts.toBill(): Bill {
    val sharers = shares.groupBy({ it.itemId }, { it.personId })
    return Bill(
        id = bill.id,
        title = bill.title,
        createdAt = bill.createdAt,
        currency = bill.currency,
        payerPersonId = bill.payerPersonId,
        people = people.sortedBy { it.position }.map { Person(id = it.id, name = it.name, phone = it.phone) },
        items =
            items.sortedBy { it.position }.map { item ->
                BillItem(
                    id = item.id,
                    name = item.name,
                    totalMinor = item.totalMinor,
                    sharedBy = sharers[item.id].orEmpty().toSet(),
                    quantity = item.quantity,
                    serviceable = item.serviceable,
                    taxable = item.taxable,
                )
            },
        service = BillCharge(bill.serviceMinor, bill.serviceRateBps),
        tax = BillCharge(bill.taxMinor, bill.taxRateBps),
        discountMinor = bill.discountMinor,
        receiptTotalMinor = bill.receiptTotalMinor,
    )
}

fun Bill.toEntity(): BillEntity =
    BillEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        currency = currency,
        payerPersonId = payerPersonId,
        serviceMinor = service.amountMinor,
        serviceRateBps = service.rateBps,
        taxMinor = tax.amountMinor,
        taxRateBps = tax.rateBps,
        discountMinor = discountMinor,
        receiptTotalMinor = receiptTotalMinor,
    )

private fun Person.toEntity(
    billId: Long,
    position: Int,
) = PersonEntity(id = id, billId = billId, name = name, phone = phone, position = position)

private fun BillItem.toEntity(
    billId: Long,
    position: Int,
) = ItemEntity(
    id = id,
    billId = billId,
    name = name,
    quantity = quantity,
    totalMinor = totalMinor,
    serviceable = serviceable,
    taxable = taxable,
    position = position,
)
