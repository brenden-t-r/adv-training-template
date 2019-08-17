package com.template.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class ExchangeRateContract : Contract {
    companion object {
        @JvmStatic
        val EXCHANGE_RATE_CONTRACT_ID = "com.template.contracts.ExchangeRateContract"
    }

    class Exchange(val currency: String, val rate: Double) : CommandData

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        requireThat {
            "There are no inputs" using (tx.inputs.isEmpty())
        }
    }
}