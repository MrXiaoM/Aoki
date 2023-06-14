package top.mrxiaom.mirai.aoki.ui

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toFile
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.databinding.ActivityAboutBinding
import top.mrxiaom.mirai.aoki.util.AokiActivity
import top.mrxiaom.mirai.aoki.util.mdToHtml
import top.mrxiaom.mirai.aoki.util.readRawText
import top.mrxiaom.mirai.aoki.util.setupRawResource
import java.io.ByteArrayInputStream

class AboutActivity : AokiActivity<ActivityAboutBinding>(ActivityAboutBinding::class) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)

        binding.aboutWebView.apply {
            setupRawResource()
            loadData(mdToHtml(readRawText(R.raw.about)), "text/html", "utf-8")
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}