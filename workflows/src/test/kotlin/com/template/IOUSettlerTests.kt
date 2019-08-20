package com.template

import com.r3.corda.finance.obligation.contracts.commands.ObligationCommands
import com.r3.corda.finance.obligation.contracts.flows.AbstractSendToSettlementOracle
import com.r3.corda.finance.obligation.contracts.states.Obligation
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


class IOUSettlerTests {
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

    init {
        listOf(a, b, c).forEach {
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
     * Task 2
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
     * Task 3
     * TODO:
     *  -
     */
    @Test
    fun test1() {
        /*(*
        Create obligation
        Get exchange rate from FX-Oracle
        Novate obligation with relevant currency (ex. XRP)
        Add additional settlement terms
        Initiate payment on the off-ledger rail
        Obtain confirmation from Settler Oracle
        Obtain signatures and settle the obligation
         */

        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Get exchange rate from FX-Oracle
        val exRateFuture = b.startFlow(QueryExchangeRate(c.info.legalIdentities.get(0), "USD"))
        network.runNetwork()
        val resultFromOracle = exRateFuture.getOrThrow()

        // 3. Novate obligation with relevant currency (ex. XRP)
        // TODO: Use NovateObligation flow or just do it ourselves?
        val novateFuture = b.startFlow(NovateObligation.Initiator(
                issuedIou.linearId,
                ObligationCommands.Novate.UpdateFaceAmountToken(
                        IOUToken("CUSTOM_TOKEN", 0),
                        FiatCurrency.getInstance("USD"),
                        c.info.legalIdentities.get(0),
                        resultFromOracle
                )
        ))
        network.runNetwork()
        val resultFromNovate = novateFuture.getOrThrow()
        val novatedIou = resultFromNovate.toLedgerTransaction(b.services).outputStates.get(0) as Obligation<TokenType>

        // 4. Add additional settlement terms
        val updateFuture = a.startFlow(UpdateSettlementMethod.Initiator(
                issuedIou.linearId,
                BankApiSettlement("ABCD1234", c.info.legalIdentities.get(0))
        ))
        network.runNetwork()
        val resultFromUpdate = updateFuture.getOrThrow()

        val finalFuture = b.startFlow(OffLedgerSettleObligation.Initiator(novatedIou.faceAmount, novatedIou.linearId))
        network.runNetwork()
        val stx = finalFuture.getOrThrow()

        assertEquals(stx.outputStates.size, 1);

//        val updatedIou = resultFromUpdate.toLedgerTransaction(b.services).outputStates.get(0) as IOUState
//        val criteria = QueryCriteria.LinearStateQueryCriteria(
//                null, listOf(issuedIou.linearId), Vault.StateStatus.UNCONSUMED, null)
//        val queryResults = a.services.vaultService.queryBy(IOUState::class.java, criteria);
//        val stateAndRef = queryResults.states.get(0) as StateAndRef<IOUState>
//
//        // 5. Initiate payment on the off-ledger rail
//        val paymentFuture = b.startFlow(MakeBankApiPayment(
//                updatedIou.amount,
//                stateAndRef,
//                BankApiSettlement("ABCD1234", c.info.legalIdentities.get(0))
//        ))
//        network.runNetwork()
//        val resultFromPayment = paymentFuture.getOrThrow()
//
//        // 6. Obtain confirmation from Settler Oracle
//        val settlementFuture = b.startFlow(SendToSettlementOracle(issuedIou.linearId))
//        network.runNetwork()
//        val oracleSigned = settlementFuture.getOrThrow()
//
//        // 7. Obtain signatures and settle the obligation
//

    }
}