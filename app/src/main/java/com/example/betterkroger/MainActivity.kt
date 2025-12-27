package com.example.betterkroger

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.example.betterkroger.models.Product
import com.example.betterkroger.models.ProductAisleLocation
import com.example.betterkroger.models.ProductImage
import com.example.betterkroger.models.ProductRes
import com.example.betterkroger.models.ProductSize
import com.example.betterkroger.models.ShoppingItem
import com.example.betterkroger.models.ShoppingList
import com.example.betterkroger.ui.theme.BetterKrogerTheme
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.io.File

private val client = HttpClient()
private val fileName = "shopping_list.json"
private val settingsFile = "settings.json"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureListFileExists(this, fileName)

        enableEdgeToEdge()
        setContent {
            BetterKrogerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BetterKrogerApp(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

fun ensureListFileExists(context: Context, fileName: String) {
    val file = File(context.getFilesDir(), fileName)

    if (!file.exists()) {
        // TODO(map) Move gson to the top level and pass an instance around
        val gson = Gson()
        val shoppingList = ShoppingList(items = mutableListOf())
        val jsonString = gson.toJson(shoppingList)

        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }

    }
}

@Composable
fun BetterKrogerApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomePage(
                modifier = modifier,
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToViewList = {
                    navController.navigate("viewList")
                },
            )
        }
        composable("viewList") {
            ViewList(
                modifier = modifier,
            )
        }
        composable("search") {
            ProductSearch(
                modifier = modifier,
            )
        }
    }
}

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onNavigateToViewList: () -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        Button(onClick = {
            onNavigateToViewList()
        }) {
            Text(text = "View List")
        }
        Button(onClick = {
            onNavigateToSearch()
        }) {
            Text(text = "Search")
        }
    }
}

@Composable
fun ViewList(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
    }
}

@Composable
fun WriteFileContents(
    fileName: String,
    shoppingItem: ShoppingItem,
) {
    val context = LocalContext.current

    Button(onClick = {
        val gson = Gson()

        val shoppingListContents =
            context.openFileInput(fileName).bufferedReader().use { it.readText() }
        var shoppingList: ShoppingList =
            gson.fromJson(shoppingListContents, ShoppingList::class.java)
        shoppingList.items.add(shoppingItem)
        val jsonString = gson.toJson(shoppingList)

        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }
    }) {
        Text(text = "Add")
    }
}

@Composable
fun ProductSearch(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("Loading...") }
    var productRes by remember { mutableStateOf<ProductRes?>(null) }
    LaunchedEffect(Unit) {
        try {
            val response: HttpResponse = client.get("http://10.0.2.2:8080/search?item=ground+pork")

            if (response.status.value == 200) {
                var response_body = response.bodyAsText()
                val gson = Gson()
                productRes = gson.fromJson(response_body, ProductRes::class.java)
                // text = productRes?.data?.get(0).description
            } else {
                text = "Error: ${response.status}"
            }
        } catch (e: Exception) {
            text = "Network error: ${e.message}"
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        productRes?.data?.forEach { product ->
            var thumbnailSize: ProductSize? = null
            var productAisleNumber: String = "No Aisle Found"
            var productAisleDescription: String = "No Aisle Description Found"
            for (productImage in product.images) {
                if (productImage.perspective == "front") {
                    for (productSize in productImage.sizes) {
                        if (productSize.size == "thumbnail") {
                            thumbnailSize = productSize
                            break
                        }
                    }
                }
            }
            if (product.aisleLocations.size > 0) {
                productAisleNumber = product.aisleLocations[0].number
                productAisleDescription = product.aisleLocations[0].description
            }
            if (thumbnailSize != null) {
                ItemPreview(
                    productId = product.productId,
                    productBrand = product.brand,
                    productAisleNumber = productAisleNumber,
                    productAisleDescription = productAisleDescription,
                    productUrl = thumbnailSize.url,
                    productDescription = product.description,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun ItemPreview(
    productId: String,
    productBrand: String,
    productAisleNumber: String,
    productAisleDescription: String,
    productUrl: String,
    productDescription: String,
    modifier: Modifier = Modifier,
) {
    var shoppingItem = ShoppingItem(
        productId = productId,
        brand = productBrand,
        productDescription = productDescription,
        aisleDescription = productAisleDescription,
        aisleNumber = productAisleNumber,
    )
    Card(modifier = modifier) {
        Row {
            Column {
                WriteFileContents(fileName, shoppingItem)
            }
            Column {
                Text(
                    text = productId,
                    modifier =
                        Modifier
                            .padding(8.dp),
                )
                Text(
                    text = productAisleDescription,
                    modifier =
                        Modifier
                            .padding(8.dp),
                )
                AsyncImage(
                    model = productUrl,
                    contentDescription = productDescription,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                )
                Text(
                    text = productDescription,
                    modifier =
                        Modifier
                            .padding(8.dp),
                )
            }
        }
    }
}
