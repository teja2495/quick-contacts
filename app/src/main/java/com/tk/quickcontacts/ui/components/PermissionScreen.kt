package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.R

private val GrantedCheckmarkColor = Color(0xFF4CAF50)
private val SpacingXLarge = 20.dp
private val SpacingLarge = 16.dp
private val IconSize = 24.dp

@Composable
fun PermissionRequestScreen(
    hasCallPermission: Boolean,
    hasContactsPermission: Boolean,
    hasCallLogPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestCallLogPermission: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.title_permissions_required),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = stringResource(R.string.privacy_description),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column {
                    PermissionRow(
                        title = stringResource(R.string.permission_contacts_access),
                        description = stringResource(R.string.permission_contacts_description),
                        isGranted = hasContactsPermission,
                        onRequestPermission = onRequestContactsPermission,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = SpacingXLarge),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    PermissionRow(
                        title = stringResource(R.string.permission_phone_access),
                        description = stringResource(R.string.permission_phone_description),
                        isGranted = hasCallPermission,
                        onRequestPermission = onRequestPhonePermission,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = SpacingXLarge),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    PermissionRow(
                        title = stringResource(R.string.permission_call_history_access),
                        description = stringResource(R.string.permission_call_history_description),
                        isGranted = hasCallLogPermission,
                        onRequestPermission = onRequestCallLogPermission,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingLarge))

        Button(
            onClick = onContinue,
            enabled = hasContactsPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                text = stringResource(R.string.action_continue),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(SpacingXLarge),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isGranted) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.permission_granted),
                tint = GrantedCheckmarkColor,
                modifier = Modifier
                    .padding(start = SpacingXLarge)
                    .size(IconSize),
            )
        } else {
            Switch(
                checked = false,
                onCheckedChange = { if (it) onRequestPermission() },
                modifier = Modifier.padding(start = SpacingXLarge),
            )
        }
    }
} 
