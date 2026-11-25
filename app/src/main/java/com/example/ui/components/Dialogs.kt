package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Playlist
import com.example.ui.songCountLabel
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgDanger
import com.example.ui.theme.FgIsDark
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgPrimaryDeep
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted

@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = FgSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FgText,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.playlist_name_placeholder)) },
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                modifier = Modifier.testTag("playlist_dialog_confirm"),
            ) {
                Text(confirmLabel, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = FgTextMuted)
            }
        },
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
) {
    var creatingNew by remember { mutableStateOf(false) }

    if (creatingNew) {
        PlaylistNameDialog(
            title = stringResource(R.string.action_new_playlist),
            confirmLabel = stringResource(R.string.action_create_and_add),
            onDismiss = { creatingNew = false },
            onConfirm = { newName ->
                creatingNew = false
                onCreateNewPlaylist(newName)
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = FgSurface,
        title = {
            Text(
                text = stringResource(R.string.action_add_to_playlist),
                style = MaterialTheme.typography.titleLarge,
                color = FgText,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { creatingNew = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = FgPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.playlist_create_new),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FgPrimary,
                    )
                }
                HorizontalDivider(color = FgBorder)

                if (playlists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlist_none_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlaylist(pl.id) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = FgTextMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = pl.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FgText,
                                    )
                                    Text(
                                        text = songCountLabel(pl.trackIds.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FgTextMuted,
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = FgBorder)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = FgTextMuted)
            }
        },
    )
}

@Composable
fun ConfirmDangerDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = FgSurface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = FgText) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = FgTextMuted) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = FgDanger),
            ) {
                Text(confirmLabel, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = FgTextMuted)
            }
        },
    )
}

private val ALPHABET_INDEX_KEYS: List<String> = listOf("#") + ('A'..'Z').map { it.toString() }

@Composable
fun JumpToLetterDialog(
    availableLetters: Set<String>,
    activeLetter: String?,
    onSelectLetter: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val isDark = FgIsDark
        val backdropColor = if (isDark) Color(0xFF0E1014).copy(alpha = 0.68f) else Color.White.copy(alpha = 0.62f)
        val panelColor = if (isDark) Color(0xFF1A1D24).copy(alpha = 0.86f) else Color.White.copy(alpha = 0.80f)
        val panelBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)

        // Translucent frosted backdrop that lets the albums/artists shine through
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .background(panelColor)
                    .border(1.dp, panelBorder)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .testTag("jump_to_letter_dialog"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("jump_to_letter_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = FgTextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val rows = ALPHABET_INDEX_KEYS.chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowLetters ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowLetters.forEach { letter ->
                                val isAvailable = letter in availableLetters
                                val isActive = isAvailable && letter == activeLetter
                                val tileBg = when {
                                    isActive -> FgPrimaryDeep
                                    isAvailable -> FgPrimary
                                    isDark -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.White.copy(alpha = 0.52f)
                                }
                                val textColor = when {
                                    isAvailable -> Color.White
                                    isDark -> Color.White.copy(alpha = 0.30f)
                                    else -> Color(0xFF9E9E9E)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(tileBg)
                                        .then(
                                            if (isAvailable) {
                                                Modifier.clickable { onSelectLetter(letter) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("jump_letter_$letter"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textColor,
                                    )
                                }
                            }
                            repeat(7 - rowLetters.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
