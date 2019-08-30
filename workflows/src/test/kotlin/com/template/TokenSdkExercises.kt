package com.template

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.flows.DeliveryVersusPaymentTokenFlow
import com.template.flows.IOUTokenIssueFlow
import com.template.states.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import kotlin.test.assertEquals


class TokenSdkExercises {
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

    // TODO: Create fixed token
    //@Test
    fun testCreateFixedToken() {
        //val token = ExampleFixedToken("test")
        assert(TokenType::class.java.isAssignableFrom(ExampleFixedToken::class.java))
    }

    // TODO: Create evolvable token
    //@Test
    fun testCreateEvolvableToken() {
        assert(EvolvableTokenType::class.java.isAssignableFrom(ExampleEvolvableToken::class.java))
    }

    // TODO: Create non fungible fixed token
    //@Test
    fun testCreateNonFungibleFixedToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createNonFungibleFixedToken(issuer, holder)
        assert(NonFungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleFixedToken::class.java, result.tokenType.tokenClass)
    }

    // TODO: Create non fungible evolvable token
    //@Test
    fun testCreateNonFungibleEvolvableToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createNonFungibleEvolvableToken(issuer, holder)
        assert(NonFungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleEvolvableToken::class.java, result.tokenType.tokenClass)
    }

    // TODO: Create fungible fixed token
    //@Test
    fun testCreateFungibleFixedToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createFungibleFixedToken(issuer, holder, 1000)
        assert(FungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleFixedToken::class.java, result.tokenType.tokenClass)
        assertEquals(1000, result.amount.quantity)
    }

    // TODO: Create fungible evolvable token
    //@Test
    fun testCreateFungibleEvolvableToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createFungibleEvolvableToken(issuer, holder, 1000)
        assert(FungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleEvolvableToken::class.java, result.tokenType.tokenClass)
        assertEquals(1000, result.amount.quantity)
    }

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
     * TODO: Implement IOUTokenIssueFlow
     */
    //@Test
    fun implementIOUTokenIssueFlow() {
        val future = b.startFlow(IOUTokenIssueFlow(25))
        network.runNetwork()
        val stx = future.getOrThrow()

        assertEquals(stx.tx.outputStates.size, 1)
        assertEquals((stx.tx.outputStates.get(0) as FungibleToken).amount.quantity, 25)
    }

    @Test
    fun implementDeliveryVersusPaymentFlow() {
        val partyA = a.info.legalIdentities.get(0);
        val partyB = b.info.legalIdentities.get(0)
        val fungibleToken = createFungibleFixedToken(partyA, partyA, 1000)!!
        val nonFungibleToken = createNonFungibleFixedToken(partyB, partyB)!!

        val fungibleFuture = a.startFlow(IssueTokensFlow(fungibleToken))
        network.runNetwork()
        val stx = fungibleFuture.getOrThrow()
        assertEquals(1, stx.tx.outputStates.size)

        val nonFungibleFuture = b.startFlow(IssueTokens(listOf(nonFungibleToken), listOf(partyA)))
        network.runNetwork()
        val stx2 = nonFungibleFuture.getOrThrow()
        assertEquals(1, stx2.tx.outputStates.size)

        val states = a.services.vaultService.queryBy(ContractState::class.java).states

        val dvpFuture = a.startFlow(DeliveryVersusPaymentTokenFlow(
                ExampleFixedToken("CUSTOMTOKEN", 2),
                ExampleFixedToken("CUSTOMTOKEN", 2),
                partyB
        ))
        network.runNetwork()
        val stx3 = dvpFuture.getOrThrow()
        assertEquals(2, stx3.tx.outputStates.size)
        assertEquals(1, stx3.tx.toLedgerTransaction(a.services).outputsOfType(FungibleToken::class.java).size)
        assertEquals(1, stx3.tx.toLedgerTransaction(a.services).outputsOfType(NonFungibleToken::class.java).size)
        var f = stx3.tx.toLedgerTransaction(a.services).outputsOfType(FungibleToken::class.java).get(0)
        var nf = stx3.tx.toLedgerTransaction(a.services).outputsOfType(NonFungibleToken::class.java).get(0)
        assertEquals(partyB, f.holder)
        assertEquals(partyA, nf.holder)
    }
}