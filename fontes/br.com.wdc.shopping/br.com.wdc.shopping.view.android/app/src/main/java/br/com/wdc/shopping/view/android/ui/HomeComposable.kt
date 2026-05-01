package br.com.wdc.shopping.view.android.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.wdc.shopping.view.android.impl.HomeViewAndroid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeComposable(view: HomeViewAndroid) {
    view.revision.value // observe

    val state = view.presenter.state
    val contentView = view.contentSlot.current.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Olá, ${state.nickName ?: ""}!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { runAsync(view.presenter.app) { view.presenter.onOpenCart() } }) {
                        BadgedBox(badge = {
                            if (state.cartItemCount > 0) {
                                Badge { Text("${state.cartItemCount}") }
                            }
                        }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrinho")
                        }
                    }
                    IconButton(onClick = { runAsync(view.presenter.app) { view.presenter.onExit() } }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (contentView != null) {
                // Show sub-content (Product, Cart, Receipt)
                RenderView(contentView)
            } else {
                // Default: products panel + purchases panel
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Products panel
                    Box(modifier = Modifier.weight(1f)) {
                        RenderView(view.productsPanelSlot.current.value)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Purchases panel
                    Box(modifier = Modifier.weight(1f)) {
                        RenderView(view.purchasesPanelSlot.current.value)
                    }
                }
            }
        }
    }

    // Show error snackbar
    if (state.errorCode != 0 && !state.errorMessage.isNullOrBlank()) {
        // The error will be shown inline; cleared on next action
    }
}
