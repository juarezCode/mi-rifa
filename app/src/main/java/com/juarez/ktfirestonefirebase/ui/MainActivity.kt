package com.juarez.ktfirestonefirebase.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showSnackBarSuccessDeletePerson
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showSnackBarSuccessUpsert
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastDowloading
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorFirestore
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorSearchTicket
import com.juarez.ktfirestonefirebase.util.MyConstants.Companion.DIALOG_MESSAGE_DELETE_TICKET
import com.juarez.ktfirestonefirebase.util.MyConstants.Companion.TITLE_MAIN_ACTIVITY
import com.juarez.ktfirestonefirebase.util.MyDate.Companion.getCurrentDate
import com.juarez.ktfirestonefirebase.util.MyDialog.Companion.showDialogTicketData
import com.juarez.ktfirestonefirebase.viewmodels.TicketViewModel
import com.juarez.ktfirestonefirebase.viewmodels.TicketViewModelProviderFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_person.*
import kotlinx.android.synthetic.main.dialog_person.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val personCollectionRef = Firebase.firestore.collection("persons")
    private lateinit var viewModel: TicketViewModel
    private var id = ""
    private lateinit var personAdapter: PersonAdapter
    private var userIsAdmin = false
    private var userUsername: String? = ""
    private var jobDelete: Job? = null
    private var jobUpdate: Job? = null
    private var jobCreate: Job? = null
    private var jobGetTickets: Job? = null
    private var jobGetAllTickets: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val personRepository = TicketRepository(TicketDatabase(this))
        val viewModelProviderFactory = TicketViewModelProviderFactory(personRepository)
        viewModel =
            ViewModelProvider(this, viewModelProviderFactory).get(TicketViewModel::class.java)

        userIsAdmin = intent.getBooleanExtra("isAdmin", false)
        userUsername = intent.getStringExtra("username")
        val userName = intent.getStringExtra("name")
        supportActionBar?.title = TITLE_MAIN_ACTIVITY + userName
        setupRecyclerView(userIsAdmin)

        if (userIsAdmin) {
            CoroutineScope(IO).launch {
                val tickets = viewModel.checkAllTicketsDB()
                if (tickets.isEmpty()) {
                    getAllTicketsFirestore()
                } else {
                    withContext(Main) {
                        hideLoading()
                    }
                }
            }
        } else {
            CoroutineScope(IO).launch {
                val tickets = viewModel.checkTicketsByUserDB(userUsername ?: "")
                if (tickets.isEmpty()) {
                    getTickets()
                } else {
                    withContext(Main) {
                        hideLoading()
                    }
                }
            }
        }

        fab_add_person.setOnClickListener {

            showDialogUpsert(null, "Agregar Boleto", false, "Boleto guardado exitosamente")
        }

        personAdapter.setOnItemClickListenerUpdate {

            CoroutineScope(IO).launch {
                val personSaved = getPerson(it.ticketNumber)
                withContext(Main) {
                    if (personSaved != null) {
                        showDialogUpsert(
                            personSaved,
                            "Actualizar Boleto",
                            true,
                            "Boleto actualizado exitosamente"
                        )
                    } else {
                        showToastErrorSearchTicket(this@MainActivity)
                    }

                }
            }
        }

        personAdapter.setOnItemClickListenerDelete {
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setMessage(DIALOG_MESSAGE_DELETE_TICKET)
            dialog.setNegativeButton("no") { dialog, _ ->
                dialog.dismiss()
            }
            dialog.setPositiveButton("si") { dialog, _ ->
                dialog.dismiss()
                showLoading()
                CoroutineScope(IO).launch {
                    val personSaved = getPerson(it.ticketNumber)
                    withContext(Main) {
                        if (personSaved != null) {
                            deletePerson(id, personSaved)
                        } else {
                            showToastErrorSearchTicket(this@MainActivity)
                        }
                        hideLoading()
                    }
                }
            }
            dialog.show()
        }

        personAdapter.setOnItemClickListener {
            showDialogTicketData(this@MainActivity, userIsAdmin, it)
        }

        if (userIsAdmin)
            observerDBTickets()
        else {
            observerDBTicketsByUser()
        }

    }

    private fun observerDBTicketsByUser() {
        viewModel.getTicketsByUserDB(userUsername ?: "null").observe(this, Observer { tickets ->
            txt_total_tickets_number.text = tickets.size.toString()
            personAdapter.differ.submitList(tickets)
        })
    }

    private fun observerDBTickets() {
        viewModel.getAllTicketsDB().observe(this, Observer { tickets ->
            txt_total_tickets_number.text = tickets.size.toString()
            personAdapter.differ.submitList(tickets)
        })
    }

    private fun getAllTicketsFirestore() {
        jobGetAllTickets = CoroutineScope(IO).launch {
            withContext(Main) {
                showToastDowloading(this@MainActivity)
                showLoading()
            }
            try {
                val querySnapshot = personCollectionRef.get().await()
                val persons = arrayListOf<Person>()

                querySnapshot?.let {
                    for (document in it) {
                        val person = document.toObject<Person>()
                        persons.add(person)
                    }
                    viewModel.savePersonsDB(persons)
                    withContext(Main) {
                        hideLoading()
                    }
                }
            } catch (e: Exception) {
                withContext(Main) {
                    showToastErrorFirestore(this@MainActivity, e.message.toString())
                }
            }
        }
    }

    private fun getTickets() {
        jobGetTickets = CoroutineScope(IO).launch {
            withContext(Main) {
                showToastDowloading(this@MainActivity)
                showLoading()
            }

            try {
                val search = personCollectionRef
                    .orderBy("ticketNumber")
                    .whereEqualTo("userUpload", userUsername)
                    .get()
                    .await()

                if (search.documents.isNotEmpty()) {
                    search?.let {
                        val persons = arrayListOf<Person>()
                        for (document in it) {
                            val person = document.toObject<Person>()
                            persons.add(person)
                        }
                        viewModel.savePersonsDB(persons)

                    }
                } else {
                    personAdapter.differ.submitList(arrayListOf())
                }
                withContext(Main) {
                    hideLoading()
                }
            } catch (e: Exception) {
                withContext(Main) {
                    showToastErrorFirestore(this@MainActivity, e.message.toString())
                }
            }

        }
    }


    private fun showDialogUpsert(
        personSaved: Person?,
        title: String,
        isUpdate: Boolean,
        successMessage: String
    ) {

        val dialogPerson =
            LayoutInflater.from(this).inflate(R.layout.dialog_person, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogPerson)
            .setTitle(title)
            .setCancelable(false)

        if (isUpdate) {

            dialogPerson.dialog_p_name.setText(personSaved?.name)
            dialogPerson.dialog_p_first_surname.setText(personSaved?.firstSurname)
            dialogPerson.dialog_p_second_surname.setText(personSaved?.secondSurname)
            dialogPerson.dialog_p_phone.setText(personSaved?.phone.toString())
            dialogPerson.dialog_p_address.setText(personSaved?.address.toString())
            dialogPerson.dialog_p_ticket_number.setText(personSaved?.ticketNumber.toString())
            dialogPerson.dialog_p_ticket_number.isEnabled = false
        }

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


            if (name.isEmpty() || firstSurname.isEmpty() || secondSurname.isEmpty() || phone.isEmpty() || address.isEmpty() || ticketNumber.isEmpty()) {
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
                    userUsername ?: "Desconocido",
                    getCurrentDate()
                )
                if (!isUpdate) {
                    savePerson(person, dialog, successMessage)
                } else {
                    updatePerson(person, dialog, successMessage)
                }

            }
        }

        dialogPerson.dialog_p_btn_cancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setupRecyclerView(isAdmin: Boolean) {
        personAdapter =
            PersonAdapter(isAdmin)
        recycler_view_person.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    /*
    private fun observerFirestoreTickets() {
        collection = personCollectionRef
            .orderBy("ticketNumber")
            .limit(1000)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    val persons = arrayListOf<Person>()
                    for (document in it) {
                        val person = document.toObject<Person>()
                        persons.add(person)
                    }
                    personAdapter.differ.submitList(persons)
                    txt_total_tickets.text = "Total: ${persons.size - 1}"
                    hideLoading()
                }
            }
    }
     */

    private suspend fun getPerson(ticketNumber: Int): Person? {
        val personQuery = personCollectionRef
            .whereEqualTo("ticketNumber", ticketNumber)
            .get()
            .await()
        id = personQuery.documents[0].id
        return personQuery.documents[0].toObject<Person>()
    }

    private fun deletePerson(id: String, person: Person) {
        jobDelete = CoroutineScope(IO).launch {

            try {
                personCollectionRef.document(id).delete().await()
                viewModel.deletePersonDB(person)
                withContext(Main) {
                    showSnackBarSuccessDeletePerson(Constraint_layout_parent)
                }
            } catch (e: Exception) {
                withContext(Main) {
                    showToastErrorFirestore(this@MainActivity, e.message.toString())
                }
            }
        }
    }


    private fun updatePerson(person: Person, dialog: AlertDialog, successMessage: String) {
        jobUpdate = CoroutineScope(IO).launch {
            try {
                personCollectionRef.document(id).set(
                    person,
                    SetOptions.merge()
                ).await()
                viewModel.savePersonDB(person)
                withContext(Main) {
                    showSnackBarSuccessUpsert(Constraint_layout_parent, successMessage)
                    dialog.progress_bar_person.visibility = View.GONE
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Main) {
                    showToastErrorFirestore(this@MainActivity, e.message.toString())
                    dialog.progress_bar_person.visibility = View.GONE
                    dialog.dialog_p_btn_ok.isEnabled = true
                }
            }

        }
    }


    private fun savePerson(person: Person, dialog: AlertDialog, successMessage: String) {
        jobCreate = CoroutineScope(IO).launch {
            val isTicketAssigned = isTicketAssigned(person.ticketNumber)
            if (isTicketAssigned) {
                withContext(Main) {
                    dialog.txt_dialog_input_required.text =
                        "Boleto ${person.ticketNumber} ya registrado"
                    dialog.txt_dialog_input_required.visibility = View.VISIBLE
                    dialog.dialog_p_btn_ok.isEnabled = true
                    dialog.progress_bar_person.visibility = View.GONE
                }
            } else {
                try {
                    personCollectionRef.add(person).await()
                    viewModel.savePersonDB(person)
                    withContext(Main) {
                        showSnackBarSuccessUpsert(Constraint_layout_parent, successMessage)
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    withContext(Main) {
                        showToastErrorFirestore(this@MainActivity, e.message.toString())
                    }
                }
                withContext(Main) {
                    dialog.dialog_p_btn_ok.isEnabled = true
                    dialog.progress_bar_person.visibility = View.GONE
                }
            }
        }
    }


    private suspend fun isTicketAssigned(tickerNumber: Number): Boolean {
        var isTicketAssigned = false
        try {
            val querySnapshot = personCollectionRef
                .whereEqualTo("ticketNumber", tickerNumber)
                .get()
                .await()

            isTicketAssigned = querySnapshot.documents.isNotEmpty()

        } catch (e: Exception) {
            withContext(Main) {
                showToastErrorFirestore(this@MainActivity, e.message.toString())
                isTicketAssigned = false
            }
        }
        return isTicketAssigned
    }

    private fun showLoading() {
        progress_bar_ticket.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progress_bar_ticket.visibility = View.GONE
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!userIsAdmin) {
            val item = menu.findItem(R.id.action_add_user)
            item.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                if (userIsAdmin)
                    getAllTicketsFirestore()
                else
                    getTickets()

                return true
            }
            R.id.action_add_user -> {
                val intent = Intent(this@MainActivity, UserActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_search_ticket -> {
                val intent = Intent(this@MainActivity, SearchActivity::class.java)
                intent.putExtra("isAdmin", userIsAdmin)
                startActivity(intent)
                return true
            }
            R.id.action_close_sesion -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }

    override fun onDestroy() {
        jobUpdate?.cancel()
        jobDelete?.cancel()
        jobCreate?.cancel()
        jobGetTickets?.cancel()
        jobGetAllTickets?.cancel()
        super.onDestroy()
    }

}