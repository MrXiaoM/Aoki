package top.mrxiaom.mirai.aoki.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
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
import net.mamoe.mirai.network.CustomLoginFailedException
import net.mamoe.mirai.utils.BotConfiguration.HeartbeatStrategy
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol
import net.mamoe.mirai.utils.DeviceVerificationRequests
import top.mrxiaom.mirai.aoki.*
import top.mrxiaom.mirai.aoki.AokiLoginSolver.userAgent
import top.mrxiaom.mirai.aoki.ExceptionAnalyzer.analyze
import top.mrxiaom.mirai.aoki.databinding.ActivityLoginBinding
import top.mrxiaom.mirai.aoki.mirai.baseBandVersion
import top.mrxiaom.mirai.aoki.mirai.kernelInfo
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
    var qrcodeImageRaw: ByteArray? = null
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
                        val image = qrcodeImageRaw ?: return@setOnLongClickListener true.also {
                            Toast.makeText(this@LoginActivity, R.string.qrlogin_save_failed, Toast.LENGTH_SHORT).show()
                        }
                        requireExternalPermission {
                            saveQRCode(contentDescription.toString(), image)
                            Toast.makeText(this@LoginActivity, R.string.qrlogin_saved, Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                }
                qrcodeInfo = findViewById(R.id.dialog_qrlogin_info)
            })
            buttonNegative(R.string.cancel) {
                qrcodeImage.contentDescription.toString().toLongOrNull()?.also {
                    Bot.getInstanceOrNull(it)?.close(object: CustomLoginFailedException(true, "用户主动取消登录") { })
                }
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
            if (isChecked && !BotManager.defaultProtocol.isSupportQRLogin) {
                binding.protocol.post {
                    binding.protocol.setSelection(2)
                }
            }
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
        observe(loginViewModel.slideRequest) {
            startActivity<SlideActivity> {
                putExtra("qq", bot.id)
                putExtra("url", url)
                putExtra("ua", bot.configuration.protocol.userAgent)
            }
        }
        // 扫码验证请求
        observe(loginViewModel.scanRequest) {
            startActivityForResult<ScanActivity>(1) {
                putExtra("qq", bot.id)
                putExtra("url", url)
                putExtra("ua", bot.configuration.protocol.userAgent)
            }
        }
        // 短信验证码请求
        observe(loginViewModel.smsRequest) {
            val def = AokiLoginSolver.smsDefList[bot.id] ?: return@observe
            val phoneNumberFull = sms.run {
                if (countryCode != null && phoneNumber != null)
                    text(R.string.captcha_sms_request_phoneNumber)
                        .replace("\$countryCode", countryCode.toString())
                        .replace("\$phoneNumber", phoneNumber.toString())
                else text(R.string.captcha_sms_request_phoneNumberNull)
            }
            dialog {
                setTitle(R.string.captcha_sms_request_title)
                setCancelable(false)
                setMessage(R.string.captcha_sms_request_message) { text ->
                    text.replace(
                        "\$phoneNumberFull",
                        phoneNumberFull
                    )
                }
                buttonPositive(R.string.captcha_sms_request_send) {
                    smsSent(def, sms)
                }
                buttonNegative(R.string.captcha_sms_request_other) {
                    def.complete(null)
                }
            }.show()
        }
        // 扫码登录
        observe(loginViewModel.qrloginRequest) {
            qrcode?.apply {
                qrcodeImageRaw = this
                qrcodeImage.setImage(this)
                qrcodeImage.contentDescription = bot.id.toString()
                if (!qrloginDialog.isShowing) qrloginDialog.show()
            }
            state?.apply {
                val message = when (this) {
                    QRCodeLoginListener.State.WAITING_FOR_SCAN -> R.string.qrlogin_state_WAITING_FOR_SCAN
                    QRCodeLoginListener.State.WAITING_FOR_CONFIRM -> R.string.qrlogin_state_WAITING_FOR_CONFIRM
                    QRCodeLoginListener.State.CANCELLED -> R.string.qrlogin_state_CANCELLED
                    QRCodeLoginListener.State.TIMEOUT -> R.string.qrlogin_state_TIMEOUT
                    QRCodeLoginListener.State.CONFIRMED -> R.string.qrlogin_state_CONFIRMED.also { qrloginDialog.dismiss() }
                    else -> null
                }
                if (message != null) qrcodeInfo.text = text(message)
            }
        }
        // 接收登录结果
        observe(loginViewModel.loginResult) {
            loading.visibility = View.GONE
            if (!checkQRLogin.isChecked) password.visibility = View.VISIBLE
            if (error != null) {
                showLoginFailed(error.analyze() + "\n" + error.stackTraceToString())
            }
            if (success != null) {
                updateUiWithUser(success)
            }
            qq.isClickable = true
            password.isClickable = true
            login.isClickable = true
            checkQRLogin.isClickable = true
        }
        qq.setOnEditorActionListener { view, actionId, _ ->
            if (checkQRLogin.isChecked && actionId == EditorInfo.IME_ACTION_DONE) login(view)
            false
        }
        password.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) login(view)
            false
        }
        login.setOnClickListener(this::login)
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
    private fun login(view: View) {
        if (checkQRLogin.isChecked && !BotManager.defaultProtocol.isSupportQRLogin) {
            Toast.makeText(this, R.string.tips_not_support_qrlogin, Toast.LENGTH_LONG).show()
            return
        }
        if (TextUtils.isEmpty(qq.text.toString()) || (!checkQRLogin.isChecked && TextUtils.isEmpty(password.text.toString()))) {
            Toast.makeText(this, R.string.tips_not_complete, Toast.LENGTH_SHORT).show()
            return
        }

        loading.visibility = View.VISIBLE
        if (!checkQRLogin.isChecked) password.visibility = View.GONE
        qq.isClickable = false
        password.isClickable = false
        login.isClickable = false
        checkQRLogin.isClickable = false
        val bot = if (checkQRLogin.isChecked) BotManager.newBotQRLogin(
            externalRoot,
            qq.text.toString().toLong()
        )
        else BotManager.newBot(
            externalRoot,
            qq.text.toString().toLong(),
            password.text.toString()
        )
        this@LoginActivity.dialog {
            setTitle(R.string.login_confirm)
            setCancelable(false)
            // TODO 编辑设备信息功能正在编写
            /*
            buttonNeutral(R.string.edit_device_action) {
                startActivity<EditDeviceInfoActivity> {
                    putExtra("qq", bot.id)
                }
                loading.visibility = View.INVISIBLE
                login.isClickable = true
                dismiss()
            }
            */
            buttonPositive(R.string.ok) {
                loginViewModel.viewModelScope.launch { loginViewModel.login(bot) }
                dismiss()
            }
            buttonNegative(R.string.cancel) {
                loading.visibility = View.INVISIBLE
                login.isClickable = true
                loginViewModel.cancelLogin(bot)
                dismiss()
            }
        }.show()
    }
    /**
     * 账号管理
     */
    private fun accountManage(view: View) = dialog {
        setTitle(R.string.accounts_title)
        buttonNegative(R.string.cancel)
        val accountList = File(externalRoot, "bots").listFiles()?.mapNotNull {
            it.name.toLongOrNull()?.toString()
        }?.toTypedArray()
        if (!accountList.isNullOrEmpty()) setItems(accountList) { topDialog, i ->
            val account = accountList[i]
            val folder = File(externalRoot, "bots/$account")
            context.dialog {
                setTitle(account)
                setItems(R.array.accounts_operation) { dialog, btn ->
                    when (btn) {
                        0 -> shareAccount(account)
                        1 -> folder.delFolder("device.json")
                        2 -> folder.delFolder("cache")
                        //TODO 并未发现以session开头的文件，故此行代码不做改动
                        3 -> deleteSession(File(folder, "cache"))
                        4 -> delFolder(folder)
                    }
                    Toast.makeText(context, R.string.accounts_operation_done, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    topDialog.dismiss()
                }
                buttonNegative(R.string.cancel)
            }.show()
        }
    }.show()

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
        dialog {
            setTitle("登录成功")
            setCancelable(false)
            let { builder -> avatar?.let { builder.setIcon(it) } ?: builder }
            setMessage(
                """
                ${bot.id}: ${bot.nick}
                群聊数量: ${bot.groups.size}
                分组数量: ${bot.friendGroups.asCollection().size}
                好友数量: ${bot.friends.size}
                
                请到 Android/data/top.mrxiaom.mirai.aoki/files/AokiMirai/bots 复制设备信息
                点击 确定 退出登录
            """.trimIndent()
            )
            buttonPositive(R.string.ok) { bot.close() }
            buttonNegative(R.string.accounts_operation_export) {
                val account = bot.id
                bot.close()
                shareAccount(account)
            }
        }.show()
    }

    /**
     * 处理登录失败
     */
    private fun showLoginFailed(errorString: String) {
        dialog {
            setTitle(R.string.login_failed)
            setMessage(errorString)
            buttonPositive(R.string.ok)
        }.show()
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
                    dialog {
                        setTitle(R.string.captcha_sms_send_fail_title)
                        setCancelable(false)
                        setMessage(t.stackTraceToString())
                        buttonPositive(R.string.ok)
                    }.show()
                }
            }
        }
        dialog {
            setTitle(R.string.captcha_sms_send_title)
            setCancelable(false)
            setMessage(R.string.captcha_sms_send_message)

            val timer = Timer()
            val textCode = EditText(context)
            requestSms()
            setView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(textCode)
                Button(context).apply {
                    val retry = text(R.string.captcha_sms_send_retry)
                    text = retry
                    isClickable = false
                    setOnClickListener { requestSms() }
                    addView(this)

                    // 更新剩余秒数
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            val diff = (System.currentTimeMillis() - lastRequested).milliseconds
                            isClickable = diff >= 1.minutes
                            text =
                                "$retry${if (isClickable) "" else " (${(1.minutes - diff).toInt(DurationUnit.SECONDS)})"}"
                        }
                    }, 1000L, 1000L)
                }
            })
            buttonPositive(R.string.ok) {
                timer.cancel()
                def.complete(textCode.text.toString())
            }
            buttonNegative(R.string.cancel) {
                timer.cancel()
                def.complete(null)
            }
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
