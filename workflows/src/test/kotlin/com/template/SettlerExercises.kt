package com.template

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.flows.*
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toLtxDjvmInternal
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import kotlin.test.assertEquals
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker


class SettlerExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
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

}
