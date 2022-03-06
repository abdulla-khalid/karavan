package org.bitcoindevkit.karavan

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.*
import org.bitcoindevkit.*
import org.bitcoindevkit.Wallet


@Service
class WalletService {


    // Create null object of type BdkProgress
    // update() function is changed to do nothing
    object NullProgress : BdkProgress {
        override fun update(progress: Float, message: String?) {}
    }

    // @TODO Hold off on making comparator until we get the Transaction.Confirmed companion object
    // Comparator needs to be of type Transcation not Transaction.Confirmed
    private val transactionCompByHeight =  Comparator<Transaction.Confirmed> { a, b ->
        when {
            (a.confirmation.height == b.confirmation.height) -> 0
            (a.confirmation.height > b.confirmation.height) -> 1
            else -> -1
        }
    }

    // Connect to Electrum network, sync wallet, and return balance as JSON
    fun getBalance(descriptor: String, networkIn: String): String{

        val db = DatabaseConfig.Memory("")
        val network : Network
        val balance : ULong

        // Check if valid network
        if (networkIn.equals("TESTNET", ignoreCase = true))
            network = Network.TESTNET
        else
            return "Invalid Network: $networkIn!"

        // Connecting to Electrum network
        val client =
            BlockchainConfig.Electrum(
                ElectrumConfig("ssl://electrum.blockstream.info:60002", null, 5u, null, 10u)
            )
        val wallet = Wallet(descriptor, null, network, db, client)

        // sync balance of descriptor
        wallet.sync(progressUpdate = NullProgress, maxAddressParam = null)

        // get the balance
        balance = wallet.getBalance()

        // put balance into JSON and return it
        val mapper = ObjectMapper()
        val balanceJSON: ObjectNode = mapper.createObjectNode()
        balanceJSON.put("balance", balance.toString())
        val balanceJSONString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(balanceJSON)

        return balanceJSONString
    }

    // Connect to Electrum network, sync wallet, and return new address in string.
    fun getNewAddress(descriptor: String, networkIn: String): String{

        val db = DatabaseConfig.Memory("")
        val network : Network
        val newAddress : String

        // Check if valid network
        if (networkIn.equals("TESTNET", ignoreCase = true))
            network = Network.TESTNET
        else
            return "Invalid Network: $networkIn!"

        // Connecting to Electrum network
        val client =
            BlockchainConfig.Electrum(
                ElectrumConfig("ssl://electrum.blockstream.info:60002", null, 5u, null, 10u)
            )
        val wallet = Wallet(descriptor, null, network, db, client)

        // sync balance of descriptor
        wallet.sync(progressUpdate = NullProgress, maxAddressParam = null)

        // get a new address
        newAddress = wallet.getNewAddress()

        return newAddress
    }

    // Return list of transactions in JSON format
    fun getTransactions(descriptor: String, networkIn: String): String{

        val db = DatabaseConfig.Memory("")
        val network : Network
        val balance : ULong

        // Check if valid network
        if (networkIn.equals("TESTNET", ignoreCase = true))
            network = Network.TESTNET
        else
            return "Invalid Network: $networkIn!"

        // Connecting to Electrum network
        val client =
            BlockchainConfig.Electrum(
                ElectrumConfig("ssl://electrum.blockstream.info:60002", null, 5u, null, 10u)
            )
        val wallet = Wallet(descriptor, null, network, db, client)

        // sync balance of descriptor
        wallet.sync(progressUpdate = NullProgress, maxAddressParam = null)



        // get transactions
        val transactionList = wallet.getTransactions()

        //transactionList.sortedWith(transactionCompByHeight)
        println(transactionList)

        // map transactionList of Transaction objects into JSON using Jackson
        val mapper = jacksonObjectMapper()
        var transactionsInJSON : String = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transactionList)

        return transactionsInJSON
    }
}