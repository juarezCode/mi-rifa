package com.juarez.ktfirestonefirebase.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juarez.ktfirestonefirebase.models.Person
import com.juarez.ktfirestonefirebase.repository.PersonRepository
import kotlinx.coroutines.launch

class PersonViewModel(private val personRepository: PersonRepository) : ViewModel() {

    fun savePerson(person: Person) = viewModelScope.launch {
        personRepository.upsert(person)
    }

    fun deletePerson(person: Person) = viewModelScope.launch {
        personRepository.deleteTicket(person)
    }

    fun savePersons(persons: List<Person>) = viewModelScope.launch {
        personRepository.upsertTickets(persons)
    }

    suspend fun checkTicketsByUser(user: String): List<Person> = personRepository.checkTicketsByUser(user)

    suspend fun checkAllTickets(): List<Person> = personRepository.checkAllTickets()

    fun getAllTicketsDB() = personRepository.getTickets()

    fun getTicketsByUserDB(user: String) = personRepository.getTicketsByUser(user)

    fun searchByTicket(ticket: Int) = personRepository.searchByTicket(ticket)

    fun searchByField(query: String) = personRepository.searchByField(query)
}