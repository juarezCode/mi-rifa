package com.juarez.ktfirestonefirebase.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.juarez.ktfirestonefirebase.models.Person

@Dao
interface TicketDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsert(person: Person)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTickets(persons: List<Person>)

    @Query("SELECT * FROM persons")
    fun getAllTickets(): LiveData<List<Person>>

    @Query("SELECT * FROM persons WHERE userUpload IN (:user)")
    fun getTicketsByUser(user: String): LiveData<List<Person>>

    @Query("SELECT * FROM persons WHERE ticketNumber IN (:ticket)")
    fun searchByTicket(ticket: Int): LiveData<List<Person>>
}