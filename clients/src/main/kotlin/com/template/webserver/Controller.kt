package com.template.webserver

import com.template.flows.IOUIssueFlow
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return proxy.nodeInfo().legalIdentities[0].name.organisation
        //return "Define an endpoint here."
    }

    @GetMapping(value = "/issue-iou/{amount}/{party}", produces = arrayOf("text/plain"))
    fun issueIOU(@PathVariable(value = "amount") amount: Int,
                 @PathVariable(value = "party") party: String): String {
        // Get party objects for myself and the counterparty.
        val me = proxy.nodeInfo().legalIdentities.first()
        val lender = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
        // Create a new IOU state using the parameters given.
        try {
            val state = IOUState(Amount(amount.toLong() * 100, Currency.getInstance("USD")), lender, me)
            // Start the IOUIssueFlow. We block and waits for the flow to return.
            val result = proxy.startTrackedFlow(::IOUIssueFlow, state).returnValue.get()
            // Return the response.
            return "Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single()}"
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (e: Exception) {
            return e.message.toString();
        }
    }

}