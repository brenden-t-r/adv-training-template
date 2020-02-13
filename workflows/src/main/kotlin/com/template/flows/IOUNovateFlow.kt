package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.contracts.IOUContract
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class IOUNovateFlow(
        val stateToSettle: StateAndRef<IOUState>,
        val settlementCurrency: String
): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Get notary identity
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Get oracle identities
        val fxOracle = Objects.requireNonNull(serviceHub.networkMapCache.getNodeByLegalName(
                CordaX500Name("ExchangeRateOracleService", "New York", "US")
        ))!!.legalIdentities[0]

        val state = stateToSettle.state.data

        val fxRate = serviceHub.cordaService(ExchangeRateOracleService::class.java).query(settlementCurrency)
        val novatedAmount = fxRate * state.amount.quantity
        val builder = TransactionBuilder(notary)
        builder.addInputState(stateToSettle)
        builder.addOutputState(state
                .withNewAmount(Amount(novatedAmount.toLong(), FiatCurrency.getInstance(settlementCurrency))),
                IOUContract.IOU_CONTRACT_ID)
        val requiredSigners = ImmutableList.of(
                state.lender, state.borrower, fxOracle)
        builder.addCommand(IOUContract.Commands.Novate(settlementCurrency, fxRate), requiredSigners.map{ it.owningKey })
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Get FxRate oracle signature and add to SignedTransaction
        return subFlow(ExchangeRateOracleFlow(ptx))
    }
}

@InitiatedBy(IOUNovateFlow::class)
class IOUNovateFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}
