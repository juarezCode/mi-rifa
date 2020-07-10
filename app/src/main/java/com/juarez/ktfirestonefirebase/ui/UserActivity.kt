package com.juarez.ktfirestonefirebase.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.juarez.ktfirestonefirebase.R
import com.juarez.ktfirestonefirebase.adapters.UserAdapter
import com.juarez.ktfirestonefirebase.models.User
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showSnackBarSuccessDelete
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showSnackBarSuccessUpsert
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorFirestore
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorUser
import kotlinx.android.synthetic.main.activity_user.*
import kotlinx.android.synthetic.main.dialog_user.*
import kotlinx.android.synthetic.main.dialog_user.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserActivity : AppCompatActivity() {
    private lateinit var userAdapter: UserAdapter
    private val userCollectionRef = Firebase.firestore.collection("users")
    private var id = ""
    private lateinit var collection: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        showBackButton()
        setupRecyclerView()
        subscribeToUsers()
        fab_add_user.setOnClickListener {
            showDialogUpsert(null, "Agregar Usuario", false, "Usuario agregado exitosamente.")
        }

        userAdapter.setOnItemClickListenerUpdate {
            CoroutineScope(IO).launch {
                val userSaved = getUser(it.username)
                withContext(Main) {
                    if (userSaved != null) {
                        showDialogUpsert(
                            userSaved,
                            "Actualizar Usuario",
                            true,
                            "Usuario actualizado exitosamente"
                        )
                    } else {
                        showToastErrorUser(this@UserActivity)
                    }

                }
            }
        }

        userAdapter.setOnItemClickListenerDelete {
            showLoading()
            CoroutineScope(IO).launch {
                val userSaved = getUser(it.username)
                withContext(Main) {
                    if (userSaved != null) {
                        deleteUser(id)
                    } else {
                        showToastErrorUser(this@UserActivity)
                    }
                    hideLoading()
                }
            }
        }

    }

    private suspend fun getUser(username: String): User? {
        val userQuery = userCollectionRef
            .whereEqualTo("username", username)
            .get()
            .await()
        id = userQuery.documents[0].id
        return userQuery.documents[0].toObject<User>()
    }

    private fun subscribeToUsers() {
        collection = userCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                showToastErrorFirestore(this@UserActivity, it.toString())
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val users = arrayListOf<User>()
                for (document in it) {
                    val user = document.toObject<User>()
                    if (!user.admin) {
                        users.add(user)
                    }

                }
                userAdapter.differ.submitList(users)
            }
            hideLoading()
        }
    }

    private fun deleteUser(id: String) = CoroutineScope(IO).launch {

        try {
            userCollectionRef.document(id).delete().await()
            withContext(Main) {
                showSnackBarSuccessDelete(coordinator_layout_user)
            }
        } catch (e: Exception) {
            withContext(Main) {
                showToastErrorFirestore(this@UserActivity, e.message.toString())
            }
        }
    }

    private fun showDialogUpsert(
        userSaved: User?,
        title: String,
        isUpdate: Boolean,
        successMessage: String
    ) {

        val dialogUser =
            LayoutInflater.from(this).inflate(R.layout.dialog_user, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogUser)
            .setTitle(title)
            .setCancelable(false)

        if (isUpdate) {
            dialogUser.dialog_user_name.setText(userSaved?.name)
            dialogUser.dialog_user_username.setText(userSaved?.username)
            dialogUser.dialog_user_password.setText(userSaved?.password)
            dialogUser.dialog_user_username.isEnabled = false
        }

        val dialog = builder.show()

        dialogUser.dialog_user_btn_ok.setOnClickListener {
            dialogUser.dialog_user_username.hideKeyboard()
            dialogUser.dialog_user_btn_ok.isEnabled = false
            dialogUser.dialog_progress_bar_user.visibility = View.VISIBLE


            val name = dialogUser.dialog_user_name.text.toString()
            val username = dialogUser.dialog_user_username.text.toString()
            val password = dialogUser.dialog_user_password.text.toString()

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                dialogUser.dialog_user_input_required.visibility = View.VISIBLE
                dialogUser.dialog_progress_bar_user.visibility = View.GONE
                dialog.dialog_user_btn_ok.isEnabled = true
            } else {
                val user = User(
                    name,
                    username,
                    password,
                    false
                )
                if (!isUpdate) {
                    saveUser(user, dialog, successMessage)
                } else {
                    updateUser(user, dialog, successMessage)
                }

            }
        }

        dialogUser.dialog_user_btn_cancel.setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun updateUser(user: User, dialog: AlertDialog, successMessage: String) =
        CoroutineScope(IO).launch {
            try {
                userCollectionRef.document(id).set(
                    user,
                    SetOptions.merge()
                ).await()
                withContext(Main) {
                    showSnackBarSuccessUpsert(coordinator_layout_user, successMessage)
                    dialog.dialog_progress_bar_user.visibility = View.GONE
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Main) {
                    dialog.dialog_user_btn_ok.isEnabled = true
                    dialog.dialog_progress_bar_user.visibility = View.GONE
                    showToastErrorFirestore(this@UserActivity, e.message.toString())
                }
            }

        }

    private fun saveUser(user: User, dialog: AlertDialog, successMessage: String) =
        CoroutineScope(IO).launch {
            val isUsernameAssigned = isUsernameAssigned(user.username)
            if (isUsernameAssigned) {
                withContext(Main) {
                    dialog.dialog_user_input_required.text =
                        "Nombre de usuario ${user.username} ya registrado"
                    dialog.dialog_user_input_required.visibility = View.VISIBLE
                    dialog.dialog_user_btn_ok.isEnabled = true
                    dialog.dialog_progress_bar_user.visibility = View.GONE
                }
            } else {
                try {
                    userCollectionRef.add(user).await()
                    withContext(Main) {
                        showSnackBarSuccessUpsert(coordinator_layout_user, successMessage)
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    withContext(Main) {
                        showToastErrorFirestore(this@UserActivity, e.message.toString())
                    }
                }
                withContext(Main) {
                    dialog.dialog_user_btn_ok.isEnabled = true
                    dialog.dialog_progress_bar_user.visibility = View.GONE
                }
            }
        }

    private suspend fun isUsernameAssigned(username: String): Boolean {
        var isUsernameAssigned = false
        try {
            val querySnapshot = userCollectionRef
                .whereEqualTo("username", username)
                .get()
                .await()

            isUsernameAssigned = querySnapshot.documents.isNotEmpty()

        } catch (e: Exception) {
            withContext(Main) {
                showToastErrorFirestore(this@UserActivity, e.message.toString())

                isUsernameAssigned = false
            }
        }
        return isUsernameAssigned
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        recycler_view_user.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@UserActivity)
        }
    }

    private fun showLoading() {
        progress_bar_user.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progress_bar_user.visibility = View.GONE
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun showBackButton() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Administrar Usuarios"
        }
    }

    @Override
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        collection.remove()
        super.onDestroy()
    }
}