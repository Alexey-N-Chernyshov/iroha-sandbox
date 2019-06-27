package com.github.alexeynchernyshov.iroha

import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import org.junit.jupiter.api.Test
import org.junit.Assert.assertEquals

class IrohaAccountTest {

    @Test
    fun testClientId() {
        val name = "name"
        val domain = "domain"
        val keypair = Ed25519Sha3().generateKeypair()
        val account = IrohaAccount(name, domain, keypair)

        assertEquals("$name@$domain", account.clientId)
    }
}
