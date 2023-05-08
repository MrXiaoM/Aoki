package top.mrxiaom.mirai.aoki.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.view.setPadding
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.youbenzi.mdtool.tool.MDTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.BotConfiguration.HeartbeatStrategy
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol
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
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class LoginActivity : AokiActivity<ActivityLoginBinding>(ActivityLoginBinding::class) {
    internal val loginViewModel = LoginViewModel()
    private lateinit var externalRoot: File

    private lateinit var qq: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var checkQRLogin: CheckBox
    private lateinit var accounts: Button
    private lateinit var loading: ProgressBar

    private lateinit var loginSolverDialog: LoginSolverDialog
    private lateinit var qrloginDialog: QRLoginDialog

    override fun onCreate(savedInstanceState: Bundle?) {
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

        super.onCreate(savedInstanceState)
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
                R.id.toolbar_settings -> startActivity<SettingsActivity>()
            }
            return@setOnMenuItemClickListener false
        }
        checkUpdate()
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
        qq.setOnEditorActionListener { _, actionId, _ ->
            if (checkQRLogin.isChecked && actionId == EditorInfo.IME_ACTION_DONE) login()
            false
        }
        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) login()
            false
        }
        login.setOnClickListener { login() }
        accounts.setOnClickListener { accountManage() }
        supportActionBar?.setDisplayUseLogoEnabled(true)
    }

    fun checkUpdate() = loginViewModel.viewModelScope.launch(Dispatchers.IO) {
        val config = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
        val enableUpdate = config.getBoolean("update_notice", true)

        @SuppressLint("SetTextI18n")
        fun log(s: String) = runInUIThread {
            logs.add(s)
            updateFooter()
        }
        log("cfg: " + config.all.map { "${it.key}=${it.value}" }.joinToString(", "))
        if (enableUpdate) {
            val isPreRelease = config.getString("update_channel", "release") == "pre-release"
            val proxy = config.getString("update_proxy", "https://ghproxy.com/") ?: ""
            val releaseUrl =
                URL("https://api.github.com/repos/MrXiaoM/Aoki/releases${if (isPreRelease) "?per_page=1" else "/latest"}")

                val connection = releaseUrl.openConnection().apply {
                    addRequestProperty("User-Agent", MiraiProtocol.ANDROID_PHONE.userAgent)
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    connect()
                }
                val raw = connection.getInputStream().readBytes().toString(Charsets.UTF_8)
                val json = if (raw.startsWith("["))
                    Json.parseToJsonElement(raw).jsonArrayOrNull?.getOrNull(0)?.jsonObject
                else Json.parseToJsonElement(raw).jsonObjectOrNull
                if (json == null) return@launch
                val tag = json["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@launch Unit.apply {
                    Toast.makeText(this@LoginActivity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                }
                if (!checkVersion(tag)) return@launch
                // 懒得获取资产，直接拼链接
                var url = "https://github.com/MrXiaoM/Aoki/releases/download/$tag/Aoki_$tag.apk"
                if (proxy.isNotEmpty()) url = proxy.removeSuffix("/") + "/" + url
                val body = json["body"]?.jsonPrimitive?.contentOrNull ?: "没有更新日志"
                dialogInUIThread {
                    setTitle(String.format(text(R.string.update_fetch_title), tag))
                    setView(ScrollView(context).apply {
                        setPadding(20)
                        addView(TextView(context).apply {
                            text = Html.fromHtml(MDTool.markdown2Html(body), 0)
                            movementMethod = LinkMovementMethod.getInstance()
                        })
                    })
                    buttonNeutral(R.string.update_fetch_download) {
                        val file = File(externalRoot, "Aoki_$tag.apk")
                        Toast.makeText(
                            this@LoginActivity,
                            String.format(text(R.string.update_download_start), file.absolutePath),
                            Toast.LENGTH_LONG
                        ).show()
                        var isCancelled = false
                        val progress = ProgressBar(context).apply {
                            max = 100
                        }
                        val progressText = TextView(context).apply {

                        }
                        val dialog = dialog {
                            setTitle(R.string.update_downloading_title)
                            setCancelable(false)
                            setView(LinearLayout(context).apply {
                                setPadding(20)
                                addView(progress)
                                addView(progressText)
                            })
                            buttonNegative(R.string.cancel) {
                                isCancelled = true
                            }
                        }
                        launch(Dispatchers.IO) {
                            log("正在下载 $url")
                            val downloadConnection = URL(url).openConnection().apply {
                                addRequestProperty("User-Agent", MiraiProtocol.ANDROID_PHONE.userAgent)
                                readTimeout = 30_000
                                connectTimeout = 30_000
                            }
                            kotlin.runCatching {
                                val dl = URL(url).openConnection() as HttpURLConnection
                                dl.connect()
                                val length = dl.contentLength
                                val input = dl.inputStream
                                val out = FileOutputStream(file)

                                var count = 0;
                                val buffer = ByteArray(1024)
                                while (!isCancelled) {
                                    val read = input.read(buffer)
                                    count += read
                                    // 计算进度条当前位置
                                    val prog = count / length * 100.0
                                    runInUIThread {
                                        progress.progress = prog.toInt()
                                        progressText.text = String.format("%.2f", prog) + "%"
                                    }
                                    // 下载完成
                                    if (read < 0) {
                                        dialog.dismiss()
                                        break
                                    }
                                    out.write(buffer, 0, read)
                                }
                                out.close()
                                input.close()
                            }
                                .onSuccess {
                                    Toast.makeText(
                                        this@LoginActivity, R.string.update_download_done, Toast.LENGTH_SHORT
                                    ).show()

                                    log("下载完成")
                                    // 打开 APK
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    val uri: Uri = FileProvider.getUriForFile(
                                        this@LoginActivity, "top.mrxiaom.mirai.aoki.fileprovider", file
                                    )
                                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                                    startActivity(intent)
                                }
                                .onFailure {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        String.format(
                                            text(R.string.update_download_failed),
                                            it.stackTraceToString()
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    throw it
                                }
                        }
                    }
                    buttonNegative(R.string.cancel)
                    buttonPositive(R.string.update_fetch_copy) { copy(url) }
                    show()
                }

        }
    }

    private fun checkVersion(remoteVersionRaw: String): Boolean {
        val remote = parseVersion(remoteVersionRaw.substring(1))
        val current = parseVersion(packageInfo.versionName)
        println("current: $current, remote: $remote")
        if (remote > current) return true
        if (remote.equals(current, false)) {
            println("remote == current")
            // 以下判断版本相同时测试情况
            val currentPre = current.pre
            val remotePre = remote.pre
            println("preC: $currentPre, preR: $remotePre")
            // 两者均为正式版不需要更新
            if (currentPre < 0 && remotePre < 0) return false
            // current 测试版，remote 正式版
            // 应当更新
            if (currentPre >= 0 && remotePre < 0) return true
            // current 正式版，remote 测试版
            // 不需要更新
            if (currentPre < 0) return false
            return currentPre < remotePre
        }
        return false
    }
    private val logs = mutableListOf<String>()
    private fun updateFooter() {
        binding.infomation.apply {
            text = """
                Aoki ${packageInfo.versionName}, mirai ${BuildConstants.miraiVersion}
                User Agent (${BotManager.defaultProtocol}): ${BotManager.defaultProtocol.userAgent}
                """.trimIndent() + logs.joinToString("\n", "\n")
        }
    }
    private fun login() {
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
        /*
        this@LoginActivity.dialog {
            setTitle(R.string.login_confirm)
            setCancelable(false)
            // TODO 编辑设备信息功能正在编写
            buttonNeutral(R.string.edit_device_action) {
                startActivity<EditDeviceInfoActivity> {
                    putExtra("qq", bot.id)
                }
                loading.visibility = View.INVISIBLE
                login.isClickable = true
                dismiss()
            }
            buttonPositive(R.string.ok) {
                loginViewModel.login(bot)
                dismiss()
            }
            buttonNegative(R.string.cancel) {
                loginViewModel.cancelLogin(bot)
                dismiss()
            }
        }.show()
         */
        loginViewModel.login(bot)
    }
    /**
     * 账号管理
     */
    private fun accountManage() = dialog {
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
                val loginType = BotManager.getLoginType(bot.id)
                info.text = """
                    Aoki ${packageInfo.versionName} Login Failed! (by $loginType)
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
