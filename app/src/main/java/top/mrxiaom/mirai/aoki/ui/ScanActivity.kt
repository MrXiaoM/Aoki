package top.mrxiaom.mirai.aoki.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.get
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.U
import top.mrxiaom.mirai.aoki.U.startActivity
import top.mrxiaom.mirai.aoki.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private var qq: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        qq = intent.getLongExtra("qq", 0)
        if (qq == 0L) finish().let { return }
        val url = intent.getStringExtra("url") ?: finish().let { return }
        setResult(RESULT_OK, Intent().putExtra("qq", qq))

        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = U.userAgent
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.toolbar_copy -> Toast.makeText(this@ScanActivity, getSystemService<ClipboardManager>()?.let {
                        it.setPrimaryClip(ClipData.newPlainText(url, url))
                        R.string.scan_copy_done
                    } ?: R.string.scan_copy_failed, Toast.LENGTH_SHORT).show()
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