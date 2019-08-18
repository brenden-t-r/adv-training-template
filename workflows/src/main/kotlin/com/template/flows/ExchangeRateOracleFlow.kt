package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.money.USD
import com.template.contracts.ExchangeRateContract
import com.template.contracts.IOUContract
import com.template.states.IOUState
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.util.*
import java.util.function.Predicate

@InitiatingFlow
@StartableByRPC
class ExchangeRateOracleFlow(val iouTokenState: StateAndRef<IOUTokenState>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    fun createFilteredTransaction(oracle: Party, builder: TransactionBuilder): FilteredTransaction {
        val ptx = serviceHub.signInitialTransaction(builder)
        return ptx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> oracle.owningKey in it.signers //&& it.value is FxRateContract.Create
                else -> false
            }
        })
    }

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Get oracle identity
        val oracleName = CordaX500Name("ExchangeRateOracleService", "New York","US")
        val oracle = serviceHub.networkMapCache.getNodeByLegalName(oracleName)?.legalIdentities?.first()
                ?: throw IllegalArgumentException("Requested oracle $oracleName not found on network.")

        // Create builder
        val builder = TransactionBuilder(notary)

        // Query oracle for value
        val resultFromOracle = subFlow(QueryExchangeRate(oracle, "USD"))

        // Update builder with value
        builder.addCommand(
                ExchangeRateContract.Exchange("USD", resultFromOracle),
                listOf(oracle.owningKey, ourIdentity.owningKey)
        )
        builder.addInputState(iouTokenState)
        builder.addOutputState(IOUState(
                Amount(iouTokenState.state.data.amount.quantity, Currency.getInstance("USD")),
                iouTokenState.state.data.lender,
                iouTokenState.state.data.borrower), IOUContract.IOU_CONTRACT_ID)
        val ptx = serviceHub.signInitialTransaction(builder)

        val filteredTx = createFilteredTransaction(oracle, builder)

        val oracleSignature = subFlow(SignExchangeRate(oracle, filteredTx))
        val stx = ptx.withAdditionalSignature(oracleSignature)

        return stx;
//        return subFlow(FinalityFlow(ptx, listOf()))
    }
}

//@InitiatedBy(ExchangeRateOracleFlow::class)
//class FxOracleFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        // Responder flow logic goes here.
//    }
//}

@InitiatingFlow
class QueryExchangeRate(val oracle: Party, val currencyCode: String) : FlowLogic<Double>() {
    @Suspendable
    override fun call(): Double {
        return initiateFlow(oracle).sendAndReceive<Double>(currencyCode).unwrap { it }
    }
}

@InitiatedBy(QueryExchangeRate::class)
class QueryHandler(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val request = session.receive<String>().unwrap { it }
        val response = serviceHub.cordaService(ExchangeRateOracleService::class.java).query(request)
        session.send(response)
    }
}

@InitiatingFlow
class SignExchangeRate(val oracle: Party, val ftx: FilteredTransaction) : FlowLogic<TransactionSignature>() {
    @Suspendable override fun call(): TransactionSignature {
        val session = initiateFlow(oracle)
        return session.sendAndReceive<TransactionSignature>(ftx).unwrap { it }
    }
}

@InitiatedBy(SignExchangeRate::class)
class SignHandler(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val request = session.receive<FilteredTransaction>().unwrap { it }
        val response = serviceHub.cordaService(ExchangeRateOracleService::class.java).sign(request)
        session.send(response)
    }
}

@CordaService
class ExchangeRateOracleService(val services: ServiceHub) : SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey
    fun query(currencyCode: String): Double {
        // Query external data source and return result
        // In practice this would be an external call to a real service
        if (currencyCode.equals("USD")) {
            return 1.5;
        } else if (currencyCode.equals("GBP")) {
            return 1.8;
        } else throw IllegalArgumentException("Unsupported currency.")
    }
    fun sign(ftx: FilteredTransaction): TransactionSignature {
        ftx.verify() // Check the partial Merkle tree is valid.

        fun isCommandCorrect(elem: Any) = when {
            elem is Command<*> && elem.value is ExchangeRateContract.Exchange -> {
                val cmdData = elem.value as ExchangeRateContract.Exchange
                myKey in elem.signers && query(cmdData.currency) == cmdData.rate
            }
            else -> {
                false
            }
        }

        // Verify the correctness of the command data
        if (ftx.checkWithFun(::isCommandCorrect)) {
            return services.createSignature(ftx, myKey)
        } else throw IllegalArgumentException("Invalid transaction.")
    }
}