package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold


@Composable
fun ManualSubtypeStepScreen(
    draft: AddItemDraft,
    onPick: (ManualSubType) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }

    val type: ManualType? = draft.manualType
    val list = type?.let { taxonomy.subtypesOf(it) }.orEmpty()
    val typeMeta = type?.let { taxonomy.typeMeta(it) }

    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choisir un sous-type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (type == null) {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = onBack,
                    label = { Text("Revenir au choix du type") }
                )
                return@AddItemStepScaffold
            }

            if (list.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aucun sous-type disponible pour ${typeMeta?.title ?: type.name}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@AddItemStepScaffold
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { subMeta ->
                    val imageRes = drawableId(context, subMeta.image)

                    SubtypeRow(
                        title = subMeta.title,
                        selected = draft.manualSubtype?.name == subMeta.code,
                        imageResId = imageRes,
                        onClick = {
                            runCatching { ManualSubType.valueOf(subMeta.code) }
                                .getOrNull()
                                ?.let(onPick)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtypeRow(
    title: String,
    selected: Boolean,
    imageResId: Int,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.large

    Surface(
        onClick = onClick,
        shape = shape,
        tonalElevation = 0.dp,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(imageResId),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
