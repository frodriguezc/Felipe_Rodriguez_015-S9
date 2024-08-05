package tech.carcher.felipe_rodriguez_015

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
        productAdapter = ProductAdapter(products) { position ->
            showContextMenu(position)
        }
        recyclerView.adapter = productAdapter

        loadProducts()
    }

    private fun showContextMenu(position: Int) {
        val popup = PopupMenu(this, recyclerView.getChildAt(position))
        popup.menuInflater.inflate(R.menu.context_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_product -> {
                    removeProduct(position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun removeProduct(position: Int) {
        if (position < products.size) {
            val removedProduct = products.removeAt(position)
            productAdapter.updateProducts(products)
            saveProducts()
            Log.d("MainActivity", getString(R.string.producto_eliminado, removedProduct.title))
            Toast.makeText(this, getString(R.string.producto_eliminado, removedProduct.title), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.title) {
            getString(R.string.eliminar_producto) -> {
                removeProduct(item.itemId)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun loadProducts() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val savedProducts = sharedPref.getString("products", null)

        if (savedProducts != null) {
            val type = object : TypeToken<List<Product>>() {}.type
            products.addAll(gson.fromJson(savedProducts, type))
            productAdapter.updateProducts(products)
            Toast.makeText(this,
                getString(R.string.productos_cargados_correctamente), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@MainActivity,
                        getString(R.string.error_al_cargar_productos), Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@MainActivity, getString(R.string.productos_cargados_correctamente), Toast.LENGTH_SHORT).show()
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
            R.id.action_show_chart -> {
                showTopRatedProductsChart()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun addRandomProduct() {
        val randomId = (1..20).random()
        val request = Request.Builder()
            .url("https://fakestoreapi.com/products/$randomId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Error fetching random product", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        getString(R.string.error_al_agregar_producto), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    val newProduct: Product = gson.fromJson(jsonString, Product::class.java)

                    runOnUiThread {
                        products.add(newProduct)
                        productAdapter.updateProducts(products)
                        saveProducts()
                        Log.d("MainActivity",
                            getString(R.string.producto_agregado, newProduct.title))
                        Toast.makeText(this@MainActivity, getString(R.string.producto_agregado, newProduct.title), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showTopRatedProductsChart() {
        val topProducts = products.sortedByDescending { it.rating.rate }.take(5)
        val entries = topProducts.mapIndexed { index, product ->
            BarEntry((topProducts.size - 1 - index).toFloat(), product.rating.rate.toFloat())
        }

        val dataSet = BarDataSet(entries, getString(R.string.calificaci_n))
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)

        val barData = BarData(dataSet)
        barData.setValueTextSize(35f)
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.1f", value)
            }
        })

        val chart = HorizontalBarChart(this)
        chart.data = barData
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 5f
        leftAxis.setDrawLabels(true)
        leftAxis.setDrawAxisLine(true)
        leftAxis.setDrawGridLines(true)

        val rightAxis = chart.axisRight
        rightAxis.setDrawAxisLine(true)
        rightAxis.setDrawGridLines(true)
        rightAxis.setDrawLabels(true)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(true)
        xAxis.granularity = 1f
        xAxis.labelCount = topProducts.size
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index in topProducts.indices) {
                    val product = topProducts[index]
                    return truncateString(product.title, 30)
                }
                return ""
            }
        }

        chart.animateY(1000)

        chart.extraLeftOffset = 35f
        chart.extraRightOffset = 20f
        chart.extraTopOffset = 10f
        chart.extraBottomOffset = 10f

        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)

        setContentView(chart)
    }

    private fun truncateString(str: String, length: Int): String {
        return if (str.length > length) str.substring(0, length) + "..." else str
    }
}