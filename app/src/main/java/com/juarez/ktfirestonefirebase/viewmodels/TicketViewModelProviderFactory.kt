package com.juarez.ktfirestonefirebase.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juarez.ktfirestonefirebase.repository.TicketRepository

class TicketViewModelProviderFactory(
    private val ticketRepository: TicketRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TicketViewModel(ticketRepository) as T
    }
}