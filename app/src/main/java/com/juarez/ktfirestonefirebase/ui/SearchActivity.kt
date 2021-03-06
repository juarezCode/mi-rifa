package com.juarez.ktfirestonefirebase.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.juarez.ktfirestonefirebase.R
import com.juarez.ktfirestonefirebase.adapters.PersonAdapter
import com.juarez.ktfirestonefirebase.db.TicketDatabase
import com.juarez.ktfirestonefirebase.models.Person
import com.juarez.ktfirestonefirebase.repository.TicketRepository
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorFirestore
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorSearchTicket
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastSuccessDeletePerson
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastSuccessUpdatePerson
import com.juarez.ktfirestonefirebase.util.MyConstants.Companion.DIALOG_MESSAGE_DELETE_TICKET
import com.juarez.ktfirestonefirebase.util.MyConstants.Companion.TITLE_SEARCH_ACTIVITY
import com.juarez.ktfirestonefirebase.util.MyDate.Companion.getCurrentDate
import com.juarez.ktfirestonefirebase.util.MyDialog.Companion.showDialogTicketData
import com.juarez.ktfirestonefirebase.viewmodels.TicketViewModel
import com.juarez.ktfirestonefirebase.viewmodels.TicketViewModelProviderFactory
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.dialog_person.*
import kotlinx.android.synthetic.main.dialog_person.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.tasks.await

class SearchActivity : AppCompatActivity() {
    private lateinit var personAdapter: PersonAdapter
    private val personCollectionRef = Firebase.firestore.collection("persons")
    private lateinit var viewModel: TicketViewModel
    private var id = ""
    private var filterSearch = "ticketNumber"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val personRepository = TicketRepository(TicketDatabase(this))
        val viewModelProviderFactory = TicketViewModelProviderFactory(personRepository)
        viewModel =
            ViewModelProvider(this, viewModelProviderFactory).get(TicketViewModel::class.java)

        val userIsAdmin = intent.getBooleanExtra("isAdmin", false)
        showBackButton()
        setupRecyclerView(userIsAdmin)

        if (!userIsAdmin)
            hideRadioInput()

