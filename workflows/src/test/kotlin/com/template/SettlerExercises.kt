package com.template

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.flows.*
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.fail

class SettlerExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode(CordaX500Name("ExchangeRateOracleService", "New York", "US"))
    private val d = network.createNode(CordaX500Name("SettlerOracleService", "New York", "US"))

    init {}


    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * TODO: Implement the [IOUNovateFlow].
     * Hint:
     * - In this flow, we will take a previously created IOU between Party A and Party B with
     * an amount of IOUTokens as the owed amount, and we'll novate it in terms of US Dollars.
     * We will use this flow as a subflow in the [IOUSettleFlow].
     *
     * - First, we need to get the notary and Exchange Rate Oracle [Party]
     * identity using the [serviceHub].
     * - Then, we build the transaction:
     * -- Use the existing IOU as input state.
     * -- Add a new IOUState as output that has the amount field replaced with the amount
     * in terms of USD. Use the [withNewAmount] helper method of IOUState and the
     * [FiatCurrency.getInstance] to get the Currency code.
     * -- Add the Novate command and be sure to add the Oracle as a required signer.
     * - Then, verify and sign the transaction.
     * - Finally, subflow the [ExchangeRateOracleFlow] and return the result.
     */
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun implementIOUNovateFlow() {
        // Create an IOU between party A and B using IOUToken
        val state = IOUState(
                Amount(100L, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.first(),
                b.info.legalIdentities.first())

        val issueFuture = a.startFlow(IOUIssueFlow(state))
        network.runNetwork()
        val signedTx = issueFuture.get()

        // Novate the IOU to be in terms of USD TokenType
        val stateAndRef = vaultQuery((signedTx.tx.outputStates.first() as IOUState).linearId)
        val future = a.startFlow(IOUNovateFlow(stateAndRef, "USD"))
        network.runNetwork()
        val stx = future.get()

        assertEquals(1, signedTx.tx.outputs.size)
        val output = stx.tx.outputStates.first() as IOUState
        assertEquals(150, output.amount.quantity)
        assertEquals("USD", output.amount.token.tokenIdentifier)
        assertEquals(false, output.settled)
        assertEquals(2, stx.sigs.size)
    }

    /**
     * TODO: Implement the [OffLedgerPaymentFlow].
     * Hint:
     * In this simple flow, we will use the [OffLedgerPaymentRailService] to
     * initiate the off ledger payment and return the resulting transaction ID.
     *
     * - First, use the [ServiceHub] to get the OffledgerPaymentRail [CordaService]
     * - Then, call the [makePayment] method with the off ledger account number and
     * amount of USD to send and return the result.
     */
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun implementOffLedgerPaymentFlow() {
        val issueFuture = a.startFlow(OffLedgerPaymentFlow("ABCD1234", 300.0))
        network.runNetwork()
        val transactionId = issueFuture.get()

        assertEquals("TEST_TRANSACTION_ID_1234", transactionId)
    }

    /**
     * TODO: Implement the [IOUVerifySettlementFlow].
     * Hint:
     * In this flow, we will use our Settler Oracle to verify that the off ledger
     * payment was and the correct amount was paid in US Dollars.
     *
     * - First, we need to get the notary and Settler Oracle [Party]
     * identity using the [serviceHub].
     * - Then, we build the transaction:
     * -- Use the existing IOU as input state.
     * -- Add a new IOUState as output that has the IOU marked as settled
     * using the [withSettled] helper method of IOUState.
     * -- Add the Settle command and be sure to add the Oracle as a required signer.
     * - Then, verify and sign the transaction.
     * - Finally, we need to get the Settler Oracle to verify the transaction and
     * sign the transaction.
     * -- Subflow the [SignOffLedgerPayment] flow. The Settler Oracle will use
     * the SettlerOracleService to verify the off ledger payment using the
     * transaction id. The flow will return the Oracle's signature.
     * -- Add the signature to the SignedTransaction and return this.
     */
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun implementIOUVerifySettlementFlow() {
        val state = IOUState(
                Amount(100L, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities[0],
                b.info.legalIdentities[0])

        val issueFuture = a.startFlow(IOUIssueFlow(state))
        network.runNetwork()
        val signedTx = issueFuture.get()

        assertEquals(1, signedTx.tx.outputs.size)

        val stateAndRef = vaultQuery((signedTx.tx.outputStates[0] as IOUState).linearId)
        val future = a.startFlow<SignedTransaction>(IOUSettleFlow(stateAndRef, "ABCD1234"))
        network.runNetwork()
        val stx = future.get()

        assertEquals(1, signedTx.tx.outputs.size)
        val output = stx.tx.outputStates[0] as IOUState
        assertEquals(150, output.amount.quantity)
        assertEquals("USD", output.amount.token.tokenIdentifier)
        assertEquals(true, output.settled)
        assertEquals(4, stx.sigs.size)
    }

    fun vaultQuery(linearId: UniqueIdentifier): StateAndRef<IOUState> {
        return a.services.vaultService.queryBy(IOUState::class.java,
                QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(linearId),
                        Vault.StateStatus.UNCONSUMED, null))
                .states[0]
    }

}
