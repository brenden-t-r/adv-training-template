package com.template.flows

import com.r3.corda.finance.obligation.contracts.states.Obligation
import com.r3.corda.finance.obligation.oracle.flows.VerifySettlement
import com.r3.corda.finance.ripple.types.XrpPayment
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class BankApiOracleService(val services: AppServiceHub) : SingletonSerializeAsToken() {
    // API methods here

    private fun checkObligeeReceivedPayment(
//            payment: BankApiPayment<TokenType>,
            obligation: Obligation<TokenType>
    ): Boolean {
        return false
    }

    fun hasPaymentSettled(
            xrpPayment: XrpPayment<TokenType>,
            obligation: Obligation<TokenType>
    ): VerifySettlement.VerifyResult {
        return VerifySettlement.VerifyResult.REJECTED
    }
}

//data class XrpPayment<T : TokenType>(
//        override val paymentReference: PaymentReference,
//        /** It is expected that the payment reaches the beneficiary by this ledger number. */
//        val lastLedgerSequence: Long,
//        override val amount: Amount<T>,
//        override var status: PaymentStatus = PaymentStatus.SENT
//) : Payment<T> {
//    override fun toString(): String {
//        return "Amount: $amount, Ripple tx hash: $paymentReference, Status: $status"
//    }
//}

//data class XrpSettlement(
//        override val accountToPay: String,
//        override val settlementOracle: Party,
//        override val paymentFlow: Class<MakeXrpPayment<*>> = MakeXrpPayment::class.java
//) : OffLedgerPayment<MakeXrpPayment<*>> {
//    override fun toString(): String {
//        return "Pay XRP address $accountToPay and use $settlementOracle as settlement Oracle."
//    }
//}

//class MakeXrpPayment<T : TokenType>(
//        amount: Amount<T>,
//        obligationStateAndRef: StateAndRef<Obligation<*>>,
//        settlementMethod: OffLedgerPayment<*>,
//        progressTracker: ProgressTracker
//) : MakeOffLedgerPayment<T>(amount, obligationStateAndRef, settlementMethod, progressTracker) {
//
    //@Suspendable
    //override fun setup() {

    //}

    //@Suspendable
    //override fun checkBalance(requiredAmount: Amount<*>) {

    //}

    //@Suspendable
    //override fun makePayment(obligation: Obligation<*>, amount: Amount<T>): XrpPayment<T> {

    //}


//}