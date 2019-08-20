package com.template.contracts

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sun.deploy.util.ReflectionUtil.isSubclassOf
import com.template.states.IOUState
import com.template.states.IOUToken
import jdk.nashorn.internal.parser.Token
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.finance.*
import org.glassfish.jersey.internal.util.ReflectionHelper.isSubClassOf
import org.junit.Test
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Practical exercise instructions.
 * Uncomment the first unit test then run the unit test using the green arrow
 * to the left of the [StateTests] class or the test method.
 * Running the unit tests from [StateTests] runs all of the unit tests defined in the class.
 * The test should fail because you need to make some changes to the IOUState to make the test pass. Read the TODO
 * under each task number for a description and a hint of what you need to do.
 * Once you have the unit test passing, uncomment the next test.
 * Continue until all the unit tests pass.
 * Hint: CMD / Ctrl + click on the brown type names in square brackets for that type's definition in the codebase.
 */
class StateTests {

    @Test
    fun iouTokenImplementsTokenTypeInterface() {
        assertTrue(TokenType::class.java == IOUToken::class.java.superclass)
    }

    /**
     * Task 1.
     * TODO: ? Update our IOUState to use [IOUToken] rather than [Currency].
     * Hint: [Amount] can be used to specify an amount of IOUToken.
     */
//    @Test
//    fun hasIOUAmountFieldOfCorrectType() {
//        // Does the amount field exist?
//        val field = IOUState::class.java.getDeclaredField("amount")
//
//        // Is the amount field of the correct type?
//        assertEquals(field.type, Amount::class.java)
//
//        // Does the amount field have the correct paramerized type?
//        val signature = (field.genericType as ParameterizedTypeImpl).actualTypeArguments[0]
//        assertEquals(signature, IOUToken::class.java)
//    }

    // TODO: queryable state tests
}