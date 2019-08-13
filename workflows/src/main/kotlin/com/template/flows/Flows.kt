package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.template.states.ExampleEvolvableToken
import net.corda.core.contracts.LinearPointer
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker

import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.UniqueIdentifier
import java.util.*
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
       //  Initiator flow logic goes here.
//        val issuer = ledgerServices.identityService.getAllIdentities().first().party
//        val recipient = ledgerServices.identityService.getAllIdentities().first().party
////        val token = IOUToken("CUSTOMTOKEN", 2);
//////        val issuedFixedToken = IssuedTokenType(issuer, token);
////
////
//        val token = ExampleEvolvableToken(listOf(), 2, "test");
//        val linearPointer = LinearPointer(token.linearId, ExampleEvolvableToken::class.java);
//        val tokenPointer = TokenPointer(linearPointer, token.fractionDigits);
//        val issuedToken = IssuedTokenType(issuer, tokenPointer)
//
//        val nonFungibleToken = NonFungibleToken(
//                issuedToken, recipient,
//                UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString()),
//                tokenPointer.getAttachmentIdForGenericParam()
//        );
//        subFlow(IssueTokens(listOf(nonFungibleToken)))
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
