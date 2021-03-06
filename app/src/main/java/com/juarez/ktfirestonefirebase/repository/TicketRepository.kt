package com.juarez.ktfirestonefirebase.repository

import com.juarez.ktfirestonefirebase.db.TicketDatabase
import com.juarez.ktfirestonefirebase.models.Person

class TicketRepository(private val db: TicketDatabase) {

    suspend fun upsert(person: Person) = db.getTicketDao().upsert(person)

    suspend fun checkTicketsByUser(user: String): List<Person> = db.getTicketDao().checkTicketsByUser(user)

    suspend fun checkAllTickets(): List<Person> = db.getTicketDao().checkAllTickets()

    suspend fun deleteTicket(person: Person) = db.getTicketDao().deleteTicket(person)

    suspend fun upsertTickets(persons: List<Person>) = db.getTicketDao().upsertTickets(persons)

    fun getTickets() = db.getTicketDao().getAllTickets()

    fun getTicketsByUser(user: String) = db.getTicketDao().getTicketsByUser(user)

    fun searchByTicket(ticket: Int) = db.getTicketDao().searchByTicket(ticket)

    fun searchByField(query: String) = db.getTicketDao().searchByField(query)

}