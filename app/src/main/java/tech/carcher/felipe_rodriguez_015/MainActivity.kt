package tech.carcher.felipe_rodriguez_015

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tech.carcher.felipe_rodriguez_015.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val products = mutableListOf<Product>()
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(products)
        recyclerView.adapter = productAdapter

        loadProducts()
    }

    private fun loadProducts() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val savedProducts = sharedPref.getString("products", null)

        if (savedProducts != null) {
            val type = object : TypeToken<List<Product>>() {}.type
            products.addAll(gson.fromJson(savedProducts, type))
            productAdapter.updateProducts(products)
            Toast.makeText(this, "Productos cargados correctamente", Toast.LENGTH_SHORT).show()
        } else {
            fetchProductsFromApi()
        }
    }

    private fun fetchProductsFromApi() {
        val request = Request.Builder()
            .url("https://fakestoreapi.com/products")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Error fetching products", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    val type = object : TypeToken<List<Product>>() {}.type
                    val fetchedProducts: List<Product> = gson.fromJson(jsonString, type)

                    runOnUiThread {
                        products.clear()
                        products.addAll(fetchedProducts)
                        productAdapter.updateProducts(products)
                        saveProducts()
                        Toast.makeText(this@MainActivity, "Productos cargados correctamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveProducts() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("products", gson.toJson(products))
            apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_product -> {
                addRandomProduct()
                true
            }
            R.id.action_remove_product -> {
                removeLastProduct()
                true
            }
            R.id.action_show_chart -> {
                showTopRatedProductsChart()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addRandomProduct() {
        val newProduct = Product(
            id = products.size + 1,
            title = "Nuevo Producto ${products.size + 1}",
            price = (10..100).random().toDouble(),
            description = "Descripción del nuevo producto",
            category = "Categoría",
            image = "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg",
            rating = Rating(rate = (1..5).random().toDouble(), count = (10..100).random())
        )
        products.add(newProduct)
        productAdapter.updateProducts(products)
        saveProducts()
        Log.d("MainActivity", "Producto agregado: ${newProduct.title}")
    }

    private fun removeLastProduct() {
        if (products.isNotEmpty()) {
            val removedProduct = products.removeAt(products.size - 1)
            productAdapter.updateProducts(products)
            saveProducts()
            Log.d("MainActivity", "Producto eliminado: ${removedProduct.title}")
        }
    }

    private fun showTopRatedProductsChart() {
        val topProducts = products.sortedByDescending { it.rating.rate }.take(5)
        val entries = topProducts.mapIndexed { index, product ->
            BarEntry(index.toFloat(), product.rating.rate.toFloat())
        }

        val dataSet = BarDataSet(entries, "Top 5 Productos")
        val barData = BarData(dataSet)

        val chart = BarChart(this)
        chart.data = barData
        chart.setFitBars(true)
        chart.invalidate()

        setContentView(chart)
    }
}