        radio_group.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radio_option_ticket -> {
                    filterSearch = "ticketNumber"
                    edt_search.inputType = InputType.TYPE_CLASS_NUMBER
                }
                R.id.radio_option_name -> {
                    filterSearch = "name"
                    edt_search.inputType = InputType.TYPE_CLASS_TEXT
                }
                R.id.radio_option_address -> {
                    filterSearch = "address"
                    edt_search.inputType = InputType.TYPE_CLASS_TEXT
                }
                R.id.radio_option_user -> {
                    filterSearch = "userUpload"
                    edt_search.inputType = InputType.TYPE_CLASS_TEXT
                }
            }
        }

        var job: Job? = null
        edt_search.addTextChangedListener { editable ->
            job?.cancel()
            job = MainScope().launch {
                delay(1000L)
                editable?.let {
                    if (editable.toString().isNotEmpty()) {
                        showLoading()
                        if (filterSearch == "ticketNumber") {
                            searchTicketInDB(editable.toString().toInt())
                        } else {
                            searchFieldsInDB(editable.toString())
                        }
                    } else {
                        txt_search_number_coincidences.text = "Coincidencias: 0"
                        personAdapter.differ.submitList(listOf())
                        hideLoading()
                        hideLabel()
                    }
                }
            }
        }

        personAdapter.setOnItemClickListenerUpdate {

            CoroutineScope(Dispatchers.IO).launch {
                val personSaved = getPerson(it.ticketNumber)
                withContext(Main) {
                    if (personSaved != null) {
                        showDialogUpdate(
                            personSaved,
                            "Actualizar Boleto",
                            personSaved.userUpload
                        )
                    } else {
                        showToastErrorSearchTicket(this@SearchActivity)
                    }

                }
            }
        }

        personAdapter.setOnItemClickListenerDelete {
            val dialog = AlertDialog.Builder(this@SearchActivity)
                .setMessage(DIALOG_MESSAGE_DELETE_TICKET)
            dialog.setNegativeButton("no") { dialog, _ ->
                dialog.dismiss()
            }
            dialog.setPositiveButton("si") { dialog, _ ->
                dialog.dismiss()
                showLoading()
                CoroutineScope(Dispatchers.IO).launch {
                    val personSaved = getPerson(it.ticketNumber)
                    withContext(Main) {
                        if (personSaved != null) {
                            deletePerson(id, personSaved)
                        } else {
                            showToastErrorSearchTicket(this@SearchActivity)
                        }
                        hideLoading()
                    }
                }
            }
            dialog.show()
        }

        personAdapter.setOnItemClickListener {
            showDialogTicketData(this@SearchActivity, userIsAdmin, it)
        }
    }


    private fun showDialogUpdate(
        personSaved: Person?,
        title: String,
        userUpload: String
    ) {
        val dialogPerson =
            LayoutInflater.from(this).inflate(R.layout.dialog_person, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogPerson)
            .setTitle(title)
            .setCancelable(false)

        dialogPerson.dialog_p_name.setText(personSaved?.name)
        dialogPerson.dialog_p_first_surname.setText(personSaved?.firstSurname)
        dialogPerson.dialog_p_second_surname.setText(personSaved?.secondSurname)
        dialogPerson.dialog_p_phone.setText(personSaved?.phone.toString())
        dialogPerson.dialog_p_address.setText(personSaved?.address.toString())
        dialogPerson.dialog_p_ticket_number.setText(personSaved?.ticketNumber.toString())
        dialogPerson.dialog_p_ticket_number.isEnabled = false

        val dialog = builder.show()

        dialogPerson.dialog_p_btn_ok.setOnClickListener {
            dialogPerson.dialog_p_name.hideKeyboard()
            dialogPerson.dialog_p_btn_ok.isEnabled = false
            dialogPerson.progress_bar_person.visibility = View.VISIBLE

            val name = dialogPerson.dialog_p_name.text.toString()
            val firstSurname = dialogPerson.dialog_p_first_surname.text.toString()
            val secondSurname = dialogPerson.dialog_p_second_surname.text.toString()
            val phone = dialogPerson.dialog_p_phone.text.toString()
            val address = dialogPerson.dialog_p_address.text.toString()
            val ticketNumber = dialogPerson.dialog_p_ticket_number.text.toString()

            if (name.isEmpty() || firstSurname.isEmpty() || secondSurname.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                dialogPerson.txt_dialog_input_required.visibility = View.VISIBLE
                dialog.dialog_p_btn_ok.isEnabled = true
                dialog.progress_bar_person.visibility = View.GONE
            } else {
                val person = Person(
                    name,
                    firstSurname,
                    secondSurname,
                    phone,
                    address,
                    ticketNumber.toInt(),
                    userUpload,
                    getCurrentDate()
                )
                updatePerson(person, dialog)
            }
        }
        dialogPerson.dialog_p_btn_cancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun updatePerson(person: Person, dialog: AlertDialog) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                personCollectionRef.document(id).set(
                    person,
                    SetOptions.merge()
                ).await()
                viewModel.savePersonDB(person)
                withContext(Main) {
                    showToastSuccessUpdatePerson(this@SearchActivity)
                    dialog.progress_bar_person.visibility = View.GONE
                    dialog.dismiss()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Main) {
                    showToastErrorFirestore(this@SearchActivity, e.message.toString())
                    dialog.progress_bar_person.visibility = View.GONE
                    dialog.dialog_p_btn_ok.isEnabled = true
                }
            }
        }

    private fun searchTicketInDB(ticket: Int) {
        viewModel.searchByTicketDB(ticket).observe(this, Observer { tickets ->
            if (tickets.isNotEmpty()) {
                txt_search_number_coincidences.text = "Coincidencias ${tickets.size}"
                personAdapter.differ.submitList(tickets)
                hideLabel()
            } else {
                personAdapter.differ.submitList(arrayListOf())
                showLabel()
            }
            hideLoading()
        })
    }

    private fun searchFieldsInDB(query: String) {
        viewModel.searchByFieldDB(query).observe(this, Observer { tickets ->
            if (tickets.isNotEmpty()) {
                txt_search_number_coincidences.text = "Coincidencias ${tickets.size}"
                personAdapter.differ.submitList(tickets)
                hideLabel()
            } else {
                personAdapter.differ.submitList(arrayListOf())
                showLabel()
            }
            hideLoading()
        })
    }


    private suspend fun getPerson(ticketNumber: Int): Person? {
        val personQuery = personCollectionRef
            .whereEqualTo("ticketNumber", ticketNumber)
            .get()
            .await()
        id = personQuery.documents[0].id
        return personQuery.documents[0].toObject<Person>()
    }

    private fun deletePerson(id: String, person: Person) = CoroutineScope(Dispatchers.IO).launch {

        try {
            personCollectionRef.document(id).delete().await()
            viewModel.deletePersonDB(person)
            withContext(Main) {
                showToastSuccessDeletePerson(this@SearchActivity)
                finish()
            }
        } catch (e: Exception) {
            withContext(Main) {
                showToastErrorFirestore(this@SearchActivity, e.message.toString())
            }
        }
    }

    private fun setupRecyclerView(isAdmin: Boolean) {
        personAdapter =
            PersonAdapter(isAdmin)
        recycler_view_search.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
        }
    }

    private fun showLabel() {
        txt_search_no_results.visibility = View.VISIBLE
    }

    private fun hideLabel() {
        txt_search_no_results.visibility = View.GONE
    }

    private fun showLoading() {
        progress_bar_search.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progress_bar_search.visibility = View.GONE
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hideRadioInput() {
        radio_option_user.visibility = View.GONE
        radio_option_name.visibility = View.GONE
        radio_option_address.visibility = View.GONE
    }

    private fun showBackButton() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = TITLE_SEARCH_ACTIVITY
        }
    }

    @Override
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}