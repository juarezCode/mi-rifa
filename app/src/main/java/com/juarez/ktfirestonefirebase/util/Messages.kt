package com.juarez.ktfirestonefirebase.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class Messages {
    companion object {
        fun showToastErrorUser(context: Context) {
            Toast.makeText(context, "No se encontro al usuario", Toast.LENGTH_SHORT)
                .show()
        }

        fun showToastErrorSearchTicket(context: Context) {
            Toast.makeText(context, "No se encontro el boleto", Toast.LENGTH_SHORT)
                .show()
        }

        fun showToastErrorFirestore(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT)
                .show()
        }
        fun showToastSuccessDeletePerson(context: Context) {
            Toast.makeText(context, "Boleto eliminado exitosamente", Toast.LENGTH_SHORT)
                .show()
        }

        fun showToastSuccessUpdatePerson(context: Context) {
            Toast.makeText(context, "Boleto actualizado exitosamente", Toast.LENGTH_SHORT)
                .show()
        }

        fun showToastLoginError(context: Context) {
            Toast.makeText(
                context, "Nombre de usuario o contraseña incorrectos", Toast.LENGTH_LONG
            )
                .show()
        }

        fun showSnackBarSuccessDelete(view: View) {
            Snackbar.make(view, "Usuario eliminado exitosamente", Snackbar.LENGTH_SHORT).show()
        }

        fun showSnackBarSuccessDeletePerson(view: View) {
            Snackbar.make(view, "Boleto eliminado exitosamente", Snackbar.LENGTH_SHORT).show()
        }

        fun showSnackBarSuccessUpsert(view: View, message: String) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}