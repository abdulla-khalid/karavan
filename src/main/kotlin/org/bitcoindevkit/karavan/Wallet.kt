package org.bitcoindevkit.karavan

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bitcoindevkit.*
import org.bitcoindevkit.Wallet

class KaravanWallet(descriptorIn: String, networkIn: String) {

    private var descriptor: String = descriptorIn
    private var network: String = networkIn

    private var isInitialized : Boolean = false

    private lateinit var wallet: Wallet
    private val networkURL: String = "ssl://electrum.blockstream.info:60002"
    private lateinit var networkType: Network

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

    // Return 0 if successful, return -1 if not
    fun initializeWallet(): Int {

        // Check if valid network
        if (network.equals("TESTNET", ignoreCase = true))
            networkType = Network.TESTNET
        else
            return -1

        if (this.descriptor.isEmpty())
            return -1

        val db = DatabaseConfig.Memory("")

        val client =
            BlockchainConfig.Electrum(
                ElectrumConfig(
                    url = networkURL,
                    socks5 = null,
                    retry = 5u,
                    timeout = null,
                    stopGap = 10u
                )
            )

        this.wallet =
            Wallet(
                descriptor = this.descriptor,
                changeDescriptor = null,
                network = this.networkType,
                databaseConfig = db,
                blockchainConfig = client
            )

        this.isInitialized = true
        return 0
    }

    fun getBalance() : String {

        if (!this.isInitialized)
            return "Wallet not Initialized!"

        // sync balance of descriptor
        this.wallet.sync(progressUpdate = WalletService.NullProgress, maxAddressParam = null)

        // get the balance
        val balance : ULong = wallet.getBalance()

        // put balance into JSON and return it
        val mapper = ObjectMapper()
        val balanceJSON: ObjectNode = mapper.createObjectNode()
        balanceJSON.put("balance", balance.toString())
        val balanceJSONString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(balanceJSON)

        return balanceJSONString
    }

    fun getNewAddress() : String {

        if (!this.isInitialized)
            return "Wallet not Initialized!"

        // sync balance of descriptor
        wallet.sync(progressUpdate = WalletService.NullProgress, maxAddressParam = null)
        return wallet.getNewAddress()
    }

    fun getTransactions(): String {

        if (!this.isInitialized)
            return "Wallet not Initialized!"

        // get transactions
        val transactionList = wallet.getTransactions()

        //transactionList.sortedWith(transactionCompByHeight)
        println(transactionList)

        // map transactionList of Transaction objects into JSON using Jackson
        val mapper = jacksonObjectMapper()
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transactionList)
    }

}


