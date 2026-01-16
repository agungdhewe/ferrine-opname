package com.ferrine.stockopname.ui.scanneditem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.SummaryItem

class ScannedItemAdapter(
    private var items: List<SummaryItem>,
    private var viewType: Int = VIEW_TYPE_FASHION,
    private var qtyLabel: String = "Qty"
) : RecyclerView.Adapter<ScannedItemAdapter.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_FASHION = 0
        const val VIEW_TYPE_SIMPLE = 1
    }

    class ViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemId: TextView = view.findViewById(R.id.tvItemId)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)

        // These might be null in Simple view
        val tvArt: TextView? = if (viewType == VIEW_TYPE_FASHION) view.findViewById(R.id.tvArt) else null
        val tvMaterial: TextView? = if (viewType == VIEW_TYPE_FASHION) view.findViewById(R.id.tvMaterial) else null
        val tvColor: TextView? = if (viewType == VIEW_TYPE_FASHION) view.findViewById(R.id.tvColor) else null
        val tvStockQty: TextView? = if (viewType == VIEW_TYPE_FASHION) view.findViewById(R.id.tvStockQty) else null
        val tvPrintQty: TextView? = if (viewType == VIEW_TYPE_FASHION) view.findViewById(R.id.tvPrintQty) else null
    }

    override fun getItemViewType(position: Int): Int = viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_SIMPLE) R.layout.row_item_list_simple else R.layout.row_item_list_fashion
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvItemName.text = item.name
        holder.tvItemId.text = item.itemId
        holder.tvDescription.text = "" // Summary items might not have description, or hide it
        holder.tvCategory.text = if (viewType == VIEW_TYPE_SIMPLE) item.category else "Cat: ${item.category}"

        if (viewType == VIEW_TYPE_FASHION) {
            holder.tvArt?.text = "Art: ${item.article}"
            holder.tvMaterial?.text = "Mat: ${item.material}"
            holder.tvColor?.text = "Col: ${item.color}"
            holder.tvStockQty?.text = "$qtyLabel: ${item.totalQty}"
            holder.tvPrintQty?.visibility = View.GONE
        } else {
            // In simple view, we might need to show the QTY somewhere if not provided by layout
            // For now, let's append it to description or name if needed, but the prompt says:
            // SIMPLE -> itemId, name, category, QTY
            holder.tvDescription.text = "$qtyLabel: ${item.totalQty}"
            holder.tvDescription.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<SummaryItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    fun setViewType(viewType: Int) {
        this.viewType = viewType
        notifyDataSetChanged()
    }

    fun setQtyLabel(label: String) {
        this.qtyLabel = label
        notifyDataSetChanged()
    }
}
