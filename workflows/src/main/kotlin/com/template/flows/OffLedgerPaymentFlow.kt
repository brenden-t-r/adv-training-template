package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

@InitiatingFlow
@StartableByRPC
class OffLedgerPaymentFlow(val recipientAccountNumber: String, val paymentAmount: Double) : FlowLogic<String>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {
        return "";
    }
}
