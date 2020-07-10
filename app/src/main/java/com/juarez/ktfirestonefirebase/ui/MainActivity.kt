package com.juarez.ktfirestonefirebase.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log.d
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.juarez.ktfirestonefirebase.R
import com.juarez.ktfirestonefirebase.adapters.PersonAdapter
import com.juarez.ktfirestonefirebase.db.TicketDatabase
import com.juarez.ktfirestonefirebase.models.Person
import com.juarez.ktfirestonefirebase.repository.PersonRepository
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorFirestore
import com.juarez.ktfirestonefirebase.viewmodels.PersonViewModel
import com.juarez.ktfirestonefirebase.viewmodels.PersonViewModelProviderFactory
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
    private lateinit var viewModel: PersonViewModel
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
        val personRepository = PersonRepository(TicketDatabase(this))
        val viewModelProviderFactory = PersonViewModelProviderFactory(personRepository)
        viewModel =
            ViewModelProvider(this, viewModelProviderFactory).get(PersonViewModel::class.java)

        userIsAdmin = intent.getBooleanExtra("isAdmin", false)
        userUsername = intent.getStringExtra("username")
        val userName = intent.getStringExtra("name")
        supportActionBar?.title = "Hola $userName"
        setupRecyclerView(userIsAdmin)

        if (userIsAdmin) {
            CoroutineScope(IO).launch {
                val tickets = viewModel.checkAllTickets()
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
                val tickets = viewModel.checkTicketsByUser(userUsername ?: "")
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
                        Toast.makeText(
                            this@MainActivity,
                            "No se encontro a el boleto",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }

                }
            }
        }

        personAdapter.setOnItemClickListenerDelete {
            showLoading()
            CoroutineScope(IO).launch {
                val personSaved = getPerson(it.ticketNumber)
                withContext(Main) {
                    if (personSaved != null) {
                        deletePerson(id, personSaved)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No se encontro a el boleto",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    hideLoading()
                }
            }
        }
        personAdapter.setOnItemClickListener {
            var userUpload = ""
            if (userIsAdmin)
                userUpload = "\nagregado por: ${it.userUpload}"

            AlertDialog.Builder(this)
                .setMessage(
                    "${it.name} ${it.firstSurname} ${it.secondSurname}" +
                            "\n${it.phone}\n${it.address}" + userUpload
                )
                .show()
        }

        if (userIsAdmin)
            observerDBTickets()
        else {
            observerDBTicketsByUser()
        }

    }

    private fun observerDBTicketsByUser() {
        viewModel.getTicketsByUserDB(userUsername ?: "null").observe(this, Observer { tickets ->
            txt_total_tickets.text = "Total: ${tickets.size}"
            personAdapter.differ.submitList(tickets)
        })
    }

    private fun observerDBTickets() {
        viewModel.getAllTicketsDB().observe(this, Observer { tickets ->
            txt_total_tickets.text = "Total: ${tickets.size}"
            personAdapter.differ.submitList(tickets)
        })
    }

    private fun getAllTicketsFirestore() {
        jobGetAllTickets = CoroutineScope(IO).launch {
            withContext(Main) {
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
                    viewModel.savePersons(persons)
                    withContext(Main) {
                        hideLoading()
                    }
                }
            } catch (e: Exception) {
                withContext(Main) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getTickets() {
        jobGetTickets = CoroutineScope(IO).launch {
            withContext(Main) {
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
                        viewModel.savePersons(persons)

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
                    d("error", e.message.toString())
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
                    userUsername ?: "Desconocido"
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
                viewModel.deletePerson(person)
                withContext(Main) {
                    Snackbar.make(
                        Constraint_layout_parent,
                        "Boleto eliminado correctamente",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Main) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
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
                viewModel.savePerson(person)
                withContext(Main) {
                    Snackbar.make(
                        Constraint_layout_parent,
                        successMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    dialog.progress_bar_person.visibility = View.GONE
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Main) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
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
                    viewModel.savePerson(person)
                    withContext(Main) {
                        Snackbar.make(
                            Constraint_layout_parent,
                            successMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    withContext(Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
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