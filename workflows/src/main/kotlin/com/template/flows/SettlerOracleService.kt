package com.template.flows

import com.template.contracts.IOUContract
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransactionVerificationException
import net.corda.core.transactions.SignedTransaction
import java.security.PublicKey

@CordaService
class SettlerOracleService(val serviceHub: ServiceHub) : SingletonSerializeAsToken() {

    private val myKey = serviceHub.myInfo.legalIdentities.first().owningKey

    fun query(transactionId: String, amount: Double, recipientAccountNumber: String): Boolean {
        return serviceHub.cordaService(OffLedgerPaymentRailService::class.java).verifyTransaction(
                transactionId, amount, recipientAccountNumber)
    }

    @Throws(FilteredTransactionVerificationException::class)
    fun sign(stx: SignedTransaction): TransactionSignature {
        val command = stx.tx.commands.first()

        if (command.value is IOUContract.Commands.Settle) {
            val settle = command.value as IOUContract.Commands.Settle

            return if (query(settle.transactionId, settle.novatedAmount, settle.settlementAccount)) {
                serviceHub.createSignature(stx, myKey)
            } else {
                throw IllegalArgumentException("Invalid transaction.")
            }
        } else {
            throw IllegalArgumentException("Invalid transaction.")
        }
    }
}
