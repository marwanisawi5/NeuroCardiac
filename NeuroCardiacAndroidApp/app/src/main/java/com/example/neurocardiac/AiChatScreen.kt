package com.example.neurocardiac

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

private fun dpToDpPadding(dpValue: Int) = Arrangement.spacedBy(dpValue.dp)

enum class ModelType { BrainTumor, HeartDisease }

@Composable
fun AIChatScreen(viewModel: AIChatViewModel) {
    val context = LocalContext.current
    var selectedModel by remember { mutableStateOf(value = ModelType.BrainTumor) }

    val brainChatHistory by viewModel.brainTumorChat.collectAsState()
    val heartChatHistory by viewModel.heartDiseaseChat.collectAsState()

    var stagedImageUri by remember { mutableStateOf<Uri?>(value = null) }
    var showHeartFormDialog by remember { mutableStateOf(value = false) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) stagedImageUri = uri }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (!success) stagedImageUri = null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        ModelToggleButton(
            selectedMode = selectedModel,
            onModeChange = { selectedModel = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        )
        if (selectedModel == ModelType.BrainTumor) {
            BrainTumorChat(
                chatHistory = brainChatHistory,
                stagedImageUri = stagedImageUri,
                onClearImage = { stagedImageUri = null },
                onGalleryClick = {
                    pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onCameraClickable = {
                    val tempFile = File.createTempFile("scan_capture_", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    stagedImageUri = uri
                    takePictureLauncher.launch(input = uri)
                },
                onSend = {
                    viewModel.sendBrainTumorMessage(context, stagedImageUri)
                    stagedImageUri = null
                }
            )
        } else {
            HeartDiseaseChat(
                chatHistory = heartChatHistory,
                onOpenHeartForm = { showHeartFormDialog = true }
            )
        }
    }

    if (showHeartFormDialog) {
        HeartDiseaseFormDialog(
            onDismiss = { showHeartFormDialog = false },
            onSubmit = { request ->
                viewModel.sendHeartClinicalData(context, request)
                showHeartFormDialog = false
            },
            onValidate = { age, bp, chol, hr ->
                viewModel.validateInputs(age, restingBP = bp, cholesterol = chol, maxHR = hr)
            }
        )
    }
}

@Composable
fun ModelToggleButton(
    selectedMode: ModelType,
    onModeChange: (ModelType) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    outerPadding: Dp = 4.dp,
    toggleAnimationDuration: Int = 300,
    containerColor: Color = Color.White,
    indicatorColor: Color = Color(0xFF6E9628),
    borderColor: Color = Color(0xFF6E9628)
) {
    val outerShape = RoundedCornerShape(size = height / 2)
    val indicatorShape = RoundedCornerShape(size = (height - (outerPadding * 2)) / 2)

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(outerShape)
            .background(containerColor)
            .border(width = 1.dp, borderColor, outerShape)
            .padding(all = outerPadding)
    ) {
        val indicatorWidth = maxWidth / 2
        val targetOffset by animateDpAsState(
            targetValue = if (selectedMode == ModelType.BrainTumor) 0.dp else indicatorWidth,
            animationSpec = tween(durationMillis = toggleAnimationDuration),
            label = "indicatorOffset"
        )

        val brainTextColor by animateColorAsState(
            targetValue = if (selectedMode == ModelType.BrainTumor) containerColor else indicatorColor,
            animationSpec = tween(durationMillis = toggleAnimationDuration),
            label = "brainTextColor"
        )
        val heartTextColor by animateColorAsState(
            targetValue = if (selectedMode == ModelType.HeartDisease) containerColor else indicatorColor,
            animationSpec = tween(durationMillis = toggleAnimationDuration),
            label = "heartTextColor"
        )

        Box(
            modifier = Modifier
                .offset(x = targetOffset)
                .fillMaxHeight()
                .width(indicatorWidth)
                .clip(indicatorShape)
                .background(indicatorColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(ModelType.BrainTumor) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Brain Tumor",
                    color = brainTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(ModelType.HeartDisease) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Heart Disease",
                    color = heartTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BrainTumorChat(
    chatHistory: List<ChatMessage>,
    stagedImageUri: Uri?,
    onClearImage: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClickable: () -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(chatHistory.reversed()) { message ->
                MessageBubble(message)
            }
        }
        stagedImageUri?.let { uri ->
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .size(120.dp)
                    .clip(shape = RoundedCornerShape(size = 8.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(uri).crossfade(enable = true).build(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onClearImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Remove",
                        tint = Color.White
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onGalleryClick) {
                Icon(
                    painterResource(id = R.drawable.outline_image_24),
                    contentDescription = "Attach from Gallery"
                )
            }
            IconButton(onClick = onCameraClickable) {
                Icon(
                    painterResource(id = R.drawable.outline_photo_camera_24),
                    contentDescription = "Take Photo"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onSend,
                enabled = stagedImageUri != null
            ) {
                Icon(
                    painterResource(id = R.drawable.baseline_send_24),
                    contentDescription = "Send Image"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze")
            }
        }
    }
}

@Composable
fun HeartDiseaseChat(
    chatHistory: List<ChatMessage>,
    onOpenHeartForm: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(chatHistory.reversed()) { message ->
                MessageBubble(message)
            }
        }
        Button(
            onClick = onOpenHeartForm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Enter Clinical Heart Data")
        }
    }
}

@Composable
fun HeartDiseaseFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (HeartDiseaseRequest) -> Unit,
    onValidate: (String, String, String, String) -> Boolean
) {
    val context = LocalContext.current
    var age by remember { mutableStateOf(value = "54") }
    var sex by remember { mutableStateOf(value = "M") }
    var chestPain by remember { mutableStateOf(value = "ASY") }
    var restingBP by remember { mutableStateOf(value = "130") }
    var cholesterol by remember { mutableStateOf(value = "240") }
    var fastingBS by remember { mutableStateOf(value = "No") }
    var maxHR by remember { mutableStateOf(value = "150") }
    var exAngina by remember { mutableStateOf(value = "No") }
    var oldpeak by remember { mutableStateOf(value = "1.0") }
    var stSlope by remember { mutableStateOf(value = "Flat") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clinical Data Input") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = dpToDpPadding(dpValue = 4)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") })
                OutlinedTextField(
                    value = sex,
                    onValueChange = { sex = it },
                    label = { Text("Sex (M/F)") })
                OutlinedTextField(
                    value = chestPain,
                    onValueChange = { chestPain = it },
                    label = { Text("Chest Pain (ASY/NAP/ATA/TA)") })
                OutlinedTextField(
                    value = restingBP,
                    onValueChange = { restingBP = it },
                    label = { Text("Resting BP") })
                OutlinedTextField(
                    value = cholesterol,
                    onValueChange = { cholesterol = it },
                    label = { Text("Cholesterol") })
                OutlinedTextField(
                    value = fastingBS,
                    onValueChange = { fastingBS = it },
                    label = { Text("Fasting BS (Yes/No)") })
                OutlinedTextField(
                    value = maxHR,
                    onValueChange = { maxHR = it },
                    label = { Text("Max HR") })
                OutlinedTextField(
                    value = exAngina,
                    onValueChange = { exAngina = it },
                    label = { Text("Exercise Angina (Yes/No)") })
                OutlinedTextField(
                    value = oldpeak,
                    onValueChange = { oldpeak = it },
                    label = { Text("Oldpeak") })
                OutlinedTextField(
                    value = stSlope,
                    onValueChange = { stSlope = it },
                    label = { Text("ST Slope (Up/Flat/Down)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (onValidate(age, restingBP, cholesterol, maxHR)) {
                    val request = HeartDiseaseRequest(
                        Age = age.toIntOrNull() ?: 50,
                        Sex = sex,
                        ChestPain = chestPain,
                        RestingBP = restingBP.toIntOrNull() ?: 120,
                        Cholesterol = cholesterol.toIntOrNull() ?: 200,
                        FastingBS = fastingBS,
                        MaxHR = maxHR.toIntOrNull() ?: 150,
                        ExAngina = exAngina,
                        Oldpeak = oldpeak.toDoubleOrNull() ?: 0.0,
                        ST_Slope = stSlope
                    )
                    onSubmit(request)
                } else Toast.makeText(
                    context,
                    "Please enter valid medical numbers.",
                    Toast.LENGTH_LONG
                ).show()
            }) { Text("Analyze") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .clip(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(all = 12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.imageURL != null) {
                AsyncImage(
                    model = message.imageURL,
                    contentDescription = "Attached Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(shape = RoundedCornerShape(size = 8.dp)),
                )
            }
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}