package com.juarez.ktfirestonefirebase.models

data class User(
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val admin: Boolean = false
)