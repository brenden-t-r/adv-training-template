package com.template.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.IOUContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity


/**
 * The IOU State object, with the following properties:
 * - [amount] The amount owed by the [borrower] to the [lender]
 * - [lender] The lending party.
 * - [borrower] The borrowing party.
 * - [paid] Records how much of the [amount] has been paid.
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *   the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *   except at issuance/termination.
 */
@BelongsToContract(IOUContract::class)
data class IOUState(val amount: Amount<TokenType>,
                    val lender: Party,
                    val borrower: Party,
                    override val linearId: UniqueIdentifier = UniqueIdentifier(),
                    val settled: Boolean = false
): QueryableState, LinearState {
    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOf(lender, borrower)

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
     */
    fun withNewLender(newLender: Party) = copy(lender = newLender)
    fun withNewAmount(newAmount: Amount<TokenType>) = copy(amount = newAmount)
    fun withSettled() = copy(settled = true)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return if (schema is IOUCustomSchema) {
            IOUCustomSchema.PersistentIOU(linearId.id, lender.name.toString(),
                    borrower.name.toString(), amount.quantity, settled)
        } else {
            throw IllegalArgumentException("Unrecognised schema \$schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(IOUCustomSchema())
    }

}

class IOUCustomSchema : MappedSchema(IOUCustomSchema::class.java, 1, listOf(PersistentIOU::class.java)) {
    @Entity
    class PersistentIOU(
            @Column(nullable = false) val linearId: UUID,
            @Column(nullable = false) val lender: String,
            @Column(nullable = false) val borrower: String,
            @Column(nullable = false) val amount: Long,
            @Column(nullable = false) val settled: Boolean
    ) : PersistentState() {}
}
