package com.example.betterkroger.models

import androidx.datastore.preferences.core.stringPreferencesKey


// Settings
object AppSettings {
    val API_URL = stringPreferencesKey("api_url")
}

// Search results models
data class ProductAisleLocation(
    val bayNumber: String,
    val description: String,
    val number: String,
    val numberOfFacings: String,
    val sequenceNumber: String,
    val side: String,
    val shelfNumber: String,
    val shelfPositionInBay: String,
)

data class ProductSize(
    val id: String,
    val size: String,
    val url: String,
)

data class ProductImage(
    val id: String,
    val perspective: String,
    val default: Boolean,
    val sizes: List<ProductSize>,
)

data class ProductPrice(
    val regular: Number
)

data class ProductInfo(
    val size: String,
    val price: ProductPrice
)

data class Product(
    val productId: String,
    val productPageURI: String,
    val aisleLocations: List<ProductAisleLocation>,
    val brand: String,
    val categories: List<String>,
    val description: String,
    val items: List<ProductInfo>,
    val images: List<ProductImage>,
)

data class ProductRes(
    val data: List<Product>? = null,
    val error: String?,
)

// Models for tracking data in the app
data class ShoppingItem(
    val productId: String,
    val brand: String,
    val productDescription: String,
    val aisleDescription: String,
    val aisleNumber: String,
    var quantity: Int = 1,
    // TODO(map) REMOVE ME: After the next iteration of the list is done this should be removed to not allow null
    var size: String? = "",
    var checked: Boolean = false,
)

data class ShoppingList(
    var items: MutableList<ShoppingItem>,
)

data class GroupedShoppingList(
    var items: MutableMap<String, MutableList<ShoppingItem>>,
) 
