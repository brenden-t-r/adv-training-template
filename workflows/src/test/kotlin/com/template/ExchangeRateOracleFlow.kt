package com.template

import com.template.contracts.ExchangeRateContract
import com.template.contracts.IOUContract
import com.template.flows.ExchangeRateOracleService
import com.template.flows.Responder
import com.template.states.IOUToken
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.function.Predicate
import kotlin.test.assertEquals

class ExchangeRateOracleFlow {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b, c).forEach {
            it.registerInitiatedFlow(Responder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * Task 1
     * TODO: Implement a [query] method for the [ExchangeRateOracleService].
     * Hint:
     * - The query method must take a [String] currency code argument and return a [Double] representing
     *   the exchange rate received from the off-ledger data source.
     * - For this simple example you can just return a hard-coded value, but in practice you
     *   would likely query an external API or file attachment to get the current value.
     */
    @Test
    fun oracleServiceQuery() {
        val oracle = ExchangeRateOracleService(services = a.services)
        assert(ExchangeRateOracleService::class.java.kotlin.members.any { it.name == "query" })
        assertEquals(oracle.query("USD")::class.java, Double::class.java);
    }

    /**
     * Task 2
     * TODO: Implement a [sign] method for the [ExchangeRateOracleService].
     * Hint:
     * - The sign method must take a [FilteredTransaction] argument and return the oracle's signature
     *   as a [TransactionSignature].
     * - The sign method must verify that the rate provided in the [Exchange] Command is valid.
     *
     * TODO: Hints
     *
     */
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
                ExchangeRateContract.Exchange("USD", rate /* Invalid rate */),
                listOf(c.info.legalIdentities.get(0).owningKey, a.info.legalIdentities.get(0).owningKey))
        builder.addOutputState(output, IOUContract.IOU_CONTRACT_ID)

        val ptx = a.services.signInitialTransaction(builder)

        val ftx = ptx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> c.info.legalIdentities.get(0).owningKey in it.signers
                        && it.value is ExchangeRateContract.Exchange
                else -> false
            }
        })

        val oracleSig = oracle.sign(ftx)
        assert(oracleSig.isValid(ftx.id))
    }


    /**
     * Task 3
     * TODO: Implement the [ExchangeRateOracleFlow].
     * Hint:
     *  -
     */
    @Test
    fun oracleServiceQueryHandler() {
        val oracle = ExchangeRateOracleService(services = c.services)
        val notary = a.services.networkMapCache.notaryIdentities.get(0)
        val builder = TransactionBuilder(notary = notary)

        // Create IOUTokenState
        val iou = IOUTokenState(
                Amount(5, IOUToken("CUSTOM_TOKEN", 2)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))



        // Query for IOUTokenState StateRef

        // Start ExchangeRate flow

        // Check that output state is correct
        // Check that input state is ocrrect
        // Check that oracle signature is present

    }







/*    *//**
     * Task 3
     * TODO: Complete the [call] function of the [QueryHandler] flow.
     * Hint:
     *  - The [QueryHandler] flow is the Responding flow to the [QueryExchangeRate] flow. We will use the
     *  Oracle service we created in the previous exercises.
     *  - First, we need to receive our query parameter form the FlowSession. This can be done using the [receive]
     *  method from our [FlowSession]. Receive<>() is parameterized by the expected data type - in this case a
     *  [String] for our currency code.
     *  -
     *//*
    @Test
    fun oracleServiceQueryHandler() {

    }*/


}