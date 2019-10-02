@file:JvmName("TransactionSerializer")

package com.github.alexeynchernyshov.irohatest

import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils

/**
 * Serialize transaction and print it
 */
fun main(args: Array<String>) {
    val kp = Utils.parseHexKeypair(
        "0000000000000000000000000000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000000000000000000000000000"
    )

    val tx = Transaction.builder("admin@nsd")
        .setAccountDetail(
            "admin@nsd",
            "password",
            "673bf9c6441d5c591423422a86946b08978a821f5137feea237f1e41619a3fe8"
        )
        .addSignatory("admin@nsd", kp.public)
        .build()
        .build()
    val serializedTx = com.google.protobuf.util.JsonFormat.printer().print(tx)

    println(serializedTx)
}
