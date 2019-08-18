package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.obligation.contracts.states.Obligation
import com.r3.corda.finance.obligation.contracts.types.OffLedgerPayment
import com.r3.corda.finance.obligation.contracts.types.Payment
import com.r3.corda.finance.obligation.contracts.types.PaymentReference
import com.r3.corda.finance.obligation.contracts.types.PaymentStatus
import com.r3.corda.finance.obligation.oracle.flows.VerifySettlement
import com.r3.corda.finance.obligation.workflows.flows.MakeOffLedgerPayment
import com.r3.corda.finance.ripple.types.XrpPayment
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.ProgressTracker

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

data class BankApiPayment<T : TokenType>(
        override val paymentReference: PaymentReference,
        override val amount: Amount<T>,
        override var status: PaymentStatus = PaymentStatus.SENT
) : Payment<T> {
    override fun toString(): String {
        return "Amount: $amount, Ripple tx hash: $paymentReference, Status: $status"
    }
}

data class BankApiSettlement(
        override val accountToPay: String,
        override val settlementOracle: Party,
        override val paymentFlow: Class<MakeBankApiPayment<*>> = MakeBankApiPayment::class.java
) : OffLedgerPayment<MakeBankApiPayment<*>> {
    override fun toString(): String {
        return "Pay XRP address $accountToPay and use $settlementOracle as settlement ExchangeRateOracleService."
    }
}

class MakeBankApiPayment<T : TokenType>(
        amount: Amount<T>,
        obligationStateAndRef: StateAndRef<Obligation<*>>,
        settlementMethod: OffLedgerPayment<*>,
        progressTracker: ProgressTracker
) : MakeOffLedgerPayment<T>(amount, obligationStateAndRef, settlementMethod, progressTracker) {

    @Suspendable
    override fun setup() {

    }

    @Suspendable
    override fun checkBalance(requiredAmount: Amount<*>) {

    }

    @Suspendable
    override fun makePayment(obligation: Obligation<*>, amount: Amount<T>): BankApiPayment<T> {
        return BankApiPayment("paymentReferenceHere", amount, PaymentStatus.FAILED)
    }


}