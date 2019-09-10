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
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()

    val ALICE = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))
    val BOB = TestIdentity(CordaX500Name(organisation = "Bob", locality = "TestCity", country = "US"))

    /**
     * TODO: Implement state grouping for the Merge command.
     * Hint:
     * -
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