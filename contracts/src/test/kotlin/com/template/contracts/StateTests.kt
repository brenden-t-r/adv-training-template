package com.template.contracts

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sun.deploy.util.ReflectionUtil.isSubclassOf
import com.template.states.IOUState
import com.template.states.IOUToken
import jdk.nashorn.internal.parser.Token
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.QueryableState
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

    /**
     * TODO: Turn the [IOUState] into a [QueryableState].
     * Hint:
     * - [QueryableState] implements [ContractState] and has two additional function requirements.
     * - Update the IOU State to implement the [QueryableState].
     * -- There will be compilation errors until we finish the remaining steps.
     * - Create custom [MappedSchema] and [PersistentState] subclass implementations.
     * -- We need to create a custom implementation of the [MappedSchema] class.
     * -- Nest within our custom [MappedSchema] class, we will need a custom implementation of a [PersistentState].
     * -- This uses JPA (Java Persistence API) notation to define how the state will be stored in the database.
     * -- Use the @Entity annotation for our [PersistentState] to define it as a database table enumeration.
     * -- Use the @Column annotation on each field within the [PersistentState] to define the table columns.
     * - Implement the [supportedSchemas] and [generateMappedObject] methods from the [QueryableState] interface.
     * -- [generateMappedObject] takes in a MappedSchema instance and returns the corresponding PersistentState.
     * -- In this way, we could potentially have multiple Schema definitions.
     * -- [supportedSchemas] simply returns a list of schemas supported by this QueryableState.
     */
    @Test
    fun implementQueryableStateOnIOU() {
        assert(QueryableState::class.java.isAssignableFrom(IOUState::class.java))
    }
}