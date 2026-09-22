package com.example.cpen321application.network

import com.example.cpen321application.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class ServerIpResponse(val ip: String)
data class ServerTimeResponse(val time: String)
data class NameResponse(val firstName: String, val lastName: String)
data class ClientIpResponse(val ip: String)

interface ApiService {
    @GET("/api/server-ip")
    suspend fun getServerIp(): ServerIpResponse

    @GET("/api/server-time")
    suspend fun getServerTime(): ServerTimeResponse

    @GET("/api/name")
    suspend fun getName(): NameResponse

    @GET("/api/client-ip")
    suspend fun getClientIp(): ClientIpResponse
}

object ApiClient {
    val service: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" })
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
