package tech.carcher.felipe_rodriguez_015

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private var products: List<Product>,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewProduct)
        val nameTextView: TextView = view.findViewById(R.id.textViewName)
        val priceTextView: TextView = view.findViewById(R.id.textViewPrice)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBarProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.nameTextView.text = product.title
        holder.priceTextView.text = "$${product.price}"
        holder.ratingBar.rating = product.rating.rate.toFloat()

        Glide.with(holder.itemView.context)
            .load(product.image)
            .into(holder.imageView)

        holder.itemView.setOnLongClickListener {
            onItemLongClick(position)
            true
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}