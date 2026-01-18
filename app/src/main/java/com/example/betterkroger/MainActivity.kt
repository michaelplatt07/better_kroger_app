package com.example.betterkroger

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.betterkroger.models.ProductRes
import com.example.betterkroger.models.ProductSize
import com.example.betterkroger.models.ShoppingItem
import com.example.betterkroger.models.ShoppingList
import com.example.betterkroger.ui.theme.BetterKrogerTheme
import com.example.betterkroger.viewmodels.AppSettingsViewModel
import com.example.betterkroger.viewmodels.ListViewModel
import com.example.betterkroger.viewmodels.SearchViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File

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
            HomePage(modifier = modifier, onNavigateToSearch = {
                navController.navigate("search")
            }, onNavigateToViewList = {
                navController.navigate("viewList")
            }, onNavigateToSettings = {
                navController.navigate("settings")
            })
        }
        composable("viewList") {
            ViewList(
                modifier = modifier,
                onNavigateToHome = {
                    navController.navigate("home")
                }
            )
        }
        composable("search") {
            ProductSearch(
                modifier = modifier,
                onNavigateToHome = {
                    navController.navigate("home")
                },
            )
        }
        composable("settings") {
            SettingsView(
                modifier = modifier,
                onNavigateToHome = {
                    navController.navigate("home")
                },
            )
        }
    }
}

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onNavigateToViewList: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(10.dp)
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
        Button(onClick = {
            onNavigateToSettings()
        }) {
            Text(text = "Settings")
        }
    }
}

@Composable
fun SettingsView(
    modifier: Modifier = Modifier,
    appSettingsViewModel: AppSettingsViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
) {
    val apiUrl by appSettingsViewModel.apiUrl.collectAsState(initial = "")
    var updatedUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(apiUrl) {
        if (updatedUrl.isEmpty() && apiUrl.isNotEmpty()) {
            updatedUrl = apiUrl
        }
    }

    Column(
        modifier = modifier
            .padding(10.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row {
            Button(
                onClick = {
                    onNavigateToHome()
                }) {
                Text(
                    text = "Home"
                )
            }
        }
        Row {
            TextField(
                value = updatedUrl,
                onValueChange = { newUrl ->
                    updatedUrl = newUrl
                },
                singleLine = true,
            )
        }
        Row {
            Button(
                onClick = {
                    appSettingsViewModel.updateApiUrl(updatedUrl)
                }) {
                Text(
                    text = "Save"
                )
            }
        }
    }
}


@Composable
fun ViewList(
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit,
    listViewModel: ListViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        listViewModel.loadShoppingItems()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row() {
            Button(onClick = { onNavigateToHome() }) {
                Text(text = "Home")
            }
            Button(onClick = { listViewModel.clearChecked() }) {
                Text(text = "Clear Checked")
            }
        }
        listViewModel.groupedShoppingItems.forEach { (aisleGroup, items) ->
            ShoppingListItemView(modifier, aisleGrouping = aisleGroup, items)
        }
    }
}

@Composable
fun ListItemView(
    modifier: Modifier = Modifier,
    shoppingItem: ShoppingItem,
    listViewModel: ListViewModel = viewModel()
) {
    val swipeState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.7f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    listViewModel.updateCheckedStatus(
                        shoppingItem, true
                    )
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    listViewModel.clearItem(
                        shoppingItem.productId
                    )
                    false
                }

                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {},
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (shoppingItem.checked) Modifier.background(
                        color = Color(
                            120, 117, 117
                        )
                    ) else Modifier.background(color = Color(209, 204, 203))
                )
        ) {
            Row {
                Checkbox(
                    shoppingItem.checked, onCheckedChange = {
                        listViewModel.updateCheckedStatus(
                            shoppingItem, it
                        )
                    })
                Text(
                    modifier = Modifier.padding(8.dp),
                    color = Color(0, 0, 0),
                    text = "Qty. ${shoppingItem.quantity} - ${shoppingItem.size} - ${shoppingItem.productDescription}",
                )
            }
        }
    }
}

@Composable
fun ShoppingListItemView(
    modifier: Modifier = Modifier,
    aisleGrouping: String,
    shoppingItems: List<ShoppingItem>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(43, 125, 251))
    ) {
        Text(
            text = aisleGrouping, modifier = Modifier.padding(24.dp)
        )
    }
    shoppingItems.forEach { shoppingItem ->
        ListItemView(modifier, shoppingItem)
    }
}

// TODO(map) Hoist to better place to write file
@Composable
fun WriteFileContents(
    shoppingItem: ShoppingItem, listViewModel: ListViewModel
) {
    val context = LocalContext.current

    Button(onClick = {
        listViewModel.addItemToList(shoppingItem)
        val productName = shoppingItem.productDescription
        // TODO(map) There might be a better way to do this with alert
        Toast.makeText(context, "$productName added.", Toast.LENGTH_SHORT).show()
    }) {
        Text(text = "Add")
    }
}


