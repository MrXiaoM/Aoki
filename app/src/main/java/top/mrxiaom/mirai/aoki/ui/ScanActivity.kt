package top.mrxiaom.mirai.aoki.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.core.content.getSystemService
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.databinding.ActivityScanBinding
import top.mrxiaom.mirai.aoki.util.AokiActivity
import top.mrxiaom.mirai.aoki.util.copy

class ScanActivity : AokiActivity<ActivityScanBinding>(ActivityScanBinding::class) {
    private var qq: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        qq = intent.getLongExtra("qq", 0)
        if (qq == 0L) finish().let { return }
        val url = intent.getStringExtra("url") ?: finish().let { return }
        val userAgent = intent.getStringExtra("ua") ?: finish().let { return }

        setResult(RESULT_OK, Intent().putExtra("qq", qq))

        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = userAgent
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.toolbar_copy -> copy(url)
                R.id.toolbar_refresh -> webView.reload()
            }
            return@setOnMenuItemClickListener false
        }
        webView.loadUrl(url)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scan_menu, menu)
        return true
    }
}