package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class CoinbaseProductDto(
    @Json(name = "id") val id: String,
    @Json(name = "base_currency") val baseCurrency: String,
    @Json(name = "quote_currency") val quoteCurrency: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "status") val status: String?
)

interface CoinbaseApiService {
    @GET("products")
    suspend fun getProducts(): List<CoinbaseProductDto>

    @GET("products/{product_id}/candles")
    suspend fun getCandles(
        @Path("product_id") productId: String,
        @Query("granularity") granularity: Int,
        @Query("start") startIso: String? = null,
        @Query("end") endIso: String? = null
    ): List<List<Double>>
}

object CoinbaseClient {
    private const val BASE_URL = "https://api.exchange.coinbase.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "BTC-Response-Scanner-Android/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: CoinbaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CoinbaseApiService::class.java)
    }
}
