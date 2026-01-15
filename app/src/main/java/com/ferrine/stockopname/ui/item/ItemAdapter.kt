package com.ferrine.stockopname.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.Item

class ItemAdapter(private var items: List<Item>) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemId: TextView = view.findViewById(R.id.tvItemId)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvArt: TextView = view.findViewById(R.id.tvArt)
        val tvMaterial: TextView = view.findViewById(R.id.tvMaterial)
        val tvColor: TextView = view.findViewById(R.id.tvColor)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvStockQty: TextView = view.findViewById(R.id.tvStockQty)
        val tvPrintQty: TextView = view.findViewById(R.id.tvPrintQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvItemName.text = item.name
        holder.tvItemId.text = item.itemId
        holder.tvDescription.text = item.description
        holder.tvArt.text = "Art: ${item.article}"
        holder.tvMaterial.text = "Mat: ${item.material}"
        holder.tvColor.text = "Col: ${item.color}"
        holder.tvCategory.text = "Cat: ${item.category}"
        holder.tvStockQty.text = "Stock: ${item.stockQty}"
        holder.tvPrintQty.text = "Print: ${item.printQty}"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Item>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
