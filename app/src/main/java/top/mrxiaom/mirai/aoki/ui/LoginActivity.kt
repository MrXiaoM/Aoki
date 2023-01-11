package top.mrxiaom.mirai.aoki.ui

import android.Manifest
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.BotConfiguration.HeartbeatStrategy
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol
import net.mamoe.mirai.utils.DeviceVerificationRequests
import net.mamoe.mirai.utils.mapToArray
import top.mrxiaom.mirai.aoki.*
import top.mrxiaom.mirai.aoki.U.buttonNegative
import top.mrxiaom.mirai.aoki.U.buttonPositive
import top.mrxiaom.mirai.aoki.U.mkdirsQuietly
import top.mrxiaom.mirai.aoki.U.needPermission
import top.mrxiaom.mirai.aoki.U.onItemSelected
import top.mrxiaom.mirai.aoki.U.setMessage
import top.mrxiaom.mirai.aoki.U.startActivity
import top.mrxiaom.mirai.aoki.U.startActivityForResult
import top.mrxiaom.mirai.aoki.U.text
import top.mrxiaom.mirai.aoki.databinding.ActivityLoginBinding
import top.mrxiaom.mirai.aoki.ui.model.LoginViewModel
import java.io.File
import java.net.URL
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes


class LoginActivity : AppCompatActivity() {

