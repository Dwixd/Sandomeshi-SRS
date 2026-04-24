package com.example.deckmaking

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SubtitleApiService {
    // 2. Tambahkan jembatan utama untuk menarik ribuan daftar anime
    @GET("api/anime")
    suspend fun getAllAnime(): List<AnimeEntry>

    @GET("api/anime/files")
    suspend fun getAnimeFiles(
        @Query("type") type: String,
        @Query("title") title: String
    ): List<String>

    @GET("download/{folder}/{title}/{filename}")
    suspend fun downloadSubtitle(
        @Path("folder") folder: String,
        @Path("title") title: String,
        @Path(value = "filename", encoded = true) filename: String
    ): ResponseBody
}

object SubtitleClient {
    private const val BASE_URL = "https://api.sandomeshi.site/"

    val service: SubtitleApiService by lazy {
        // CCTV Retrofit
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SubtitleApiService::class.java)
    }
}