package com.juarez.ktfirestonefirebase.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.juarez.ktfirestonefirebase.models.Person

@Database(
    entities = [Person::class],
    version = 1
)
abstract class TicketDatabase : RoomDatabase() {

    abstract fun getTicketDao(): TicketDao

    companion object {
        @Volatile
        private var instance: TicketDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also { instance = it }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                TicketDatabase::class.java,
                "ticket_db.db"
            ).build()
    }
}