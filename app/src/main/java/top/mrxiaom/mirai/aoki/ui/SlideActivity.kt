package top.mrxiaom.mirai.aoki.ui

import android.net.Uri
import android.os.Bundle
import android.webkit.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.mrxiaom.mirai.aoki.AokiLoginSolver
import top.mrxiaom.mirai.aoki.databinding.ActivitySlideBinding
import top.mrxiaom.mirai.aoki.util.AokiActivity

/**
 * 验证逻辑来自
 * https://github.com/mzdluo123/TxCaptchaHelper/blob/master/app/src/main/java/io/github/mzdluo123/txcaptchahelper/CaptchaActivity.kt
 */
class SlideActivity : AokiActivity<ActivitySlideBinding>(ActivitySlideBinding::class) {
    private var qq: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        qq = intent.getLongExtra("qq", 0)
        if (qq == 0L) finish().let { return }
        val url = intent.getStringExtra("url") ?: finish().let { return }
        val userAgent = intent.getStringExtra("ua") ?: finish().let { return }

        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = userAgent
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = onJsBridgeInvoke(request.url)

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean = onJsBridgeInvoke(Uri.parse(url))
        }
        WebView.setWebContentsDebuggingEnabled(true)
        webView.loadUrl(url)
    }

    private fun onJsBridgeInvoke(request: Uri): Boolean {
        if (request.path.equals("/onVerifyCAPTCHA")) {
            val p = request.getQueryParameter("p") ?: return false

            val capUnion = runCatching {
                Json.decodeFromString(JsonObject.serializer(), p)
            }.getOrNull()
            val ticket = capUnion?.let { it["ticket"]?.jsonPrimitive?.content } ?: return false

            AokiLoginSolver.slideDefList[qq]?.complete(ticket)
            finish()
        }
        return false
    }
}