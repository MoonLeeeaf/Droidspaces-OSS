package com.droidspaces.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.R
import com.droidspaces.app.util.AnimationUtils
import com.droidspaces.app.util.ContainerInfo
import com.droidspaces.app.util.ContainerStatus
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContainerCard(
    container: ContainerInfo,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onRestart: () -> Unit = {},
    onEdit: () -> Unit = {},
    onEnter: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onMigrate: () -> Unit = {},
    onResize: () -> Unit = {},
    onExport: () -> Unit = {},
    isOperationRunning: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onShowLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(20.dp)
    Box {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .clip(cardShape)
                .combinedClickable(
                    onClick = {
                        /* if (container.isRunning) onEnter() else  */onToggleExpand()
                    },
                    indication = rememberRipple(bounded = true),
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            shape = cardShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                        .animateContentSize(animationSpec = AnimationUtils.mediumSpec())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Name and Status - single row with proper alignment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Icon + Name (single line for proper alignment)
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (container.isRunning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                        Text(
                            text = container.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Right side: Terminal button + Status label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Console button - always visible, stores all logs
                        IconButton(
                            onClick = onShowLogs,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = context.getString(R.string.view_logs),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Status card indicator
                        val (statusText, statusColor, statusBgColor) = when (container.status) {
                            ContainerStatus.RUNNING -> Triple(
                                context.getString(R.string.status_running),
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                            ContainerStatus.RESTARTING -> Triple(
                                context.getString(R.string.status_restarting),
                                MaterialTheme.colorScheme.onTertiaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                            ContainerStatus.STOPPED -> Triple(
                                context.getString(R.string.status_stopped),
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBgColor
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }
                }

                // PID info - separate row below header (only show if running)
                if (container.pid != null) {
                    Text(
                        text = context.getString(R.string.pid_label, container.pid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Hostname with sparse image indicator
                val hasHostname = container.hostname.isNotEmpty() && container.hostname != container.name
                val hasSparseImage = container.useSparseImage && container.sparseImageSizeGB != null

                if (hasHostname || hasSparseImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Only show hostname if it's not empty and different from container name
                        if (hasHostname) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = context.getString(R.string.hostname_label, container.hostname),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // Sparse image indicator (disk icon with size)
                        if (hasSparseImage) {
                            // Only show comma if hostname is also shown
                            if (hasHostname) {
                                Text(
                                    text = context.getString(R.string.comma),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_disk),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = context.getString(R.string.gb_size, container.sparseImageSizeGB ?: 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Options summary
                val options = mutableListOf<String>()
                if (container.disableIPv6) options.add(context.getString(R.string.ipv6_option))
                if (container.enableAndroidStorage) options.add(context.getString(R.string.storage_option))
                if (container.enableHwAccess) options.add(context.getString(R.string.hw_option))
                if (container.enableGpuMode || container.enableHwAccess) options.add(context.getString(R.string.gpu_option))
                if (container.enableTermuxX11) options.add(context.getString(R.string.x11_option))
                if (container.selinuxPermissive) options.add(context.getString(R.string.selinux_permissive_option))
                if (container.volatileMode) options.add(context.getString(R.string.volatile_option))
                if (container.forceCgroupv1) options.add(context.getString(R.string.cgroup_v1_option))
                if (container.blockNestedNs) options.add(context.getString(R.string.deadlock_shield_option))
                if (container.privileged.isNotEmpty()) options.add(context.getString(R.string.privileged_option))
                if (container.runAtBoot) options.add(context.getString(R.string.run_at_boot))
                if (options.isNotEmpty()) {
                    Text(
                        text = context.getString(R.string.options_label, options.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                HorizontalDivider()

                // Action buttons - evenly distributed, icons only (no labels to save space)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start button (icon only)
                    Button(
                        onClick = onStart,
                        enabled = !container.isRunning && !isOperationRunning,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp) // Remove default padding for icon-only buttons
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = context.getString(R.string.start),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Stop button (icon only)
                    Button(
                        onClick = onStop,
                        enabled = container.isRunning && !isOperationRunning,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = context.getString(R.string.stop),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Restart button (icon only)
                    Button(
                        onClick = onRestart,
                        enabled = container.isRunning && !isOperationRunning,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = context.getString(R.string.restart),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

                // Integrated Expandable Action Drawer (Sealed & Interactive)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = AnimationUtils.mediumSpec(),
                        expandFrom = Alignment.Top
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        animationSpec = AnimationUtils.mediumSpec(),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Spacer(Modifier.height(3.dp))

                        ActionItem(
                            icon = Icons.Default.Edit,
                            label = context.getString(R.string.edit_container_configuration),
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { onEdit() }
                    )

                    if (!container.useSparseImage) {
                        ActionItem(
                            icon = painterResource(id = R.drawable.ic_disk),
                            label = context.getString(R.string.migrate_to_sparse_image),
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = { onMigrate() }
                        )
                    } else {
                        ActionItem(
                            icon = painterResource(id = R.drawable.ic_disk),
                            label = context.getString(R.string.resize_sparse_image),
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = { onResize() }
                        )
                    }

                        ActionItem(
                            icon = Icons.Default.FileDownload,
                            label = context.getString(R.string.export_container),
                            tint = MaterialTheme.colorScheme.tertiary,
                            onClick = { onExport() }
                        )

                        ActionItem(
                            icon = Icons.Default.Delete,
                            label = context.getString(R.string.uninstall_container_menu),
                            tint = MaterialTheme.colorScheme.error,
                            isBold = true,
                            onClick = { onUninstall() }
                        )
                    }
                }
            }
        }
    }


@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    isBold: Boolean = false,
    onClick: () -> Unit
) = ActionItemLayout(label, tint, isBold, onClick) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
}

@Composable
private fun ActionItem(
    icon: Painter,
    label: String,
    tint: Color,
    isBold: Boolean = false,
    onClick: () -> Unit
) = ActionItemLayout(label, tint, isBold, onClick) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                        }

@Composable
private fun ActionItemLayout(
    label: String,
    tint: Color,
    isBold: Boolean,
    onClick: () -> Unit,
    iconSlot: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            iconSlot()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = if (isBold) tint else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

