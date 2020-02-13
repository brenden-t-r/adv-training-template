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
        val future = a.startFlow<SignedTransaction>(IOUNovateFlow(stateAndRef, "USD"))
        network.runNetwork()
        val stx = future.get()

        assertEquals(1, signedTx.tx.outputs.size)
        val output = stx.tx.outputStates.first() as IOUState
        assertEquals(150, output.amount.quantity)
        assertEquals("USD", output.amount.token.tokenIdentifier)
        assertEquals(false, output.settled)
        assertEquals(2, stx.sigs.size)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun implementOffLedgerPaymentFlow() {
        val issueFuture = a.startFlow(OffLedgerPaymentFlow("ABCD1234", 300.0))
        network.runNetwork()
        val transactionId = issueFuture.get()

        assertEquals("TEST_TRANSACTION_ID_1234", transactionId)
    }

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
