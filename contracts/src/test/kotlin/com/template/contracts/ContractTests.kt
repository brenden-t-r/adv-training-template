package com.template.contracts

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.template.states.ExampleEvolvableToken
import com.template.states.IOUToken
import net.corda.core.contracts.LinearPointer
import net.corda.testing.node.MockServices
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()

    @Test
    fun `dummy test`() {
//        val issuer = ledgerServices.identityService.getAllIdentities().first().party
//        val recipient = ledgerServices.identityService.getAllIdentities().first().party
////        val token = IOUToken("CUSTOMTOKEN", 2);
////        val issuedFixedToken = IssuedTokenType(issuer, token);
//
//
//        val token = ExampleEvolvableToken(listOf(), 2, "test");
//        val linearPointer = LinearPointer(token.linearId, ExampleEvolvableToken::class.java);
//        val tokenPointer = TokenPointer(linearPointer, token.fractionDigits);
//        val issuedToken = IssuedTokenType(issuer, tokenPointer)




    }
}