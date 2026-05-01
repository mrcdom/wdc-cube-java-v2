package br.com.wdc.shopping.view.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.wdc.shopping.view.android.impl.PurchasesPanelViewAndroid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesPanelComposable(view: PurchasesPanelViewAndroid) {
    view.revision.value // observe

    val state = view.presenter.state
    val purchases = state.purchases ?: emptyList()
    val page = state.page
    val pageSize = state.pageSize
    val totalCount = state.totalCount
    val totalPages = if (totalCount > 0 && pageSize > 0) ((totalCount + pageSize - 1) / pageSize) else 1
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column {
        Text(
            text = "Compras realizadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (purchases.isEmpty()) {
                item {
                    Text(
                        text = "Nenhuma compra realizada",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            items(purchases, key = { it.id }) { purchase ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runAsync(view.presenter.app) { view.presenter.onOpenReceipt(purchase.id) } },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dateFormat.format(Date(purchase.date)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val itemsSummary = purchase.items?.joinToString(", ") ?: ""
                            if (itemsSummary.isNotBlank()) {
                                Text(
                                    text = itemsSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "R$ %.2f".format(purchase.total),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Pagination
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { runAsync(view.presenter.app) { view.presenter.onPageChange(page - 1) } },
                enabled = page > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior")
            }
            Text("${page + 1} / $totalPages")
            IconButton(
                onClick = { runAsync(view.presenter.app) { view.presenter.onPageChange(page + 1) } },
                enabled = page + 1 < totalPages
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próxima")
            }
        }
    }
}
