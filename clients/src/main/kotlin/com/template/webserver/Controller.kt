package com.template.webserver

import com.template.flows.IOUIssueFlow
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
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

    @GetMapping(value = "/getIOUs")
    private fun getIOUs(): List<StateAndRef<IOUState>> {
        return proxy.vaultQuery(IOUState::class.java).states
    }

}