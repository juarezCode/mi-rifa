package com.juarez.ktfirestonefirebase.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.juarez.ktfirestonefirebase.R
import com.juarez.ktfirestonefirebase.models.User
import kotlinx.android.synthetic.main.item_user.view.*

class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.username == newItem.username
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_user,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: UserAdapter.UserViewHolder, position: Int) {
        val user = differ.currentList[position]
        holder.itemView.apply {
            txt_item_user_data.text = "${user.name} (${user.username})"


            setOnClickListener {
                onItemClickListener?.let { it(user) }
            }
        }
        holder.itemView.btn_user_delete.apply {
            setOnClickListener {
                onItemClickListenerDelete?.let { it(user) }
            }
        }
        holder.itemView.btn_user_edit.apply {
            setOnClickListener {
                onItemClickListenerUpdate?.let { it(user) }
            }
        }
    }

    private var onItemClickListenerDelete: ((User) -> Unit)? = null
    private var onItemClickListenerUpdate: ((User) -> Unit)? = null
    private var onItemClickListener: ((User) -> Unit)? = null

    fun setOnItemClickListenerUpdate(listener: (User) -> Unit) {
        onItemClickListenerUpdate = listener
    }

    fun setOnItemClickListenerDelete(listener: (User) -> Unit) {
        onItemClickListenerDelete = listener
    }

    fun setOnItemClickListener(listener: (User) -> Unit) {
        onItemClickListener = listener
    }
}
