package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.storage.StorageUriHelper
import java.io.File

/**
 * Minimal and streamlined storage location configuration dialog.
 */
@Composable
fun StorageConfigOverlay(
    currentFolderName: String,
    currentBasePath: String,
    isFirstLaunch: Boolean,
    onConfirm: (folderName: String, customPath: String?, treeUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var folderName by remember { mutableStateOf(currentFolderName.ifBlank { "VoiceNotes" }) }
    var selectedCustomPath by remember { mutableStateOf<String?>(null) }
    var selectedTreeUri by remember { mutableStateOf<String?>(null) }
    var selectedPreset by remember { mutableStateOf("Documents") } // "Documents", "Recordings", "Music", "Custom"

    // Storage Access Framework system folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val resolvedPath = StorageUriHelper.getPathFromTreeUri(context, uri)
            selectedTreeUri = uri.toString()
            if (!resolvedPath.isNullOrBlank()) {
                selectedCustomPath = resolvedPath
                selectedPreset = "Custom"
            } else {
                selectedCustomPath = uri.path
                selectedPreset = "Custom"
            }
        }
    }

    // Resolve the active base directory to display
    val activeBaseDirectory: File = when (selectedPreset) {
        "Documents" -> StorageUriHelper.getPublicDocumentsDir()
        "Recordings" -> StorageUriHelper.getPublicRecordingsDir()
        "Music" -> StorageUriHelper.getPublicMusicDir()
        "Custom" -> {
            if (!selectedCustomPath.isNullOrBlank()) File(selectedCustomPath!!)
            else StorageUriHelper.getPublicDocumentsDir()
        }
        else -> StorageUriHelper.getPublicDocumentsDir()
    }

    val finalResolvedDirectory = File(activeBaseDirectory, folderName.ifBlank { "VoiceNotes" })
    val displayPath = finalResolvedDirectory.absolutePath
    val internalStoragePrefix = Environment.getExternalStorageDirectory().absolutePath
    val friendlyPath = if (displayPath.startsWith(internalStoragePrefix)) {
        displayPath.substring(internalStoragePrefix.length).trimStart('/')
    } else {
        displayPath
    }

    Dialog(
        onDismissRequest = {
            if (!isFirstLaunch) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isFirstLaunch,
            dismissOnClickOutside = !isFirstLaunch,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .testTag("storage_config_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header: Compact icon & title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Storage Location",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location Presets (minimal chips with icons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedPreset == "Documents",
                        onClick = {
                            selectedPreset = "Documents"
                            selectedCustomPath = null
                            selectedTreeUri = null
                        },
                        label = { Text("Documents") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = selectedPreset == "Recordings",
                        onClick = {
                            selectedPreset = "Recordings"
                            selectedCustomPath = null
                            selectedTreeUri = null
                        },
                        label = { Text("Recordings") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = selectedPreset == "Music",
                        onClick = {
                            selectedPreset = "Music"
                            selectedCustomPath = null
                            selectedTreeUri = null
                        },
                        label = { Text("Music") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = selectedPreset == "Custom",
                        onClick = { folderPickerLauncher.launch(null) },
                        label = { Text(if (selectedPreset == "Custom") "Custom" else "Browse...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("browse_device_storage_button"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subfolder Name Input
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("VoiceNotes") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_folder_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtle path preview
                Text(
                    text = "Path: $friendlyPath",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons: Cancel and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFirstLaunch) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            val resolvedName = folderName.ifBlank { "VoiceNotes" }
                            val customPath = if (selectedPreset == "Custom") selectedCustomPath else activeBaseDirectory.absolutePath
                            onConfirm(resolvedName, customPath, selectedTreeUri)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_storage_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
