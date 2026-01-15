package com.ferrine.stockopname.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.User

class UserAdapter(
    private var users: List<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFullname: TextView = view.findViewById(R.id.tvFullname)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val ibMore: ImageButton = view.findViewById(R.id.ibMore)
    }

    fun updateData(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvFullname.text = user.fullname
        
        val roles = mutableListOf<String>()
        if (user.isAdmin) roles.add("Admin")
        if (user.allowOpname) roles.add("Opname")
        if (user.allowReceiving) roles.add("Receiving")
        if (user.allowTransfer) roles.add("Transfer")
        if (user.allowPrintlabel) roles.add("PrintLabel")
        
        val roleStr = if (roles.isEmpty()) "No Roles" else roles.joinToString(", ")
        holder.tvUsername.text = "${user.username} | $roleStr"

        holder.ibMore.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Edit")
            popup.menu.add("Delete")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Edit" -> onEditClick(user)
                    "Delete" -> onDeleteClick(user)
                }
                true
            }
            popup.show()
        }

        holder.itemView.setOnClickListener { onEditClick(user) }
    }

    override fun getItemCount() = users.size
}
