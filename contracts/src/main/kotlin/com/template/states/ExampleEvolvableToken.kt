package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class ExampleEvolvableToken(
        override val maintainers: List<Party>,
        override val fractionDigits: Int,
        val exampleDataProperty: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType()