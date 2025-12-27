package com.example.betterkroger.models

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

data class Product(
    val productId: String,
    val productPageURI: String,
    val aisleLocations: List<ProductAisleLocation>,
    val brand: String,
    val categories: List<String>,
    val description: String,
    val images: List<ProductImage>,
)

data class ProductRes(
    val	data: List<Product>,
)

data class ShoppingItem(
    val productId: String,
    val brand: String,
    val productDescription: String,
    val aisleDescription: String,
    val aisleNumber: String,
)

data class ShoppingList(
    var items: MutableList<ShoppingItem>,
)
