package com.primestrider.kongsi.feature.bill.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A bill and its charges. Rates are in basis points (10% = 1000) and null when the charge was typed as an amount. */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val title: String,
    val createdAt: Long,
    val currency: String,
    /** A person of this bill; not a foreign key, since people reference the bill. */
    val payerPersonId: Long?,
    val serviceMinor: Long,
    val serviceRateBps: Int?,
    val taxMinor: Long,
    val taxRateBps: Int?,
    val discountMinor: Long,
    val receiptTotalMinor: Long?,
    /** Whether the payment accounts for this bill were chosen; until then the default accounts apply. */
    @ColumnInfo(defaultValue = "0") val accountsChosen: Boolean,
)

/** One receipt line; [position] keeps the receipt's order. */
@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("billId")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val billId: Long,
    val name: String,
    val quantity: Int,
    val totalMinor: Long,
    val serviceable: Boolean,
    val taxable: Boolean,
    val position: Int,
)

/** Someone on a bill; [position] keeps the order they were added in. */
@Entity(
    tableName = "bill_people",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("billId")],
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val billId: Long,
    val name: String,
    val phone: String?,
    val position: Int,
)

/**
 * Who had which item (many-to-many). [billId] is redundant with the item's, but lets a bill load its shares in one
 * relation; removing the bill, the item or the person removes the share.
 */
@Entity(
    tableName = "item_shares",
    primaryKeys = ["itemId", "personId"],
    foreignKeys = [
        ForeignKey(entity = BillEntity::class, parentColumns = ["id"], childColumns = ["billId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("billId"), Index("personId")],
)
data class ItemShareEntity(
    val billId: Long,
    val itemId: Long,
    val personId: Long,
)

/**
 * A payment account chosen for a bill, listed in its WhatsApp messages. Deleting the bill or the account removes the
 * choice, so a deleted account never shows up in a later message.
 */
@Entity(
    tableName = "bill_payment_accounts",
    primaryKeys = ["billId", "accountId"],
    foreignKeys = [
        ForeignKey(entity = BillEntity::class, parentColumns = ["id"], childColumns = ["billId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = PaymentAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class BillPaymentAccountEntity(
    val billId: Long,
    val accountId: Long,
)

/** A bill row with everything that belongs to it, loaded in one transaction. */
data class BillWithParts(
    @Embedded val bill: BillEntity,
    @Relation(parentColumn = "id", entityColumn = "billId") val items: List<ItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "billId") val people: List<PersonEntity>,
    @Relation(parentColumn = "id", entityColumn = "billId") val shares: List<ItemShareEntity>,
    @Relation(parentColumn = "id", entityColumn = "billId") val accounts: List<BillPaymentAccountEntity>,
)
