package com.template.webserver

import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.contracts.ExchangeRateContract
import com.template.contracts.IOUContract
import com.template.flows.*
import com.template.flows.ExchangeRateOracleFlow
import com.template.states.IOUState
import com.template.states.IOUToken
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.junit.After
import org.junit.Before
import org.junit.Test
import rx.Observable
import java.util.*
import java.util.function.Predicate
import kotlin.test.assertEquals

class ApiExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode(CordaX500Name("ExchangeRateOracleService", "New York", "US"))

    init {
        listOf(a, b, c).forEach {
            it.registerInitiatedFlow(IOUIssueFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun testIouIssueFlow() {
        val iou = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)), a.info.legalIdentities.get(0), b.info.legalIdentities.get(0))

        val future = a.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = future.getOrThrow()

        assertEquals(1, a.services.vaultService.queryBy(IOUState::class.java).states.size)
    }
}