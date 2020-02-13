package com.template.flows

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
internal class OffLedgerPaymentRailService(val serviceHub: ServiceHub) : SingletonSerializeAsToken() {

    val myKey = serviceHub.myInfo.legalIdentities.first().owningKey

    fun verifyTransaction(transactionId: String, paymentAmount: Double, recipientAccountNumber: String): Boolean {
        return true
    }

    fun makePayment(recipientAccountNumber: String, amount: Double): String {
        return "TEST_TRANSACTION_ID_1234"
    }

}
