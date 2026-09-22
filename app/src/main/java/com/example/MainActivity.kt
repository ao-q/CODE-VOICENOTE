package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.FloatingFaceCamOverlayManager
import com.example.service.FloatingFaceCamService
import com.example.service.FloatingRecordingOverlayManager
import com.example.service.RecordingStateManager
import com.example.ui.VoiceNotesViewModel
import com.example.ui.screens.MainVoiceNotesScreen
import com.example.ui.theme.VoiceNotesTheme

class MainActivity : ComponentActivity() {

    private var viewModel: VoiceNotesViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // When actively recording, moving task to back keeps recording alive and shows floating overlay
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (RecordingStateManager.state.value.isRecording) {
                    moveTaskToBack(true)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            val vm: VoiceNotesViewModel = viewModel()
            viewModel = vm
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            VoiceNotesTheme(darkTheme = uiState.isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainVoiceNotesScreen(viewModel = vm)
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == FloatingRecordingOverlayManager.ACTION_STOP_AND_SAVE) {
            viewModel?.stopAndSaveRecording()
        }
    }

    override fun onStart() {
        super.onStart()
        // When returning to app, hide floating window controls
        FloatingRecordingOverlayManager.hide()
    }

    override fun onStop() {
        super.onStop()
        // When leaving app during active recording, show floating recording controls over other apps
        if (RecordingStateManager.state.value.isRecording) {
            FloatingRecordingOverlayManager.show(this)
        }
        // When leaving app with FaceCam active, ensure foreground service keeps camera streaming over other apps
        if (FloatingFaceCamOverlayManager.isShowing() && FloatingFaceCamOverlayManager.canDrawOverlays(this)) {
            FloatingFaceCamService.start(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!RecordingStateManager.state.value.isRecording) {
            FloatingRecordingOverlayManager.hide()
        }
    }
}
