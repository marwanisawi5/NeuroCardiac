package com.example.neurocardiac

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val imageURL: String? = null,
    val isFromUser: Boolean,
    val modelType: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query(value = "SELECT * FROM chat_messages WHERE modelType = 'BrainTumor' ORDER BY timestamp ASC")
    fun getBrainTumorHistory(): Flow<List<ChatMessage>>

    @Query(value = "SELECT * FROM chat_messages WHERE modelType = 'HeartDisease' ORDER BY timestamp ASC")
    fun getHeartDiseaseHistory(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)
}

class AIChatViewModel(
    private val chatDao: ChatDao
) : ViewModel() {
    val brainTumorChat: StateFlow<List<ChatMessage>> = chatDao.getBrainTumorHistory()
        .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())

    val heartDiseaseChat: StateFlow<List<ChatMessage>> = chatDao.getHeartDiseaseHistory()
        .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())

    fun validateInputs(age: String, restingBP: String, cholesterol: String, maxHR: String): Boolean {
        val ageInt = age.toIntOrNull() ?: return false
        val bpInt = restingBP.toIntOrNull() ?: return false
        val cholInt = cholesterol.toIntOrNull() ?: return false
        val hrInt = maxHR.toIntOrNull() ?: return false

        if (ageInt !in 1..120) return false
        if (bpInt !in 0..300) return false
        if (cholInt !in 0..600) return false
        if (hrInt !in 60..220) return false
        return true
    }
    fun sendBrainTumorMessage(context: Context, imageUri: Uri?) {
        if (imageUri == null) return

        viewModelScope.launch {
            val permanentImageUrl = saveImageToInternalStorage(context, imageUri)
            val userMsg = ChatMessage(
                text = "Scan uploaded for analysis.",
                imageURL = permanentImageUrl,
                isFromUser = true,
                modelType = "BrainTumor"
            )
            chatDao.insertMessage(userMsg)

            try {
                val filePart = prepareFilePart(context, imageUri)
                if (filePart != null) {
                    val response = RetrofitClient.apiService.predictImage(filePart)
                    val aiReplyText = "Diagnosis: ${response.prediction}\nConfidence: ${response.confidence}%"
                    chatDao.insertMessage(ChatMessage(text = aiReplyText, isFromUser = false, modelType = "BrainTumor"))
                } else {
                    chatDao.insertMessage(ChatMessage(text = "Error: Could not read image file.", isFromUser = false, modelType = "BrainTumor"))
                }
            } catch (e: Exception) {
                chatDao.insertMessage(ChatMessage(text = "Network Error: ${e.message}", isFromUser = false, modelType = "BrainTumor"))
            }
        }
    }

    fun sendHeartClinicalData(context: Context, request: HeartDiseaseRequest) {
        viewModelScope.launch {
            val summary = "Heart Clinical Form Submitted:\nAge: ${request.Age} | Sex: ${request.Sex} | BP: ${request.RestingBP} | MaxHR: ${request.MaxHR}"
            chatDao.insertMessage(ChatMessage(text = summary, isFromUser = true, modelType = "HeartDisease"))

            try {
                val response = RetrofitClient.apiService.predictHeart(request)
                val aiRepleyText = "Cardiology Assessment:\nDiagnosis: ${response.prediction}"
                chatDao.insertMessage(ChatMessage(text = aiRepleyText, isFromUser = false, modelType = "HeartDisease"))
            } catch (e: Exception) {
                chatDao.insertMessage(ChatMessage(text = "Network Error: ${e.message}", isFromUser = false, modelType = "HeartDisease"))
            }
        }
    }

    private fun prepareFilePart(context: Context, uri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return null
            val requestBody = bytes.toRequestBody(contentType = "image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(name = "file", filename = "scan.jpg", requestBody)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "scan_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(context.filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}