@Composable
fun ProductSearch(
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit,
    searchViewModel: SearchViewModel = viewModel(),
    listViewModel: ListViewModel = viewModel(),
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var productRes by remember { mutableStateOf<ProductRes?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val apiUrl = appSettingsViewModel.apiUrl.collectAsState(initial = "")

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),

        ) {
        TextField(
            value = text,
            onValueChange = { newText ->
                text = newText
            },
            placeholder = { Text(text = "Type Product Name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    scope.launch {
                        loading = true
                        productRes = searchViewModel.searchForProduct(apiUrl.value, text)
                        loading = false
                    }
                }),
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                    }
                }
            }
        )
        Button(
            onClick = {
                onNavigateToHome()
            }) {
            Text(
                text = "Home"
            )
        }
        Button(
            onClick = {
                keyboardController?.hide()
                scope.launch {
                    loading = true
                    productRes = searchViewModel.searchForProduct(apiUrl.value, text)
                    loading = false
                }
            }) {
            Text(
                text = "Search"
            )
        }
        if (loading) {
            Text(
                text = "Loading..."
            )
        } else {
            productRes?.data?.forEach { product ->
                var thumbnailSize: ProductSize? = null
                var productAisleNumber: String = "No Aisle Found"
                var productAisleDescription: String = "No Aisle Description Found"
                for (productImage in product.images) {
                    if (productImage.perspective == "front") {
                        // TODO(map) What should I do to ensure that we always get an image? And if no image is present,
                        // should we ensure that there is a default?
                        if (productImage.sizes.size >= 1) {
                            thumbnailSize = productImage.sizes.get(0)
                        }
                        // for (productSize in productImage.sizes) {
                        //     if (productSize.size == "thumbnail") {
                        //         thumbnailSize = productSize
                        //         break
                        //     }
                        // }
                    }
                }
                if (product.aisleLocations.size > 0) {
                    productAisleNumber = product.aisleLocations[0].number
                    productAisleDescription = product.aisleLocations[0].description
                }
                // TODO(map) Figure out how to manage quantity here.
                if (thumbnailSize != null) {
                    ItemPreview(
                        productId = product.productId,
                        productBrand = product.brand,
                        productAisleNumber = productAisleNumber,
                        productAisleDescription = productAisleDescription,
                        productUrl = thumbnailSize.url,
                        productDescription = product.description,
                        productSize = product.items.get(0)?.size,
                        productPrice = product.items.get(0)?.price?.regular,
                        modifier = modifier,
                        listViewModel = listViewModel
                    )
                }
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
    productSize: String?,
    productPrice: Number?,
    modifier: Modifier = Modifier,
    listViewModel: ListViewModel,
) {
    Log.d("ItemPreview", "Product IDs: ${listViewModel.shoppingItems}")
    val itemIndex = listViewModel.shoppingItems.indexOfFirst { it.productId == productId }
    Log.d("ItemPreview", "Product: $productId in cart ${itemIndex != -1}")
    if (itemIndex != -1) {
        Card(modifier = modifier) {
            Row {
                Column {
                    Row {
                        IncreaseQuantityButton(productId, listViewModel = listViewModel)
                        Text(
                            text = listViewModel.shoppingItems[itemIndex].quantity.toString(),
                            modifier = Modifier.padding(8.dp),
                        )
                        DecreaseQuantityButton(productId, listViewModel = listViewModel)
                    }
                }
                Column {
                    Text(
                        text = "$${productPrice}",
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = productAisleDescription,
                        modifier = Modifier.padding(8.dp),
                    )
                    AsyncImage(
                        model = productUrl,
                        contentDescription = productDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                    Text(
                        text = productSize ?: "",
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = productDescription,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }

    } else {
        var shoppingItem = ShoppingItem(
            productId = productId,
            brand = productBrand,
            productDescription = productDescription,
            aisleDescription = productAisleDescription,
            aisleNumber = productAisleNumber,
            size = productSize ?: "",
        )
        Card(modifier = modifier) {
            Row {
                Column {
                    WriteFileContents(shoppingItem, listViewModel)
                }
                Column {
                    Text(
                        text = "$${productPrice}",
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = productAisleDescription,
                        modifier = Modifier.padding(8.dp),
                    )
                    AsyncImage(
                        model = productUrl,
                        contentDescription = productDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                    Text(
                        text = productSize ?: "",
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = productDescription,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun IncreaseQuantityButton(
    productId: String, listViewModel: ListViewModel
) {
    Button(
        onClick = { listViewModel.increaseQuantity(productId) }) {
        Icon(
            Icons.Default.KeyboardArrowUp,
            contentDescription = "Increase",
        )
    }

}

@Composable
fun DecreaseQuantityButton(
    productId: String, listViewModel: ListViewModel
) {
    Button(
        onClick = { listViewModel.decreaseQuantity(productId) }) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = "Decrease",
        )
    }
}
