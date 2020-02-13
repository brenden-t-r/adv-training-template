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
        val oracleName = CordaX500Name("SettlerOracleService", "New York","US")

        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = null)
        )
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
