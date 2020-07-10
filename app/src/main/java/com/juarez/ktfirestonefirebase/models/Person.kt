package com.juarez.ktfirestonefirebase.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "persons"
)
data class Person(

    val name: String = "",
    val firstSurname: String = "",
    val secondSurname: String = "",
    val phone: String = "",
    val address: String = "",
    @PrimaryKey
    val ticketNumber: Int = -1,
    val userUpload: String = ""
)