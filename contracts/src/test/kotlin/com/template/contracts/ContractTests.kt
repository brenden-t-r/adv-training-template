package com.template.contracts

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearPointer
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.core.transactions.LedgerTransaction.InOutGroup
import net.corda.core.transactions.LedgerTransaction
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()

    val ALICE = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))
    val BOB = TestIdentity(CordaX500Name(organisation = "Bob", locality = "TestCity", country = "US"))

    /**
     * TODO: Implement state grouping for the Merge command.
     * Hint:
     * - Use State Grouping in the [verify] function of the [IOUContract] to validate a Merge operation.
     * - The [IOUState] has been updated to use a Token from the Token SDK instead of a Currency.
     * We'll learn more about the Token SDK later in the course.
     * - We just need to make sure the the [IOUState]'s [amount.token.tokenIdentifier] field matches for
     * any tokens that are being merged.
     * - First, we need to use the [groupStates] function of the [tx] object.
     * -- Parametrize the groupStates function by the [IOUState] and [String] using the <IOUState, String> notation.
     * This specifies that we are grouping [IOUState]s and the that grouping key is a [String]
     * -- Within our groupStates {} clause, we need to specify the grouping key.
     * In our case this will be the [amount.token.tokenIdentifier].
     * -- The return type from the [groupStates] function is a list of [InOutGroup].
     * - Next, we'll loop through the [InOutGroup]s and include that the total amount quantity
     * of the [inputs] matches that of the [outputs] (i.e. two IOU's of 5 can be merged to 1 IOU of quantity 10).
     */
    @Test
    fun `state grouping test`() {
        val token1 = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)), ALICE.party, BOB.party)
        val token2 = IOUState(Amount(100, IOUToken("IOU_TOKEN", 2)), ALICE.party, BOB.party)
        val token3 = IOUState(Amount(100, IOUToken("DIFFERENT_TOKEN", 2)), ALICE.party, BOB.party)
        val token4 = IOUState(Amount(200, IOUToken("DIFFERENT_TOKEN", 2)), ALICE.party, BOB.party)

        val output1 = IOUState(Amount(150, IOUToken("IOU_TOKEN", 2)), ALICE.party, BOB.party)
        val output2 = IOUState(Amount(300, IOUToken("DIFFERENT_TOKEN", 2)), ALICE.party, BOB.party)

        val invalidOutput = IOUState(Amount(301, IOUToken("DIFFERENT_TOKEN", 2)), ALICE.party, BOB.party)

        ledgerServices.ledger {
            transaction {
                input(IOUContract.IOU_CONTRACT_ID, token1)
                input(IOUContract.IOU_CONTRACT_ID, token2)
                input(IOUContract.IOU_CONTRACT_ID, token3)
                input(IOUContract.IOU_CONTRACT_ID, token4)
                output(IOUContract.IOU_CONTRACT_ID, output2)
                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Merge())
                this.fails() // Not the same token identifier

            }
            transaction {
                input(IOUContract.IOU_CONTRACT_ID, token1)
                input(IOUContract.IOU_CONTRACT_ID, token2)
                input(IOUContract.IOU_CONTRACT_ID, token3)
                input(IOUContract.IOU_CONTRACT_ID, token4)
                output(IOUContract.IOU_CONTRACT_ID, output1)
                output(IOUContract.IOU_CONTRACT_ID, invalidOutput)
                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Merge())
                this `fails with` "Output total must equal input total for each token identifier" // Incorrect merge sum
            }
            transaction {
                input(IOUContract.IOU_CONTRACT_ID, token1)
                input(IOUContract.IOU_CONTRACT_ID, token2)
                input(IOUContract.IOU_CONTRACT_ID, token3)
                input(IOUContract.IOU_CONTRACT_ID, token4)
                output(IOUContract.IOU_CONTRACT_ID, output1)
                output(IOUContract.IOU_CONTRACT_ID, output2)
                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Merge())
                this.verifies()
            }
        }
    }
}