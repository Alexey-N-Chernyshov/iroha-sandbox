package com.github.alexeynchernyshov.iroha

import java.security.KeyPair

data class IrohaAccount(
    val accountName: String,
    val domain: String,
    val keyPair: KeyPair
) {
    val clientId: String
        get() = "$accountName@$domain"
}
