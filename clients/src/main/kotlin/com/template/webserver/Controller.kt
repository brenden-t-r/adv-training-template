package com.template.webserver

//import com.template.flows.IOUIssueFlow
//import com.template.states.IOUCustomSchema
//import com.template.states.IOUState
//import com.template.states.IOUToken
import com.template.states.IOUCustomSchema
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.Builder
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(proxy: CordaRPCOps) {

    init {
        this.exerciseSetup()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private var proxy = proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    public fun templateendpoint(): String {
        return proxy.nodeInfo().legalIdentities[0].name.organisation
        //return "Define an endpoint here."
    }

    @GetMapping(value = "/exerciseSetup")
    public fun exerciseSetup(): Unit {
       // proxy.startTrackedFlow(::IOUIssueFlow,
//                IOUState(
//                Amount(50, IOUToken("CUSTOMTOKEN", 2)),
//                proxy.nodeInfo().legalIdentities[0], proxy.nodeInfo().legalIdentities[1])
//        ).returnValue.get()
//
//        proxy.startTrackedFlow(::IOUIssueFlow,
//                IOUState(
//                Amount(80, IOUToken("CUSTOMTOKEN", 2)),
//                proxy.nodeInfo().legalIdentities[0], proxy.nodeInfo().legalIdentities[1])
//        ).returnValue.get()

        print("\nOK")
    }

   @GetMapping(value = "/getIOUs")
    public fun getIOUs(): List<StateAndRef<IOUState>>? {
        //return null;
        return proxy.vaultQuery(IOUState::class.java).states
    }


    @GetMapping(value = "/getIOUs/{amount}")
    public fun getIOUsWithAmountGreaterThan(@PathVariable amount: Long): List<StateAndRef<IOUState>>? {
        return builder {
            val criteria = QueryCriteria.VaultCustomQueryCriteria(
                    IOUCustomSchema.PersistentIOU::amount.greaterThan(amount)
            )
            proxy.vaultQueryBy<IOUState>(criteria)
        }.states
    }

}