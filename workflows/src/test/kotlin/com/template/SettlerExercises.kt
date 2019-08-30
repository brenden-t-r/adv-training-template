package com.template

import com.r3.corda.finance.obligation.contracts.commands.ObligationCommands
import com.r3.corda.finance.obligation.contracts.flows.AbstractSendToSettlementOracle
import com.r3.corda.finance.obligation.contracts.states.Obligation
import com.r3.corda.finance.obligation.contracts.types.PaymentStatus
import com.r3.corda.finance.obligation.oracle.flows.VerifySettlement
import com.r3.corda.finance.obligation.workflows.flows.NovateObligation
import com.r3.corda.finance.obligation.workflows.flows.OffLedgerSettleObligation
import com.r3.corda.finance.obligation.workflows.flows.SendToSettlementOracle
import com.r3.corda.finance.obligation.workflows.flows.UpdateSettlementMethod
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
            TestCordapp.findCordapp("com.r3.corda.finance.obligation.workflows.flows"),
            TestCordapp.findCordapp("com.r3.corda.finance.obligation.oracle.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode(CordaX500Name("ExchangeRateOracleService", "New York", "US"))
    private val d = network.createNode(CordaX500Name("SettlerOracleService", "New York", "US"))


    init {
        listOf(a, b, c, d).forEach {
            it.registerInitiatedFlow(QueryHandler::class.java)
            it.registerInitiatedFlow(SignHandler::class.java)
            it.registerInitiatedFlow(VerifySettlement::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()



    /**
     * Task 1
     * TODO: Update IOUState to subclass the Obligation state from the Settler CorDapp in order
     * to allow integration of Settler with our IOU applicaton.
     * Hint:
     *  -
     */
    //@Test
    fun isLinearState() {
        assert(Obligation::class.java.isAssignableFrom(IOUState::class.java))
    }

    /**
     * Task 2
     * TODO: Implement the CordaSettlerNovateIOUFlow
     * Steps:
     * 1) Get exchange rate from our ExchangeRateOracle
     * 2) Novate obligation with relevant currency
     */
    @Test
    fun testCordaSettlerNovateIOUFlow() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = b.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val resultFromNovate = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        //assertEquals(resultFromNovate.toLedgerTransaction(a.services).outputStates.size, 1)
        assertEquals(resultFromNovate.faceAmount, Amount(7500, TokenType("USD", 2)));
    }

    /**
     * Task 3
     * TODO: Implement the CordaSettlerUpdateSettlementMethodFlow
     * Steps:

     */
    //@Test
    fun testCordaSettlerUpdateSettlementMethodFlow() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = a.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val novatedIOU = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        val updateFuture = a.startFlow(CordaSettlerUpdateSettlementMethodFlow(novatedIOU, "ABCD1234"))
        network.runNetwork()
        val resultFromUpdate = updateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        assertEquals(resultFromUpdate.settlementMethod!!::class.java, BankApiSettlement::class.java);
    }

    /**
     * Task 4
     * TODO: Update VerifySettlement flow
     * Steps:
     */
    //@Test
    fun testUpdateVerifySettlementForBankApiSettlement() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 2)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = a.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val novatedIOU = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        val updateFuture = a.startFlow(CordaSettlerUpdateSettlementMethodFlow(novatedIOU, "ABCD1234"))
        network.runNetwork()
        val resultUpdate = updateFuture.getOrThrow().toLedgerTransaction(a.services)
        val resultFromUpdate = resultUpdate.outputStates.get(0) as Obligation<TokenType>
        val stateRef = resultUpdate.outRef<Obligation<TokenType>>(0)

        val tx = b.startFlow(
                MakeBankApiPayment(Amount(75, FiatCurrency.getInstance("USD")),
                        stateRef, resultFromUpdate.settlementMethod as BankApiSettlement))
        network.runNetwork()

        val oracleFuture = b.startFlow(SendToSettlementOracle(resultFromUpdate.linearId))
        network.runNetwork()
        val oracleResult = oracleFuture.getOrThrow()
    }

    /**
     * Task 5
     * TODO: Implement the CordaSettlerBankApiSettlement Flow
     * Steps:
     */
    @Test
    fun testCordaSettlerBankApiSettlement() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = a.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val novatedIOU = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        // Update Settlement Terms
        val updateFuture = a.startFlow(CordaSettlerUpdateSettlementMethodFlow(novatedIOU, "ABCD1234"))
        network.runNetwork()
        val resultUpdate = updateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        // Settle
        val updateSettle = b.startFlow(CordaSettlerBankApiSettlement(resultUpdate))
        network.runNetwork()
        val resultFromSettle = updateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        // no ouputs, 1 input
        // see that original IOU was settled. There should be no unconsumed IOU or Obligations in vault at this point
        b.transaction {
            assertEquals(0, b.services.vaultService.queryBy(IOUState::class.java).states.size)
            assertEquals(1, b.services.vaultService.queryBy(Obligation::class.java).states.size)
            assertEquals(PaymentStatus.SETTLED, b.services.vaultService.queryBy(Obligation::class.java).states.get(0).state.data.payments.get(0).status)
        }

    }
}