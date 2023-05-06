package top.mrxiaom.mirai.aoki.ui

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.utils.BotConfiguration.HeartbeatStrategy
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol
import net.mamoe.mirai.utils.DeviceVerificationRequests
import top.mrxiaom.mirai.aoki.*
import top.mrxiaom.mirai.aoki.AokiLoginSolver.userAgent
import top.mrxiaom.mirai.aoki.ExceptionAnalyzer.analyze
import top.mrxiaom.mirai.aoki.databinding.ActivityLoginBinding
import top.mrxiaom.mirai.aoki.ui.dialog.LoginSolverDialog
import top.mrxiaom.mirai.aoki.ui.dialog.QRLoginDialog
import top.mrxiaom.mirai.aoki.ui.model.LoginViewModel
import top.mrxiaom.mirai.aoki.util.*
import xyz.cssxsh.mirai.tool.FixProtocolVersion
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

    private lateinit var loginSolverDialog: LoginSolverDialog
    private lateinit var qrloginDialog: QRLoginDialog
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
        loginSolverDialog = LoginSolverDialog(this, loginViewModel)
        qrloginDialog = QRLoginDialog(this, loginViewModel)

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
        // 接收登录结果
        observe(loginViewModel.loginResult) {
            loading.visibility = View.GONE
            if (!checkQRLogin.isChecked) password.visibility = View.VISIBLE
            if (success) {
                updateUiWithUser(bot)
            }
            else {
                val message = error?.run { error.analyze() + "\n" + error.stackTraceToString() } ?: "Login failed with no message."
                showLoginFailed(bot, message)
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
        if (!checkQRLogin.isChecked) password.visibility = View.INVISIBLE
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
                loginViewModel.login(bot)
                dismiss()
            }
            buttonNegative(R.string.cancel) {
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
            avatar?.let { setIcon(it) }
            setMessage(
                """
                请务必在导出文件后，等待半小时左右再登录，以免因为短时间内两次登录的 IP 归属地变动较大被风控。
                
                ${bot.id}: ${bot.nick}
                群聊数量: ${bot.groups.size}
                分组数量: ${bot.friendGroups.asCollection().size}
                好友数量: ${bot.friends.size}
                
                推荐同时在将要登录该账号的 mirai 中安装 fix-protocol-version，详见论坛官方公告。
                
                请到 Android/data/top.mrxiaom.mirai.aoki/files/AokiMirai/bots 复制设备信息。
                或者使用以下的按钮导出设备信息。
                点击 下方任意按钮 退出登录
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
    @SuppressLint("SetTextI18n")
    private fun showLoginFailed(bot: Bot, errorString: String) {
        dialog {
            setTitle(R.string.login_failed)
            setView(layout(R.layout.dialog_login_failed) {
                val info = findViewById<TextView>(R.id.dialog_login_failed_info)
                val log = findViewById<TextView>(R.id.dialog_login_failed_log)
                val version = packageManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS).versionName
                val loginType = BotManager.getLoginType(bot.id)
                info.text = """
                    Aoki $version Login Failed! (by $loginType)
                    Android ${Build.VERSION.RELEASE} 
                    Powered by Mirai ${BuildConstants.miraiVersion}
                    Protocol: ${bot.configuration.protocol}, hbStrategy: ${bot.configuration.heartbeatStrategy}
                """.trimIndent()
                val protocolVersions = FixProtocolVersion.info().map { "${it.key}: ${it.value}" }.joinToString("\n")
                log.text = "报错日志:\n$errorString\n\n协议信息:\n$protocolVersions"
            })
            buttonPositive(R.string.ok)
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
            loginSolverDialog.onScanResult(data)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
