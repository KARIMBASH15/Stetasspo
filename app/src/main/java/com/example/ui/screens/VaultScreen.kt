package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manager.FileManager
import com.example.manager.VaultManager
import com.example.model.VaultEntry
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VaultScreen(
    vaultManager: VaultManager,
    fileManager: FileManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var isPinConfigured by remember { mutableStateOf(vaultManager.isPinSet()) }
    var isUnlocked by remember { mutableStateOf(false) }

    // PIN Input state
    var pinInput by remember { mutableStateOf("") }
    var setupPinStep by remember { mutableStateOf(1) } // 1: Enter new, 2: Confirm new
    var firstEnteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val vaultFiles = remember { mutableStateListOf<VaultEntry>() }

    fun refreshVault() {
        scope.launch {
            val list = vaultManager.getLockedFiles()
            vaultFiles.clear()
            vaultFiles.addAll(list)
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) refreshVault()
    }

    if (!isPinConfigured) {
        // Initial PIN Setup
        PinKeypadView(
            title = if (setupPinStep == 1) "Create Vault PIN (تعيين كلمة مرور الخزنة)" else "Confirm Vault PIN (تأكيد الرمز)",
            subtitle = if (setupPinStep == 1) "Enter a 4-digit security PIN to protect private files" else "Re-enter your 4-digit PIN to confirm",
            pin = pinInput,
            errorMessage = errorMessage,
            onDigitClick = { digit ->
                if (pinInput.length < 4) {
                    pinInput += digit
                    errorMessage = null
                    if (pinInput.length == 4) {
                        if (setupPinStep == 1) {
                            firstEnteredPin = pinInput
                            pinInput = ""
                            setupPinStep = 2
                        } else {
                            if (pinInput == firstEnteredPin) {
                                vaultManager.setPin(pinInput)
                                isPinConfigured = true
                                isUnlocked = true
                                Toast.makeText(context, "Safe Vault PIN Created! 🛡️", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMessage = "PINs do not match. Try again."
                                pinInput = ""
                                setupPinStep = 1
                            }
                        }
                    }
                }
            },
            onDeleteClick = {
                if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
            }
        )
    } else if (!isUnlocked) {
        // Unlock Screen
        PinKeypadView(
            title = "Safe Vault Locked (الخزنة مقفلة)",
            subtitle = "Enter your 4-digit PIN to access private encrypted files",
            pin = pinInput,
            errorMessage = errorMessage,
            onDigitClick = { digit ->
                if (pinInput.length < 4) {
                    pinInput += digit
                    errorMessage = null
                    if (pinInput.length == 4) {
                        if (vaultManager.verifyPin(pinInput)) {
                            isUnlocked = true
                            pinInput = ""
                        } else {
                            errorMessage = "Incorrect PIN. Access Denied."
                            pinInput = ""
                        }
                    }
                }
            },
            onDeleteClick = {
                if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
            }
        )
    } else {
        // Unlocked Vault Content
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                // Vault Header & Lock Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF10B981))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Safe Vault (الخزنة الآمنة)",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Encrypted & Hidden Storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { isUnlocked = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock Now")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy & Security Dashboard Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy & Security Protection: High",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All files in this vault are encrypted with obfuscated byte-level isolation and hidden from the standard system media scanner.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(""))
                                    Toast.makeText(context, "Clipboard cleared securely!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Clipboard")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Encrypted Vault Files (${vaultFiles.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (vaultFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No files in the vault yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use the file manager menu 'Lock to Vault' to secure private documents, audio, or video here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(vaultFiles, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.originalName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${fileManager.formatFileSize(item.size)} • ${fileManager.formatDate(item.dateLocked)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Unlock / Restore button
                            IconButton(onClick = {
                                scope.launch {
                                    val restored = vaultManager.unlockFile(item)
                                    if (restored != null) {
                                        Toast.makeText(context, "Restored to ${restored.name}", Toast.LENGTH_SHORT).show()
                                        refreshVault()
                                    } else {
                                        Toast.makeText(context, "Failed to restore", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Permanent Delete button
                            IconButton(onClick = {
                                scope.launch {
                                    vaultManager.deleteVaultFile(item)
                                    Toast.makeText(context, "Wiped permanently", Toast.LENGTH_SHORT).show()
                                    refreshVault()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Wipe", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinKeypadView(
    title: String,
    subtitle: String,
    pin: String,
    errorMessage: String?,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                val isFilled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Keypad grid 1-9, 0, Backspace
        val digits = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            digits.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(70.dp))
                        } else if (key == "DEL") {
                            Surface(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .clickable { onDeleteClick() },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Backspace, contentDescription = "Delete")
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .clickable { onDigitClick(key) }
                                    .testTag("key_$key"),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
