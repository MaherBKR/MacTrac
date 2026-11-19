package com.macrotracker.data.remote

import com.macrotracker.data.local.entity.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class OffResponse(
    val status: Int = 0,
    val product: OffProduct? = null
)

@Serializable
private data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: OffNutriments? = null
)

@Serializable
private data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null
)

@Singleton
class OpenFoodFactsClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json
) {
    suspend fun fetchFoodByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MacroTrackerAndroid/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val parsed = json.decodeFromString<OffResponse>(body)
                if (parsed.status != 1 || parsed.product == null) return@withContext null

                val product = parsed.product
                val nutriments = product.nutriments
                val name = product.productName?.takeIf { it.isNotBlank() } ?: return@withContext null

                FoodItem(
                    name = name.trim(),
                    brand = product.brands?.trim()?.takeIf { it.isNotBlank() },
                    barcode = barcode,
                    kcalPer100g = nutriments?.energyKcal100g ?: 0.0,
                    proteinPer100g = nutriments?.proteins100g ?: 0.0,
                    carbsPer100g = nutriments?.carbohydrates100g ?: 0.0,
                    fatPer100g = nutriments?.fat100g ?: 0.0,
                    defaultBaseUnit = "g",
                    isBuiltIn = false
                )
            }
        }.getOrNull()
    }
}
