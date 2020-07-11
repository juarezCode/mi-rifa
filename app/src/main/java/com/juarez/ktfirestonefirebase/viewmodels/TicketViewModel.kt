package com.juarez.ktfirestonefirebase.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juarez.ktfirestonefirebase.models.Person
import com.juarez.ktfirestonefirebase.repository.TicketRepository
import kotlinx.coroutines.launch

class TicketViewModel(private val ticketRepository: TicketRepository) : ViewModel() {

    fun savePersonDB(person: Person) = viewModelScope.launch {
        ticketRepository.upsert(person)
    }

    fun deletePersonDB(person: Person) = viewModelScope.launch {
        ticketRepository.deleteTicket(person)
    }

    fun savePersonsDB(persons: List<Person>) = viewModelScope.launch {
        ticketRepository.upsertTickets(persons)
    }

    suspend fun checkTicketsByUserDB(user: String): List<Person> = ticketRepository.checkTicketsByUser(user)

    suspend fun checkAllTicketsDB(): List<Person> = ticketRepository.checkAllTickets()

    fun getAllTicketsDB() = ticketRepository.getTickets()

    fun getTicketsByUserDB(user: String) = ticketRepository.getTicketsByUser(user)

    fun searchByTicketDB(ticket: Int) = ticketRepository.searchByTicket(ticket)

    fun searchByFieldDB(query: String) = ticketRepository.searchByField(query)
}