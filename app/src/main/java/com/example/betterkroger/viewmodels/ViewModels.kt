package com.example.betterkroger.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterkroger.models.GroupedShoppingList
import com.example.betterkroger.models.ProductRes
import com.example.betterkroger.models.ShoppingItem
import com.example.betterkroger.models.ShoppingList
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO(map) Consolidate and combine with other instance in MainActivity.kt
private val fileName = "shopping_list.json"

private val client = HttpClient(OkHttp) {
    expectSuccess = false
}

class SearchViewModel(
) : ViewModel() {
    suspend fun searchForProduct(productTerm: String): ProductRes {
        try {
            val encodedParam = Uri.encode(productTerm)
            print("Using product term $encodedParam")
            val response: HttpResponse =
                client.get("http://10.0.2.2:8080/search?item=$encodedParam")

            if (response.status.value == 200) {
                var response_body = response.bodyAsText()
                print("Response $response_body")
                val gson = Gson()
                var productRes = gson.fromJson(response_body, ProductRes::class.java)
                return productRes
            } else {
                return ProductRes(error = "Error: ${response.status}")
            }
        } catch (e: Exception) {
            return ProductRes(error = "Network error: ${e.message}")
        }
    }
}

class ListViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private var saveJob: Job? = null

    var shoppingItems = mutableStateListOf<ShoppingItem>()
    val groupedShoppingItems by derivedStateOf { shoppingItems.groupBy { it.aisleDescription } }

    init {
        loadShoppingItems()
    }

    fun loadShoppingItems() {
        Log.d("ListViewModel", "Loading shopping items...")
        if (shoppingItems.isNotEmpty()) {
            Log.d("ListViewModel", "Items aready loaded...")
            return
        }
        val gson = Gson()
        val shoppingListContents =
            context.openFileInput(fileName).bufferedReader().use { it.readText() }
        var shoppingList: ShoppingList =
            gson.fromJson(shoppingListContents, ShoppingList::class.java)
        shoppingItems.clear()
        shoppingItems.addAll(shoppingList.items)
    }

    fun addItemToList(shoppingItem: ShoppingItem) {
        shoppingItems.add(shoppingItem)
        scheduleSave(shoppingItems)
    }

    fun clearChecked() {
        Log.d("ListViewModel", "Clearing checked items")
        shoppingItems.removeAll { it.checked == true }
        scheduleSave(shoppingItems)
    }

    fun updateCheckedStatus(shoppingListItem: ShoppingItem, checked: Boolean) {
        val index = shoppingItems.indexOfFirst {
            it.productId == shoppingListItem.productId
        }
        Log.d("ListViewModel", "Index = $index")
        if (index != -1) {
            shoppingItems[index] = shoppingItems[index].copy(checked = checked)
            scheduleSave(shoppingItems)
        }
    }

    fun increaseQuantity(productId: String) {
        val index = shoppingItems.indexOfFirst {
            it.productId == productId
        }
        if (index != -1) {
            val oldQuant = shoppingItems[index].quantity
            shoppingItems[index] = shoppingItems[index].copy(quantity = oldQuant + 1)
            scheduleSave(shoppingItems)
        }
    }

    fun decreaseQuantity(productId: String) {
        val index = shoppingItems.indexOfFirst {
            it.productId == productId
        }
        Log.d("REMOVE ME", "Index = $index")
        if (index != -1) {
            val oldQuant = shoppingItems[index].quantity
            if (oldQuant - 1 <= 0) {
                Log.d("REMOVE ME ", "Removing item at index $index")
                shoppingItems.removeAt(index)
            } else {
                shoppingItems[index] = shoppingItems[index].copy(quantity = oldQuant - 1)
            }
            scheduleSave(shoppingItems)
        }
    }

    private fun scheduleSave(shoppingItems: MutableList<ShoppingItem>) {
        // Cancel previous scheduled save if it exists
        saveJob?.cancel()

        saveJob = viewModelScope.launch {
            delay(1000L) // 1-second debounce
            saveToFile(shoppingItems)
        }
    }

    private fun saveToFile(shoppingItems: MutableList<ShoppingItem>) {
        val gson = Gson()
        val shoppingList = ShoppingList(items = shoppingItems)
        val jsonString = gson.toJson(shoppingList)

        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }
    }
}
