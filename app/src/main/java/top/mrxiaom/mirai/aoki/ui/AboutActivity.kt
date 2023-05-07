package top.mrxiaom.mirai.aoki.ui

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.databinding.ActivityAboutBinding
import top.mrxiaom.mirai.aoki.util.AokiActivity
import top.mrxiaom.mirai.aoki.util.text

class AboutActivity : AokiActivity<ActivityAboutBinding>(ActivityAboutBinding::class) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)

        binding.textView.apply {
            text = Html.fromHtml(text(R.string.about_body), 0)
            movementMethod = LinkMovementMethod.getInstance()
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