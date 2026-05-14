package com.droidspaces.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.util.Constants
import com.droidspaces.app.util.SystemInfoManager
import androidx.compose.foundation.combinedClickable
import com.droidspaces.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DroidspacesStatus {
    Working,
    UpdateAvailable,
    NotInstalled,
    Unsupported,
    Corrupted,
    ModuleMissing
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DroidspacesStatusCard(
    status: DroidspacesStatus,
    version: String? = null,
    isChecking: Boolean = false,
    isRootAvailable: Boolean = true,
    refreshTrigger: Int = 0,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isWorking = isRootAvailable && status == DroidspacesStatus.Working

    var droidspacesVersion by remember {
        mutableStateOf(version ?: SystemInfoManager.getCachedDroidspacesVersion(context))
    }

    // Backend execution mode ("direct" or "daemon")
    var backendMode by remember {
        mutableStateOf(if (isWorking) SystemInfoManager.getCachedBackendMode(context) else null)
    }

    LaunchedEffect(status, isRootAvailable, refreshTrigger) {
        if (isWorking) {
            droidspacesVersion = SystemInfoManager.getDroidspacesVersion(context)
            backendMode = SystemInfoManager.getBackendMode(context)
        } else {
            backendMode = null
        }
    }

    

    // Avoid Pair allocation - compute directly
    // Match KernelSU's error styling: red error container for all error states
    val containerColor = when {
        !isRootAvailable -> MaterialTheme.colorScheme.errorContainer
        status == DroidspacesStatus.Working -> MaterialTheme.colorScheme.secondaryContainer
        status == DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val isError = !isWorking

    // Load cached values immediately (synchronous, instant display)
    var rootProviderVersion by remember {
        mutableStateOf<String?>(
            if (status == DroidspacesStatus.Working || status == DroidspacesStatus.UpdateAvailable) {
                SystemInfoManager.getCachedRootProviderVersion(context) ?: context.getString(R.string.unknown)
            } else null
        )
    }

    val cardShape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val icon = when {
        !isRootAvailable -> Icons.Default.Error
        status == DroidspacesStatus.Working -> Icons.Default.Verified
        status == DroidspacesStatus.UpdateAvailable -> Icons.Default.Update
        else -> Icons.Default.CheckCircle
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        shape = cardShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(
                // Make clickable if onClick is provided OR if status requires re-installation/update
                if (onClick != {} || status == DroidspacesStatus.UpdateAvailable) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(38.dp),
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !isRootAvailable -> MaterialTheme.colorScheme.onErrorContainer
                    status == DroidspacesStatus.Working -> MaterialTheme.colorScheme.primary
                    status == DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            !isRootAvailable -> context.getString(R.string.root_unavailable)
                            isChecking -> context.getString(R.string.backend_checking)
                            isWorking -> context.getString(R.string.backend_installed)
                            status == DroidspacesStatus.UpdateAvailable -> context.getString(R.string.backend_update_available)
                            else -> context.getString(R.string.backend_attention_required)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            !isRootAvailable -> MaterialTheme.colorScheme.onErrorContainer
                            status == DroidspacesStatus.Working -> MaterialTheme.colorScheme.onSecondaryContainer
                            status == DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    // Execution mode badge (DIRECT / DAEMON)
                    if (backendMode != null && isWorking) {
                        Surface(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = backendMode!!,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // Check root availability first - if root is unavailable, always show the grant message
                if (!isRootAvailable) {
                    Text(
                        text = context.getString(R.string.grant_root_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                } else if (status == DroidspacesStatus.Working || status == DroidspacesStatus.UpdateAvailable) {
                    Text(
                        text = context.getString(R.string.version_label, droidspacesVersion ?: context.getString(R.string.unknown)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (status) {
                            DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        }
                    )
                    // Root provider version - instant display, no animations
                    Text(
                        text = context.getString(R.string.root_provider_label, rootProviderVersion ?: context.getString(R.string.unknown)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (status) {
                            DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        }
                    )
                } else {
                    Text(
                        text = when {
                            status == DroidspacesStatus.UpdateAvailable -> context.getString(R.string.update_available_message)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            status == DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }
}

