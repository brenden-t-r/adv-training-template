package com.template

import com.r3.corda.finance.obligation.contracts.states.Obligation
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.flows.TokenIOUIssueFlow
import com.template.states.IOUState
import com.template.states.IOUToken
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

class IOUSettlerTests {
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
    @Test
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
     * Task 2
     * TODO: Update IOUState to subclass the Obligation state from the Settler CorDapp in order
     * to allow integration of Settler with our IOU applicaton.
     * Hint:
     *  -
     */
    @Test
    fun isLinearState() {
        assert(Obligation::class.java.isAssignableFrom(IOUState::class.java))
    }
}