    private lateinit var mHandler: Handler
    internal val loginViewModel = LoginViewModel()
    private lateinit var binding: ActivityLoginBinding
    private lateinit var externalRoot: File
    fun runInUIThread(action: LoginActivity.() -> Unit) {
        mHandler.post { action() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler(mainLooper)

        externalRoot = File(getExternalFilesDir(null), "AokiMirai").also { it.mkdirsQuietly() }

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { p0, e ->
            try {
                File(externalRoot, "crash.log").writeText(e.stackTraceToString())
            }catch (_: Throwable){
                // 收声
            }
            handler?.uncaughtException(p0, e)
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AokiLoginSolver.loginActivity = this

        needPermission(
            1,
            Manifest.permission.INTERNET
        )

        val qq = binding.qq
        val password = binding.password
        val login = binding.login
        val accounts = binding.accounts
        val loading = binding.loading
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.toolbar_about -> startActivity<AboutActivity>()
            }
            return@setOnMenuItemClickListener false
        }
        binding.infomation.apply {
            val version = packageManager.getPackageInfo(packageName,PackageManager.GET_CONFIGURATIONS).versionName

            text = """
                Aoki $version, mirai $miraiVersion
                User Agent: ${U.userAgent}
                """.trimIndent()
        }
        val protocols = arrayOf(
            MiraiProtocol.ANDROID_PHONE,
            MiraiProtocol.ANDROID_PAD,
            MiraiProtocol.ANDROID_WATCH,
            MiraiProtocol.IPAD,
            MiraiProtocol.MACOS,
        )
        val protocol = binding.protocol
        protocol.adapter = ArrayAdapter.createFromResource(this, R.array.spinner_protocol, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        protocol.onItemSelected {
            if (it < 0) return@onItemSelected
            BotManager.defaultProtocol = protocols[it]
        }
        val hbStrategies = arrayOf(
            HeartbeatStrategy.STAT_HB,
            HeartbeatStrategy.REGISTER,
            HeartbeatStrategy.NONE,
        )
        val hbStrategy = binding.hbStrategy
        hbStrategy.adapter = ArrayAdapter.createFromResource(this, R.array.spinner_hb_strategy, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        hbStrategy.onItemSelected {
            if (it < 0) return@onItemSelected
            BotManager.defaultHbStrategy = hbStrategies[it]
        }
        // 滑块验证请求
        loginViewModel.slideRequest.observe(this) {
            val slideRequest = it ?: return@observe
            startActivity<SlideActivity> {
                putExtra("qq", slideRequest.bot.id)
                putExtra("url", slideRequest.url)
            }
        }
        // 扫码验证请求
        loginViewModel.scanRequest.observe(this) {
            val scanRequest = it ?: return@observe
            startActivityForResult<ScanActivity>(1) {
                putExtra("qq", scanRequest.bot.id)
                putExtra("url", scanRequest.url)
            }
        }
        // 短信验证码请求
        loginViewModel.smsRequest.observe(this@LoginActivity) {
            val smsRequest = it ?: return@observe
            val def = AokiLoginSolver.smsDefList[smsRequest.bot.id] ?: return@observe
            val phoneNumberFull = smsRequest.sms.run {
                if (countryCode != null && phoneNumber != null)
                    text(R.string.captcha_sms_request_phoneNumber)
                        .replace("\$countryCode", countryCode.toString())
                        .replace("\$phoneNumber", phoneNumber.toString())
                else text(R.string.captcha_sms_request_phoneNumberNull)
            }
            AlertDialog.Builder(this).setTitle(R.string.captcha_sms_request_title)
                .setCancelable(false)
                .setMessage(R.string.captcha_sms_request_message) { text -> text.replace("\$phoneNumberFull", phoneNumberFull) }
                .buttonPositive(R.string.captcha_sms_request_send) {
                    smsSent(def, smsRequest.sms)
                }.buttonNegative(R.string.captcha_sms_request_other) {
                    def.complete(null)
                }.show()
        }
        loginViewModel.loginResult.observe(this) {
            val loginResult = it ?: return@observe

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error.stackTraceToString())
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            login.isClickable = true
        }
        fun login() {
            loginViewModel.viewModelScope.launch {
                loginViewModel.login(
                    externalRoot,
                    qq.text.toString().toLong(),
                    password.text.toString()
                )
            }
        }
        password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    login()
                }
            }
            false
        }
        login.setOnClickListener {
            loading.visibility = View.VISIBLE
            login.isClickable = false

            login()
        }
        accounts.setOnClickListener {
            val alert = AlertDialog.Builder(this)
                .setTitle(R.string.accounts_title)
                .buttonNegative(R.string.cancel)
            val accountList = File(externalRoot, "bots").listFiles()?.mapNotNull {
                it.name.toLongOrNull()
            }?.map { it.toString() }?.toTypedArray()
            if (!accountList.isNullOrEmpty()) alert.setItems(accountList) { _, i ->
                val account = accountList[i]
                val folder = File(externalRoot, "bots/$account")
                AlertDialog.Builder(this).setTitle(account)
                    .setItems(R.array.accounts_operation) { _, btn ->
                        when (btn) {
                            0 -> File(folder, "device.json").delete()
                            1 -> File(folder, "cache").delete()
                            2 -> folder.delete()
                        }
                        Toast.makeText(this, R.string.accounts_operation_done, Toast.LENGTH_SHORT).show()
                    }
                    .buttonNegative(R.string.cancel)
                    .show()
            }
            alert.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
        return true
    }

    private fun updateUiWithUser(bot: Bot) {
        val avatar = runCatching {
            Drawable.createFromStream(URL(bot.avatarUrl).openStream(), "${bot.id}.png")
        }.getOrNull()
        // TODO 专门写一个登录成功后转跳到的主界面
        AlertDialog.Builder(this).setTitle("登录成功")
            .setCancelable(false)
            .let { builder -> avatar?.let { builder.setIcon(it) } ?: builder }
            .setMessage("""
                ${bot.id}: ${bot.nick}
                群聊数量: ${bot.groups.size}
                分组数量: ${bot.friendGroups.asCollection().size}
                好友数量: ${bot.friends.size}
                
                请到 Android/data/top.mrxiaom.mirai.aoki/files/AokiMirai/bots 复制设备信息
                点击 确定 退出登录
            """.trimIndent())
            .setPositiveButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun showLoginFailed(errorString: String) {
        AlertDialog.Builder(this).setTitle(R.string.login_failed)
            .setMessage(errorString)
            .setPositiveButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun smsSent(def: CompletableDeferred<String?>, sms: DeviceVerificationRequests.SmsRequest) {
        var lastRequested = 0L
        fun requestSms() {
            lastRequested = System.currentTimeMillis()
            loginViewModel.viewModelScope.launch {
                try {
                    sms.requestSms()
                } catch(t: Throwable) {
                    AlertDialog.Builder(this@LoginActivity).setTitle(R.string.captcha_sms_send_fail_title)
                        .setCancelable(false)
                        .setMessage(t.stackTraceToString())
                        .buttonPositive(R.string.ok) { }
                        .show()
                }
            }
        }

        val panel = LinearLayout(this@LoginActivity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val textCode = EditText(this@LoginActivity).also { panel.addView(it) }
        val timer = Timer()
        Button(this@LoginActivity).apply {
            val retry = text(R.string.captcha_sms_send_retry)
            text = retry
            isClickable = false
            setOnClickListener { requestSms() }
            panel.addView(this)

            // 更新剩余秒数
            timer.schedule(object : TimerTask() {
                override fun run() {
                    val diff = (System.currentTimeMillis() - lastRequested).milliseconds
                    isClickable = diff >= 1.minutes
                    text = "$retry${if (isClickable) "" else " (${1.minutes - diff})"}"
                }
            }, 1000L)
        }
        requestSms()
        AlertDialog.Builder(this@LoginActivity).setTitle(R.string.captcha_sms_send_title)
            .setCancelable(false)
            .setMessage(R.string.captcha_sms_send_message)
            .setView(panel)
            .buttonPositive(R.string.ok) {
                timer.cancel()
                def.complete(textCode.text.toString())
            }.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == 1 && resultCode == RESULT_OK) {
            AokiLoginSolver.scanDefList[data.getLongExtra("qq", 0)]?.complete(1)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
