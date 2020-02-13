package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.template.states.IOUState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class IOUSettleFlow(val stateToSettle: StateAndRef<IOUState>,
                    val settlementAccount: String,
                    val settlementCurrency: String= "USD"
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        /*
         Novate IOU
         */
        var oracleSignedTx = subFlow(IOUNovateFlow(
                stateToSettle, settlementCurrency
        ))
        var sessions = createCounterpartySessions(oracleSignedTx)
        oracleSignedTx = subFlow(CollectSignaturesFlow(oracleSignedTx, sessions))
        oracleSignedTx = subFlow(FinalityFlow(oracleSignedTx, sessions))
        val novatedIOU = oracleSignedTx.tx.outputStates[0] as IOUState

        /*
         Make offledger payment
         */
        val transactionId = subFlow(OffLedgerPaymentFlow(
                settlementAccount, novatedIOU.amount.toDecimal().toDouble()
        ))

        /*
        Settle
         */
        val novatedStateRef = vaultQuery(novatedIOU.linearId)
        val settlerSignedTx = subFlow<SignedTransaction>(IOUVerifySettlementFlow(
                novatedStateRef, settlementAccount, transactionId
        ))

        // Collect counter-party signature and finalize
        sessions = createCounterpartySessions(settlerSignedTx)
        val stx = subFlow(CollectSignaturesFlow(settlerSignedTx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }

    private fun vaultQuery(linearId: UniqueIdentifier): StateAndRef<IOUState> {
        return serviceHub.vaultService.queryBy(IOUState::class.java,
                QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(linearId), Vault.StateStatus.UNCONSUMED, null))
                .states[0]
    }

    private fun createCounterpartySessions(stx: SignedTransaction): Set<FlowSession> {
        return (stx.tx.outputStates[0].participants - ourIdentity).map { initiateFlow(it as Party) }.toSet()
    }
}

@InitiatedBy(IOUSettleFlow::class)
class IOUSettleFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}
