package com.juarez.ktfirestonefirebase.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MyDate {
    companion object {
        fun getCurrentDate(): String {
            var answer: String
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                answer = current.format(formatter)
            } else {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                answer = formatter.format(Date())
            }
            return answer
        }
    }
}