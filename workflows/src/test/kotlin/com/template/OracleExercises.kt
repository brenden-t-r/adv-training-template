package com.template

import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.contracts.IOUContract
import com.template.flows.*
import com.template.flows.ExchangeRateOracleFlow
import com.template.states.IOUState
import com.template.states.IOUToken
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.function.Predicate
import kotlin.test.assertEquals

class OracleExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode(CordaX500Name("ExchangeRateOracleService", "New York", "US"))

    init {
        listOf(a, b, c).forEach {
            it.registerInitiatedFlow(QueryHandler::class.java)
            it.registerInitiatedFlow(SignHandler::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()
//
//    /**
//     * Task 1
//     * TODO: Implement a [query] method for the [ExchangeRateOracleService].
//     * Hint:
//     * - The query method must take a [String] currency code argument and return a [Double] representing
//     *   the exchange rate received from the off-ledger data source.
//     * - For this simple example you can just return a hard-coded value, but in practice you
//     *   would likely query an external API or file attachment to get the current value.
//     */
    @Test
    fun oracleServiceQuery() {
        val oracle = ExchangeRateOracleService(services = a.services)
        assert(ExchangeRateOracleService::class.java.kotlin.members.any { it.name == "query" })
        assertEquals(oracle.query("USD")::class.java, Double::class.java);
    }
//
//    /**
//     * Task 2
//     * TODO: Implement a [sign] method for the [ExchangeRateOracleService].
//     * Hint:
//     * - The sign method must take a [FilteredTransaction] argument and return the oracle's signature
//     *   as a [TransactionSignature].
//     * - The sign method must verify that the rate provided in the [Exchange] Command is valid.
//     *
//     * TODO: Hints
//     *
//     */
    @Test
    fun oracleServiceSign() {
        val oracle = ExchangeRateOracleService(services = c.services)
        assert(ExchangeRateOracleService::class.java.kotlin.members.any { it.name == "sign" })

        val notary = a.services.networkMapCache.notaryIdentities.get(0)
        val builder = TransactionBuilder(notary = notary)
        val rate = oracle.query("USD")
        val output = IOUTokenState(
                Amount(3, IOUToken("CUSTOM_TOKEN", 2)),
                a.services.myInfo.legalIdentities.get(0),
                b.services.myInfo.legalIdentities.get(0))
        builder.addCommand(
                IOUContract.Commands.Exchange("USD", rate /* Invalid rate */),
                listOf(c.info.legalIdentities.get(0).owningKey, a.info.legalIdentities.get(0).owningKey))
        builder.addOutputState(output, IOUContract.IOU_CONTRACT_ID)

        val ptx = a.services.signInitialTransaction(builder)

        val ftx = ptx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> c.info.legalIdentities.get(0).owningKey in it.signers
                        && it.value is IOUContract.Commands.Exchange
                else -> false
            }
        })

        val oracleSig = oracle.sign(ftx)
        assert(oracleSig.isValid(ftx.id))
    }
//
//
//    // TODO: Implement the createFilteredTransaction() method as a separate exercise
//
//    /**
//     * Task 3
//     * TODO: Implement the [OracleExercises].
//     * Hint:
//     *  -
//     */
    @Test
    fun oracleFlow() {
        val notary = a.services.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)

        val future = a.startFlow(QueryExchangeRate(c.info.legalIdentities.get(0), "USD"))
        network.runNetwork()
        val resultFromOracle = future.getOrThrow()

        // Update builder with value
        builder.addCommand(
                IOUContract.Commands.Exchange("USD", resultFromOracle),
                listOf(c.info.legalIdentities.get(0).owningKey, a.info.legalIdentities.get(0).owningKey)
        )
        val iouTokenState = IOUTokenState(
                Amount(5, IOUToken("CUSTOM_TOKEN", 2)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        FiatCurrency.getInstance("USD")
        builder.addOutputState(IOUState(
                Amount(iouTokenState.amount.quantity, FiatCurrency.getInstance("USD")),
                iouTokenState.lender,
                iouTokenState.borrower), IOUContract.IOU_CONTRACT_ID)
        val ptx = a.services.signInitialTransaction(builder)

        val oracleFuture = a.startFlow(ExchangeRateOracleFlow(ptx))
        network.runNetwork()
        val signedTx = oracleFuture.getOrThrow()

        // Check that oracle signature is present
        signedTx.verifyRequiredSignatures()

    }

}