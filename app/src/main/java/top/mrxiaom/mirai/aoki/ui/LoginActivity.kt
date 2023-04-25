package top.mrxiaom.mirai.aoki.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.utils.BotConfiguration.HeartbeatStrategy
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol
import net.mamoe.mirai.utils.DeviceVerificationRequests
import top.mrxiaom.mirai.aoki.*
import top.mrxiaom.mirai.aoki.AokiLoginSolver.userAgent
import top.mrxiaom.mirai.aoki.ExceptionAnalyzer.analyze
import top.mrxiaom.mirai.aoki.databinding.ActivityLoginBinding
import top.mrxiaom.mirai.aoki.ui.model.LoginViewModel
import top.mrxiaom.mirai.aoki.util.*
import java.io.File
import java.net.URL
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit


class LoginActivity : AppCompatActivity() {

    private lateinit var mHandler: Handler
    internal val loginViewModel = LoginViewModel()
    private lateinit var binding: ActivityLoginBinding
    private lateinit var externalRoot: File
    private lateinit var qq: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var checkQRLogin: CheckBox
    private lateinit var accounts: Button
    private lateinit var loading: ProgressBar
    private lateinit var qrcodeImage: ImageView
    private lateinit var qrcodeInfo: TextView
    private lateinit var qrloginDialog: AlertDialog
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
            } catch (_: Throwable) {
                // 收声
            }
            handler?.uncaughtException(p0, e)
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        AokiLoginSolver.loginActivity = this

        needPermission(
            1,
            Manifest.permission.INTERNET
        )

        qrloginDialog = dialog {
            setTitle(R.string.qrlogin_title)
            setCancelable(false)
            setView(layout(R.layout.dialog_qrlogin) {
                qrcodeImage = findViewById<ImageView>(R.id.dialog_qrlogin_image).apply {
                    setOnLongClickListener {
                        // TODO: 保存二维码图片到本地
                        Toast.makeText(this@LoginActivity, R.string.qrlogin_saved, Toast.LENGTH_SHORT).show()
                        true
                    }
                }
                qrcodeInfo = findViewById(R.id.dialog_qrlogin_info)
            })
            buttonNegative(R.string.cancel) {
                // TODO 取消登录
                dismiss()
            }
        }

        this.qq = binding.qq
        this.password = binding.password
        this.login = binding.login
        this.checkQRLogin = binding.checkQrlogin
        this.accounts = binding.accounts
        this.loading = binding.loading
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.toolbar_about -> startActivity<AboutActivity>()
            }
            return@setOnMenuItemClickListener false
        }
        updateFooter()
        checkQRLogin.setOnCheckedChangeListener { _, isChecked ->
            password.visibility = if (isChecked) View.INVISIBLE else View.VISIBLE
        }
        setupDropdownBox(
            binding.protocol,
            R.array.spinner_protocol,
            MiraiProtocol.ANDROID_PHONE,
            MiraiProtocol.ANDROID_PAD,
            MiraiProtocol.ANDROID_WATCH,
            MiraiProtocol.IPAD,
            MiraiProtocol.MACOS
        ) { _, protocol ->
            BotManager.defaultProtocol = protocol
            updateFooter()
        }
        setupDropdownBox(
            binding.hbStrategy,
            R.array.spinner_hb_strategy,
            HeartbeatStrategy.STAT_HB,
            HeartbeatStrategy.REGISTER,
            HeartbeatStrategy.NONE
        ) { _, hbStrategy -> BotManager.defaultHbStrategy = hbStrategy }
        // 滑块验证请求
        loginViewModel.slideRequest.observe(this) {
            it?.apply {
                startActivity<SlideActivity> {
                    putExtra("qq", bot.id)
                    putExtra("url", url)
                    putExtra("ua", bot.configuration.protocol.userAgent)
                }
            }
        }
        // 扫码验证请求
        loginViewModel.scanRequest.observe(this) {
            it?.apply {
                startActivityForResult<ScanActivity>(1) {
                    putExtra("qq", bot.id)
                    putExtra("url", url)
                    putExtra("ua", bot.configuration.protocol.userAgent)
                }
            }
        }
        // 短信验证码请求
        loginViewModel.smsRequest.observe(this@LoginActivity) {
            it?.apply {
                val def = AokiLoginSolver.smsDefList[bot.id] ?: return@observe
                val phoneNumberFull = sms.run {
                    if (countryCode != null && phoneNumber != null)
                        text(R.string.captcha_sms_request_phoneNumber)
                            .replace("\$countryCode", countryCode.toString())
                            .replace("\$phoneNumber", phoneNumber.toString())
                    else text(R.string.captcha_sms_request_phoneNumberNull)
                }
                AlertDialog.Builder(this@LoginActivity).setTitle(R.string.captcha_sms_request_title)
                    .setCancelable(false)
                    .setMessage(R.string.captcha_sms_request_message) { text ->
                        text.replace(
                            "\$phoneNumberFull",
                            phoneNumberFull
                        )
                    }
                    .buttonPositive(R.string.captcha_sms_request_send) {
                        smsSent(def, sms)
                    }.buttonNegative(R.string.captcha_sms_request_other) {
                        def.complete(null)
                    }.show()
            }
        }
        // 扫码登录
        loginViewModel.qrloginRequest.observe(this) {
            it?.apply {
                it.qrcode?.apply {
                    qrcodeImage.setImage(this)
                    if (!qrloginDialog.isShowing) qrloginDialog.show()
                }
                it.state?.apply {
                    val message = when (state) {
                        QRCodeLoginListener.State.WAITING_FOR_SCAN -> R.string.qrlogin_state_WAITING_FOR_SCAN
                        QRCodeLoginListener.State.WAITING_FOR_CONFIRM -> R.string.qrlogin_state_WAITING_FOR_CONFIRM
                        QRCodeLoginListener.State.CANCELLED -> R.string.qrlogin_state_CANCELLED
                        QRCodeLoginListener.State.TIMEOUT -> R.string.qrlogin_state_TIMEOUT
                        QRCodeLoginListener.State.CONFIRMED -> R.string.qrlogin_state_CONFIRMED.also { qrloginDialog.dismiss() }
                        else -> null
                    }
                    if (message != null)
                        qrcodeInfo.text = text(message)
                }
            }
        }
        // 接收登录结果
        loginViewModel.loginResult.observe(this) {
            val loginResult = it ?: return@observe

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error.analyze() + "\n" + loginResult.error.stackTraceToString())
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            login.isClickable = true
        }
        qq.setOnEditorActionListener { _, actionId, _ ->
            if (checkQRLogin.isChecked && actionId == EditorInfo.IME_ACTION_DONE) login()
            false
        }
        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) login()
            false
        }
        login.setOnClickListener { login() }
        accounts.setOnClickListener(this::accountManage)
        supportActionBar?.setDisplayUseLogoEnabled(true)
    }
    private fun <T> setupDropdownBox(
        dropdownBox: Spinner,
        @IdRes array: Int,
        vararg values: T,
        onSelected: (i: Int, value: T) -> Unit
    ) {
        dropdownBox.adapter =
            ArrayAdapter.createFromResource(this, array, android.R.layout.simple_spinner_item)
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        dropdownBox.onItemSelected {
            if (it < 0) return@onItemSelected
            onSelected(it, values[it])
        }
    }
    private fun updateFooter() {
        binding.infomation.apply {
            val version = packageManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS).versionName

            text = """
                Aoki $version, mirai ${BuildConstants.miraiVersion}
                User Agent (${BotManager.defaultProtocol}): ${BotManager.defaultProtocol.userAgent}
                """.trimIndent()
        }
    }
    private fun login() {
        if (TextUtils.isEmpty(qq.text.toString()) || (!checkQRLogin.isChecked && TextUtils.isEmpty(password.text.toString()))) {
            Toast.makeText(this, R.string.tips_not_complete, Toast.LENGTH_SHORT).show()
            return
        }

        loading.visibility = View.VISIBLE
        login.isClickable = false
        val bot = if (checkQRLogin.isChecked) BotManager.newBotQRLogin(
            externalRoot,
            qq.text.toString().toLong()
        )
        else BotManager.newBot(
            externalRoot,
            qq.text.toString().toLong(),
            password.text.toString()
        )
        AlertDialog.Builder(this@LoginActivity)
            .setTitle(R.string.login_confirm)
            .setCancelable(false)
            .buttonNeutral(R.string.edit_device_action) {
                startActivity<EditDeviceInfoActivity> {
                    putExtra("qq", bot.id)
                }
                loading.visibility = View.INVISIBLE
                login.isClickable = true
                dismiss()
            }
            .buttonPositive(R.string.ok) {
                loginViewModel.viewModelScope.launch { loginViewModel.login(bot) }
                dismiss()
            }
            .buttonNegative(R.string.cancel) {
                loading.visibility = View.INVISIBLE
                login.isClickable = true
                dismiss()
            }
            .show()
    }
    /**
     * 账号管理
     */
    private fun accountManage(view: View) {
        val alert = AlertDialog.Builder(this)
            .setTitle(R.string.accounts_title)
            .buttonNegative(R.string.cancel)
        val accountList = File(externalRoot, "bots").listFiles()?.mapNotNull {
            it.name.toLongOrNull()
        }?.map { it.toString() }?.toTypedArray()
        if (!accountList.isNullOrEmpty()) alert.setItems(accountList) { topDialog, i ->
            val account = accountList[i]
            val folder = File(externalRoot, "bots/$account")
            AlertDialog.Builder(this).setTitle(account)
                .setItems(R.array.accounts_operation) { dialog, btn ->
                    when (btn) {
                        0 -> shareAccount(account)
                        1 -> folder.delFolder("device.json")
                        2 -> folder.delFolder("cache")
                        //TODO 并未发现以session开头的文件，故此行代码不做改动
                        3 -> deleteSession(File(folder, "cache"))
                        4 -> delFolder(folder)
                    }
                    Toast.makeText(this, R.string.accounts_operation_done, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    topDialog.dismiss()
                }
                .buttonNegative(R.string.cancel)
                .show()
        }
        alert.show()
    }

    /**
     * 删除 session 开头的文件
     **/
    private fun deleteSession(cacheDir: File) {
        cacheDir.listFiles { it -> it?.name?.startsWith("session", true) ?: false }?.forEach { it?.delete() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
        return true
    }

    /**
     * 处理登录成功
     */
    private fun updateUiWithUser(bot: Bot) {
        val avatar = runCatching {
            Drawable.createFromStream(URL(bot.avatarUrl).openStream(), "${bot.id}.png")
        }.getOrNull()
        // TODO 专门写一个登录成功后转跳到的主界面
        AlertDialog.Builder(this).setTitle("登录成功")
            .setCancelable(false)
            .let { builder -> avatar?.let { builder.setIcon(it) } ?: builder }
            .setMessage(
                """
                ${bot.id}: ${bot.nick}
                群聊数量: ${bot.groups.size}
                分组数量: ${bot.friendGroups.asCollection().size}
                好友数量: ${bot.friends.size}
                
                请到 Android/data/top.mrxiaom.mirai.aoki/files/AokiMirai/bots 复制设备信息
                点击 确定 退出登录
            """.trimIndent()
            )
            .buttonPositive(R.string.ok) { bot.close() }
            .buttonNegative(R.string.accounts_operation_export) {
                val account = bot.id
                bot.close()
                shareAccount(account)
            }
            .show()
    }

    /**
     * 处理登录失败
     */
    private fun showLoginFailed(errorString: String) {
        AlertDialog.Builder(this).setTitle(R.string.login_failed)
            .setMessage(errorString)
            .buttonPositive(R.string.ok) { }
            .show()
    }

    /**
     * 处理发送短信验证码
     */
    private fun smsSent(def: CompletableDeferred<String?>, sms: DeviceVerificationRequests.SmsRequest) {
        var lastRequested = 0L
        fun requestSms() {
            lastRequested = System.currentTimeMillis()
            loginViewModel.viewModelScope.launch {
                try {
                    sms.requestSms()
                } catch (t: Throwable) {
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
                    text = "$retry${if (isClickable) "" else " (${(1.minutes - diff).toInt(DurationUnit.SECONDS)})"}"
                }
            }, 1000L, 1000L)
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

    /**
     * 导出账户并分享
     */
    private fun shareAccount(account: Any) {
        try {
            val src = File(externalRoot, "bots/$account")
            val zip = File(externalRoot, "export/$account.zip")
            if (zip.exists()) zip.delete()
            zip(src, zip)
            share("$account.zip")
        } catch (t: Throwable) {
            runInUIThread {
                Toast.makeText(this, t.stackTraceToString(), Toast.LENGTH_LONG)
            }
        }
    }

    /**
     * 分享在 AokiMirai/export 下的文件
     */
    private fun share(fileName: String) {
        runInUIThread {
            val share = File(getExternalFilesDir(null), "AokiMirai/export/$fileName")
            val uri = FileProvider.getUriForFile(this@LoginActivity, "top.mrxiaom.mirai.aoki.fileprovider", share)

            val intent = Intent(Intent.ACTION_SEND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            startActivity(Intent.createChooser(intent, "分享"))
        }
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
