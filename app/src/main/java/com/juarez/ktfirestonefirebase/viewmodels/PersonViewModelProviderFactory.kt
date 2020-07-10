package com.juarez.ktfirestonefirebase.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juarez.ktfirestonefirebase.repository.PersonRepository

class PersonViewModelProviderFactory(
    private val personRepository: PersonRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PersonViewModel(personRepository) as T
    }
}