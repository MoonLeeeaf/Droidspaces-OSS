package com.droidspaces.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.droidspaces.app.R
import com.droidspaces.app.util.ContainerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpstreamInterfaceList(
    upstreamInterfaces: List<String>,
    onInterfacesChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showUpstreamDialog by remember { mutableStateOf(false) }
    var availableUpstreams by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        availableUpstreams = ContainerManager.listUpstreamInterfaces()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Selected interfaces
        upstreamInterfaces.forEach { iface ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = iface, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onInterfacesChange(upstreamInterfaces - iface) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Add Button
        if (upstreamInterfaces.size < 8) {
            OutlinedButton(
                            onClick = { showUpstreamDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(context.getString(R.string.add_upstream_interface))
                        }
        }
    }

    if (showUpstreamDialog) {
        AddUpstreamDialog(
            availableUpstreams = availableUpstreams,
            selectedInterfaces = upstreamInterfaces,
            onDismiss = { showUpstreamDialog = false },
            onRefresh = {
                val newOnes = ContainerManager.listUpstreamInterfaces()
                availableUpstreams = newOnes
            },
            onAdd = { iface ->
                if (!upstreamInterfaces.contains(iface)) {
                    onInterfacesChange(upstreamInterfaces + iface)
                }
                showUpstreamDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddUpstreamDialog(
    availableUpstreams: List<String>,
    selectedInterfaces: List<String>,
    onDismiss: () -> Unit,
    onRefresh: suspend () -> Unit,
    onAdd: (String) -> Unit
) {
    val context = LocalContext.current
    var customIface by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            tween(durationMillis = 600, easing = LinearEasing)
        } else {
            tween(durationMillis = 0, easing = LinearEasing)
        },
        label = "refresh_rotation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .wrapContentHeight(),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(R.string.add_upstream_interface),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        IconButton(
                                            onClick = {
                                                if (!isRefreshing) {
                                                    isRefreshing = true
                                                    scope.launch {
                                                        val startTime = System.currentTimeMillis()
                                                        onRefresh()
                                                        val elapsed = System.currentTimeMillis() - startTime
                                                        val minRotationTime = 600L
                                                        if (elapsed < minRotationTime) {
                                                            delay(minRotationTime - elapsed)
                                                        }
                                                        isRefreshing = false
                                                    }
                                                }
                                            },
                                            enabled = !isRefreshing,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Refresh Interfaces",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .graphicsLayer { rotationZ = rotation },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (availableUpstreams.isNotEmpty()) {
                                        Text(context.getString(R.string.available_system_interfaces), style = MaterialTheme.typography.labelMedium)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f, fill = false)
                                                .heightIn(max = 240.dp)
                                        ) {
                                            FlowRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .verticalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                availableUpstreams.forEach { iface ->
                                                    OutlinedButton(
                                                        onClick = { onAdd(iface) },
                                                        enabled = !selectedInterfaces.contains(iface),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                    ) {
                                                        Text(iface)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(context.getString(R.string.enter_manually), style = MaterialTheme.typography.labelMedium)
                                    OutlinedTextField(
                                        value = customIface,
                                        onValueChange = { customIface = it },
                                        label = { Text(context.getString(R.string.interface_name_hint)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = onDismiss) {
                                            Text(context.getString(R.string.cancel))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                onAdd(customIface.trim())
                                            },
                                            enabled = customIface.isNotBlank() && selectedInterfaces.size < 8
                                        ) {
                                            Text(context.getString(R.string.add))
                                        }
                                    }
                                }
                            }
    }
}
