package com.example.neurocardiac

import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class PredictionResponse(
    val prediction: String,
    val confidence: Double,
    val filename: String? = null
)

data class HeartDiseaseRequest(
    val Age: Int,
    val Sex: String,
    val ChestPain: String,
    val RestingBP: Int,
    val Cholesterol: Int,
    val FastingBS: String,
    val MaxHR: Int,
    val ExAngina: String,
    val Oldpeak: Double,
    val ST_Slope: String
)

interface AiModelApi {
    @Multipart
    @POST(value = "predict/image")
    suspend fun predictImage(@Part file: MultipartBody.Part): PredictionResponse

    @POST(value = "predict/heart")
    suspend fun predictHeart(@Body request: HeartDiseaseRequest): PredictionResponse
}
object RetrofitClient {
    // Configured via NEUROCARDIAC_BASE_URL in local.properties or gradle.properties.
    //   Android Emulator -> "http://10.0.2.2:8000/"
    //   Physical Phone   -> "http://<YOUR_PC_IPV4>:8000/"
    // Defaults to the emulator loopback address if unset. See README.
    private val BASE_URL = BuildConfig.BASE_URL

    val apiService: AiModelApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiModelApi::class.java)
    }
}