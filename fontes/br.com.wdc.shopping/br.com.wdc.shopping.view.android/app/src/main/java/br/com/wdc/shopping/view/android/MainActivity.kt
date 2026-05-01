package br.com.wdc.shopping.view.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.wdc.framework.commons.concurrent.ScheduledExecutor
import br.com.wdc.shopping.domain.ShoppingConfig
import br.com.wdc.shopping.presentation.presenter.Routes
import br.com.wdc.shopping.view.android.impl.RootViewAndroid
import br.com.wdc.shopping.api.client.RestConfig
import br.com.wdc.shopping.api.client.RestRepositoryBootstrap
import br.com.wdc.shopping.view.android.ui.RenderView
import br.com.wdc.shopping.view.android.ui.ShoppingTheme
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ShoppingMain"

        // TODO: tornar configurável (Settings ou BuildConfig)
        private const val SERVER_BASE_URL = "http://10.0.2.2:8080"
    }

    private var executorService: ScheduledExecutorService? = null
    private var app: ShoppingAndroidApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeBackend()

        val application = ShoppingAndroidApplication()
        Routes.root(application)
        this.app = application

        AndroidRenderLoop.start()

        enableEdgeToEdge()
        setContent {
            ShoppingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val rootPresenter = application.rootPresenter
                    val rootView = rootPresenter?.view() as? RootViewAndroid
                    if (rootView != null) {
                        RenderView(rootView)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        AndroidRenderLoop.stop()

        app?.release()
        app = null

        executorService?.shutdownNow()
        executorService = null

        releaseRepositories()
        ScheduledExecutor.BEAN.set(null)

        super.onDestroy()
    }

    private fun initializeBackend() {
        val baseDir = filesDir.toPath().resolve("work")
        configureDirectories(baseDir)

        val executor = Executors.newScheduledThreadPool(2)
        this.executorService = executor
        ScheduledExecutor.BEAN.set(ScheduledExecutorAndroidAdapter(executor))

        val restConfig = RestConfig(SERVER_BASE_URL)
        RestRepositoryBootstrap.initialize(restConfig)

        Log.i(TAG, "Backend initialized with REST client → $SERVER_BASE_URL")
    }

    private fun releaseRepositories() {
        RestRepositoryBootstrap.release()
    }

    private fun configureDirectories(baseDir: java.nio.file.Path) {
        val dirs = listOf("config", "data", "log", "temp")
        for (dir in dirs) {
            val path = baseDir.resolve(dir).toFile()
            if (!path.exists()) {
                path.mkdirs()
            }
        }

        ShoppingConfig.Internals.setBaseDir(baseDir)
        ShoppingConfig.Internals.setConfigDir(baseDir.resolve("config"))
        ShoppingConfig.Internals.setDataDir(baseDir.resolve("data"))
        ShoppingConfig.Internals.setLogDir(baseDir.resolve("log"))
        ShoppingConfig.Internals.setTempDir(baseDir.resolve("temp"))
    }
}
