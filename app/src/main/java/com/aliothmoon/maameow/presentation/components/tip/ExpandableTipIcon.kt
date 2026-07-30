package com.aliothmoon.maameow.presentation.components.tip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R

@Composable
fun ExpandableTipIcon(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateDescription = stringResource(
        if (expanded) R.string.common_collapse else R.string.common_expand,
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { this.stateDescription = stateDescription }
            .clickable { onExpandedChange(!expanded) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = stringResource(R.string.accessibility_tip),
            tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}
