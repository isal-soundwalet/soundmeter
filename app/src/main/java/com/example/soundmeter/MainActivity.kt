package com.example.soundmeter

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.soundmeter.ui.theme.SoundmeterTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permission
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (!isGranted) {
                    // Handle permission denial with a message or dialog
                }
            }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            SoundmeterTheme {
                SoundMeterScreen()
            }
        }
    }
}

@Composable
fun SoundMeterScreen() {
    var soundLevel by remember { mutableStateOf("0.00 dB") }
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = soundLevel,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isRecording) {
                            coroutineScope.cancel() // Stop coroutine
                            isRecording = false
                        } else {
                            isRecording = true
                            coroutineScope.launch {
                                startRecording { level ->
                                    soundLevel = "${String.format("%.2f", level)} dB"
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isRecording) "Stop Measuring" else "Start Measuring")
                }
            }
        }
    )
}

suspend fun startRecording(onSoundLevelUpdate: (Double) -> Unit) {
    withContext(Dispatchers.Default) {
        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            throw IllegalStateException("Buffer size is invalid")
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val buffer = ShortArray(bufferSize)
        audioRecord.startRecording()

        try {
            while (isActive) { // Stop when coroutine is cancelled
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val amplitude = buffer.map { it * it.toDouble() }.sum() / read
                    val decibels = if (amplitude > 0) 10 * kotlin.math.log10(amplitude) else 0.0
                    onSoundLevelUpdate(decibels)
                }
                delay(200) // Update every 200ms
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SoundMeterScreenPreview() {
    SoundmeterTheme {
        SoundMeterScreen()
    }
}
