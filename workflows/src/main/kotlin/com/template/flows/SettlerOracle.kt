package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap


@InitiatingFlow
class SignOffLedgerPayment(val oracle: Party, val stx: SignedTransaction) : FlowLogic<TransactionSignature>() {
    @Suspendable
    override fun call(): TransactionSignature {
        val session = initiateFlow(oracle)
        return session.sendAndReceive<TransactionSignature>(stx).unwrap { it }
    }
}

@InitiatedBy(SignOffLedgerPayment::class)
class SignOffLedgerPaymentHandler(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val request = session.receive<SignedTransaction>().unwrap { it }
        val response = serviceHub.cordaService(SettlerOracleService::class.java).sign(request)
        session.send(response)
    }
}
