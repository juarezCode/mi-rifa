package com.juarez.ktfirestonefirebase.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.juarez.ktfirestonefirebase.models.Person
import com.juarez.ktfirestonefirebase.R
import kotlinx.android.synthetic.main.item_person.view.*

class PersonAdapter(val isAdmin: Boolean) : RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    inner class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Person>() {
        override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean {
            return oldItem.ticketNumber == newItem.ticketNumber
        }

        override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        return PersonViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_person,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = differ.currentList[position]
        holder.itemView.apply {
            txt_item_person_name.text =
                "${person.name} ${person.firstSurname}"

            txt_item_person_address.text = "${person.address}"
            txt_item_person_ticket_number.text = person.ticketNumber.toString()
            if (!isAdmin) {
                item_person_btn_edit.visibility = View.GONE
                item_person_btn_delete.visibility = View.GONE
            }

            setOnClickListener {
                onItemClickListener?.let { it(person) }
            }
        }
        holder.itemView.item_person_btn_delete.apply {
            setOnClickListener {
                onItemClickListenerDelete?.let { it(person) }
            }
        }
        holder.itemView.item_person_btn_edit.apply {
            setOnClickListener {
                onItemClickListenerUpdate?.let { it(person) }
            }
        }
    }

    private var onItemClickListenerDelete: ((Person) -> Unit)? = null
    private var onItemClickListenerUpdate: ((Person) -> Unit)? = null
    private var onItemClickListener: ((Person) -> Unit)? = null

    fun setOnItemClickListenerUpdate(listener: (Person) -> Unit) {
        onItemClickListenerUpdate = listener
    }

    fun setOnItemClickListenerDelete(listener: (Person) -> Unit) {
        onItemClickListenerDelete = listener
    }

    fun setOnItemClickListener(listener: (Person) -> Unit) {
        onItemClickListener = listener
    }
}