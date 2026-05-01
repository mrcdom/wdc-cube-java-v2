package br.com.wdc.shopping.view.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.wdc.shopping.view.android.impl.CartViewAndroid

@Composable
fun CartComposable(view: CartViewAndroid) {
    view.revision.value // observe

    val state = view.presenter.state
    val items = state.items ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Carrinho de Compras",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.errorCode != 0 && !state.errorMessage.isNullOrBlank()) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        text = "Carrinho vazio",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "R$ %.2f".format(item.price),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Quantity controls
                        IconButton(onClick = {
                            runAsync(view.presenter.app) { view.presenter.onModifyQuantity(item.id, item.quantity - 1) }
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                        }
                        Text("${item.quantity}", style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = {
                            runAsync(view.presenter.app) { view.presenter.onModifyQuantity(item.id, item.quantity + 1) }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Aumentar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { runAsync(view.presenter.app) { view.presenter.onRemoveProduct(item.id) } }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remover",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = { runAsync(view.presenter.app) { view.presenter.onOpenProducts() } }) {
                Text("Continuar comprando")
            }
            Button(
                onClick = { runAsync(view.presenter.app) { view.presenter.onBuy() } },
                enabled = items.isNotEmpty()
            ) {
                Text("COMPRAR")
            }
        }
    }
}
