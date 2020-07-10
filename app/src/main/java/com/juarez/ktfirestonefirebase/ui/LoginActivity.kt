package com.juarez.ktfirestonefirebase.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.juarez.ktfirestonefirebase.R
import com.juarez.ktfirestonefirebase.models.User
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastErrorFirestore
import com.juarez.ktfirestonefirebase.util.Messages.Companion.showToastLoginError
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private val userCollectionRef = Firebase.firestore.collection("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.title = "Iniciar Sesión"
        addTextWatcher()
        btn_login.setOnClickListener {
            val username = edt_login_username.text.toString()
            val password = edt_login_password.text.toString()
            login(username, password)
        }
    }

    private fun login(username: String, password: String) = CoroutineScope(Dispatchers.IO).launch {
        withContext(Main) {
            edt_login_username.hideKeyboard()
            progress_bar_login.visibility = View.VISIBLE
            btn_login.isEnabled = false
        }
        try {
            val userSearch = userCollectionRef
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .get()
                .await()

            withContext(Main) {

                if (userSearch.documents.isNotEmpty()) {

                    val user = userSearch.documents[0].toObject<User>()
                    user?.let {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("isAdmin", user.admin)
                        intent.putExtra("name", user.name)
                        intent.putExtra("username", user.username)
                        startActivity(intent)
                        cleanInputs()
                    }

                } else {
                    showToastLoginError(this@LoginActivity)
                }
                progress_bar_login.visibility = View.GONE
                btn_login.isEnabled = true
            }
        } catch (e: Exception) {
            withContext(Main) {
                showToastErrorFirestore(this@LoginActivity, e.message.toString())
                btn_login.isEnabled = true
            }
        }

    }

    private fun addTextWatcher() {
        edt_login_password.addTextChangedListener(textWatcher)
        edt_login_username.addTextChangedListener(textWatcher)
    }

    private fun cleanInputs() {
        edt_login_password.text?.clear()
        edt_login_username.text?.clear()
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (edt_login_password?.text.toString().isEmpty()) {
                edt_login_layout_password.error = "Contraseña requerida"

            } else {

                edt_login_layout_password?.error = null
            }
            if (edt_login_username?.text.toString().isEmpty()) {
                edt_login_layout_username.error = "Nombre de usuario requerido"

            } else {
                edt_login_layout_username.error = null

            }

            btn_login?.isEnabled = edt_login_password?.text.toString()
                .isNotEmpty() && edt_login_username?.text.toString().isNotEmpty()

        }
    }
}