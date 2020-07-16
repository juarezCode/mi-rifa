package com.juarez.ktfirestonefirebase.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.juarez.ktfirestonefirebase.models.Person

class MyDialog {
    companion object {
        fun showDialogTicketData(context: Context, userIsAdmin: Boolean, ticket: Person) {
            var moreData = ""
            if (userIsAdmin)
                moreData = "\nagregado por: ${ticket.userUpload}\nfecha: ${ticket.createDate}"

            AlertDialog.Builder(context)
                .setMessage(
                    "${ticket.name} ${ticket.firstSurname} ${ticket.secondSurname}" +
                            "\n${ticket.phone}\n${ticket.address}" + moreData
                )
                .show()
        }
    }
}