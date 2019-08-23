package com.template

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.flows.ExchangeRateOracleService
import com.template.flows.Responder
import com.template.flows.TokenIOUIssueFlow
import com.template.states.IOUState
import com.template.states.IOUToken
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import kotlin.test.assertEquals


class TokenIOUIssueFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b, c).forEach {
            //it.registerInitiatedFlow(TokenIOUIssueFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * Task 1
     * TODO: Convert IOUState to be in terms of an Amount of Token SDK Tokens rather than Currency
     * Hint:
     *  -
     */
    //@Test
    fun hasIOUAmountFieldOfCorrectType() {
        // Does the amount field exist?
        val field = IOUState::class.java.getDeclaredField("amount")

        // Is the amount field of the correct type?
        assertEquals(field.type, Amount::class.java)

        // Does the amount field have the correct paramerized type?
        val signature = (field.genericType as ParameterizedTypeImpl).actualTypeArguments[0]
        assertEquals(signature, TokenType::class.java)
    }

    /**
     * Task 1
     * TODO: Implement TokenIOUIssueFlow
     */
    //@Test
    fun test1() {
        val future = b.startFlow(TokenIOUIssueFlow(25))
        network.runNetwork()
        val stx = future.getOrThrow()

        assertEquals(stx.tx.outputStates.size, 1)
        assertEquals((stx.tx.outputStates.get(0) as FungibleToken).amount.quantity, 25)
    }
}