package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.IOUContract
import com.template.states.IOUState
import com.template.states.IOUToken
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TokenIOUIssueFlow(val tokenAmount: Int): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val issuedTokenType = IOUToken("CUSTOM_TOKEN", 0) issuedBy ourIdentity
        val fungibleTokens = tokenAmount of issuedTokenType heldBy ourIdentity
        return subFlow(IssueTokens(listOf(fungibleTokens)));

//       ALTERNATIVE SYNTAX
//       val issuedTokenType = IssuedTokenType(ourIdentity, state.amount.token);
//       val fungibleToken =
//                FungibleToken(
//                        Amount(10000, issuedTokenType),
//                        state.lender
//       );
    }
}

///**
// * This is the flow which signs IOU issuances.
// * The signing is handled by the [SignTransactionFlow].
// */
//@InitiatedBy(TokenIOUIssueFlow::class)
//class TokenIOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an IOU transaction" using (output is IOUTokenState)
//            }
//        }
//
//        val txWeJustSignedId = subFlow(signedTransactionFlow)
//
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
//    }
//}