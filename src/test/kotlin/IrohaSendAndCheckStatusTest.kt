import iroha.protocol.BlockOuterClass
import iroha.protocol.Primitive.RolePermission
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionStatusObserver
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.security.KeyPair

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaSendAndCheckStatusTest {

    private val crypto = Ed25519Sha3()

    private val peerKeypair = crypto.generateKeypair()
    private val userRolename = "user"
    private val userDomain = "user"
    private val userAlice = "alice"
    private val userAliceKeypair = crypto.generateKeypair()
    private val userBob = "bob"
    private val userBobKeypair = crypto.generateKeypair()
    private val assetName = "cap"

    private val iroha = IrohaContainer().withPeerConfig(getPeerConfig())

    @BeforeAll
    fun beforeAll() {
        iroha.start()
    }

    @AfterAll
    fun afterAll() {
        iroha.stop()
    }

    /**
     * Create Iroha genesis block
     */
    private fun getGenesisBlock(): BlockOuterClass.Block {
        return GenesisBlockBuilder()
            // first transaction
            .addTransaction(
                // transactions in genesis block can have no creator
                Transaction.builder(null)
                    // by default peer is listening on port 10001
                    .addPeer("0.0.0.0:10001", peerKeypair.public)
                    // create default "user" role
                    .createRole(
                        userRolename,
                        listOf(
                            RolePermission.can_transfer,
                            RolePermission.can_get_my_acc_ast,
                            RolePermission.can_get_my_txs,
                            RolePermission.can_receive
                        )
                    )
                    .createDomain(userDomain, userRolename)
                    // create user John
                    .createAccount(userAlice, userDomain, userAliceKeypair.public)
                    // create user B
                    .createAccount(userBob, userDomain, userBobKeypair.public)
                    .createAsset(assetName, userDomain, 2)
                    .build() // returns ipj model Transaction
                    .build() // returns unsigned protobuf Transaction
            )
            .addTransaction(
                Transaction.builder("$userAlice@$userDomain")
                    .addAssetQuantity("$assetName#$userDomain", BigDecimal.TEN)
                    .build()
                    .build()
            )
            .build()
    }

    fun getPeerConfig(): PeerConfig {
        val config = PeerConfig.builder()
            .genesisBlock(getGenesisBlock())
            .build()

        // don't forget to add peer keypair to config
        config.withPeerKeyPair(peerKeypair)

        return config
    }

    fun getBalance(api: IrohaAPI, userId: String, assetId: String, keyPair: KeyPair): Int {
        // build protobuf query, sign it
        val q = Query.builder(userId, 1)
            .getAccountAssets(userId)
            .buildSigned(keyPair)

        // execute query, get response
        val res = api.query(q)

        // get list of assets from our response
        val assets = res.accountAssetsResponse.accountAssetsList

        // find usd asset
        val assetUsdOptional = assets
            .stream()
            .filter { a -> a.assetId.equals(assetId) }
            .findFirst()

        // numbers are small, so we use int here for simplicity
        return assetUsdOptional
            .map { a -> Integer.parseInt(a.balance) }
            .orElse(0)
    }

    @Test
    fun runTest() {
        val irohaApi = iroha.api

        val aliceClientId = "$userAlice@$userDomain"
        val bobClientId = "$userBob@$userDomain"
        val assetId = "$assetName#$userDomain"

        val tx = Transaction.builder(aliceClientId)
            .transferAsset(aliceClientId, bobClientId, assetId, "", "1")
            .sign(userAliceKeypair)
            .build()

        // create transaction observer
        // here you can specify any kind of handlers on transaction statuses
        val observer = TransactionStatusObserver.builder()
            .onError { println("On error called: $it") }
            .onTransactionFailed { println("Transaction ${it.txHash} has been failed with message: ${it.errOrCmdName}") }
            .onTransactionCommitted { println("Transaction ${it.txHash} has been commited") }
            .onComplete { println("Completed") }
            .build()

        irohaApi.transaction(tx)
            .blockingSubscribe(observer)

        println(getBalance(irohaApi, aliceClientId, assetId, userAliceKeypair))
        println(getBalance(irohaApi, bobClientId, assetId, userBobKeypair))
    }


}
