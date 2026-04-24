package com.example.deckmaking

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class JikanResponse(val data: List<AnimeData>)

data class AnimeData(
    val mal_id: Int,
    val title: String,
    val images: AnimeImages
)

data class AnimeImages(val jpg: JpgImage)

data class JpgImage(val image_url: String)

interface JikanApiService {
    @GET("v4/seasons/now")
    suspend fun getNowPlaying(): JikanResponse
}

object JikanClient {
    private const val BASE_URL = "https://api.jikan.moe/"

    val service: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JikanApiService::class.java)
    }
}
