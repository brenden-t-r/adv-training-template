package com.template.webserver

import com.template.flows.IOUIssueFlow
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals


class ApiExercises {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `vault query`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        val iou = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)

        partyBHandle.rpc.startFlow(::IOUIssueFlow, iou).returnValue.getOrThrow()

        val result = Controller(partyAHandle.rpc).getIOUs()!!
        assertEquals(1, result.size)
        assertEquals(50, result.get(0).state.data.amount.quantity)
    }

    @Test
    fun `vault query linear id`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        val iou = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
        val iou2 = IOUState(Amount(51, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)

        partyBHandle.rpc.startFlow(::IOUIssueFlow, iou).returnValue.getOrThrow()

        val result = Controller(partyAHandle.rpc).getIousWithLinearId(iou.linearId.toString())!!
        assertEquals(1, result.size)
        assertEquals(50, result.get(0).state.data.amount.quantity)
    }

    @Test
    fun `vault query custom schema`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        partyBHandle.rpc.startFlow(::IOUIssueFlow,
                IOUState(Amount(49, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
        ).returnValue.getOrThrow()

        partyBHandle.rpc.startFlow(::IOUIssueFlow,
                IOUState(Amount(52, IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
        ).returnValue.getOrThrow()

        val result = Controller(partyAHandle.rpc).getIOUsWithAmountGreaterThan(50)!!
        assertEquals(1, result.size)
        assertEquals(52, result.get(0).state.data.amount.quantity)
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true, cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")))
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()
}