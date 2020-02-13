package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.contracts.IOUContract
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*
import java.util.stream.Collectors


@InitiatingFlow
@StartableByRPC
class IOUVerifySettlementFlow(
        val stateToSettle: StateAndRef<IOUState>,
        val settlementAccount: String,
        val transactionId: String
): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Get notary identity
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Get oracle identity
        val settlerOracle = Objects.requireNonNull(serviceHub.networkMapCache.getNodeByLegalName(
                CordaX500Name("SettlerOracleService", "New York", "US")
        ))!!.legalIdentities.first()

        val state = stateToSettle.state.data

        val builder = TransactionBuilder(notary)
        builder.addInputState(stateToSettle)
        builder.addOutputState(state.withSettled(), IOUContract.IOU_CONTRACT_ID)
        val requiredSigners = ImmutableList.of(
                state.lender, state.borrower, settlerOracle)
        builder.addCommand(
                IOUContract.Commands.Settle(
                        transactionId, state.amount.toDecimal().toDouble(),
                        state.amount.token.tokenIdentifier, settlementAccount),
                requiredSigners.map{ it.owningKey })
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Get Settlement oracle signature and add to SignedTransaction
        val oracleSignature = subFlow(SignOffLedgerPayment(settlerOracle, ptx))

        return ptx.withAdditionalSignature(oracleSignature)
    }
}

@InitiatedBy(IOUVerifySettlementFlow::class)
class IOUVerifySettlementResponderFlow(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}
