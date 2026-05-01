package br.com.wdc.shopping.view.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.wdc.shopping.view.android.impl.RootViewAndroid

@Composable
fun RootComposable(view: RootViewAndroid) {
    view.revision.value // observe changes

    Box(modifier = Modifier.fillMaxSize()) {
        val contentView = view.contentSlot.current.value
        RenderView(contentView)

        val errorMessage = view.presenter.state.errorMessage
        if (!errorMessage.isNullOrBlank()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                action = {
                    TextButton(onClick = {
                        view.presenter.state.errorMessage = null
                        view.presenter.update()
                    }) {
                        Text("OK")
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Text(text = errorMessage)
            }
        }
    }
}
