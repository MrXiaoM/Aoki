package top.mrxiaom.mirai.aoki

import android.graphics.BitmapFactory
import android.os.Build
import android.util.DisplayMetrics
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CompletableDeferred
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.DeviceVerificationRequests
import net.mamoe.mirai.utils.DeviceVerificationResult
import net.mamoe.mirai.utils.LoginSolver
import top.mrxiaom.mirai.aoki.ui.LoginActivity
import top.mrxiaom.mirai.aoki.ui.model.QRLoginRequest
import top.mrxiaom.mirai.aoki.ui.model.ScanRequest
import top.mrxiaom.mirai.aoki.ui.model.SlideRequest
import top.mrxiaom.mirai.aoki.ui.model.SmsRequest
import top.mrxiaom.mirai.aoki.util.buttonPositive

object AokiLoginSolver : LoginSolver() {

    /**
     * 浏览器 UA，提取自 QQ 客户端 8.8.83
     *
     * 手机 QQ 内置浏览器访问 https://ie.icoa.cn/ 即可获取 UA
     */
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    val BotConfiguration.MiraiProtocol.userAgent: String
        get() {
            val display = DisplayMetrics()
            val screenHeight = display.heightPixels
            val pixel = display.widthPixels
            val meta = MiraiProtocolInternal[this]
            val qqVersion = meta.ver
            val version = qqVersion.split(".")
            val AVersion = version
            // 8.8.83_2654 暂时不明尾数的含义
            val SQVersion = "${version[0]}.${version[1]}.${version[2]}_2654"
            // 不清楚这个 appId 与 meta.id 是否相同，待验证
            val appId = 537114588
            return arrayOf(
                "Mozilla/5.0",
                "(Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID}; wv)",
                "AppleWebKit/537.36",
                "(KHTML, like Gecko)",
                "Version/4.0",
                "Chrome/105.0.5195.136",
                "Mobile",
                "Safari/537.36",
                "V1_AND_SQ_${SQVersion}_YYB_D",
                /*"A_$AVersion", // 待处理 */
                "QQ/$qqVersion",
                "NetType/4G",
                "WebP/0.4.1",
                "Pixel/$pixel",
                "StatusBarHeight/$screenHeight",
                "SimpleUISwitch/0",
                "QQTheme/2006078",
                "InMagicWin/0",
                "StudyMode/0",
                "CurrentMode/0",
                "CurrentFontScale/1.0",
                "GlobalDensityScale/0.9",
                "AppId/$appId"
            ).joinToString(" ")
        }
    val slideDefList = mutableMapOf<Long, CompletableDeferred<String>>()
    val smsDefList = mutableMapOf<Long, CompletableDeferred<String?>>()
    val scanDefList = mutableMapOf<Long, CompletableDeferred<Any>>()
    var loginActivity: LoginActivity? = null
    override val isSliderCaptchaSupported: Boolean = true

    /**
     * 图片验证码
     */
    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        // TODO: Not tested yet.
        loginActivity?.apply {
            val def = CompletableDeferred<String>()
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            ImageView(this).apply {
                setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.size))
            }
            val textCode = EditText(this).also { panel.addView(it) }
            AlertDialog.Builder(this).setTitle(R.string.captcha_pic_title)
                .setCancelable(false)
                .setMessage(R.string.captcha_pic_message)
                .setView(panel)
                .buttonPositive(R.string.ok) {
                    def.complete(textCode.text.toString())
                }.show()
            return def.await()
        }
        error("LoginActivity == null")
    }

    /**
     * 滑块验证
     */
    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String {
        val def = CompletableDeferred<String>().also { slideDefList[bot.id] = it }
        loginActivity?.runInUIThread {
            loginViewModel._slideRequest.value = SlideRequest(bot, url)
        }
        return def.await()
    }

    /**
     * 设备验证，包括手机号验证和扫码验证
     */
    override suspend fun onSolveDeviceVerification(
        bot: Bot,
        requests: DeviceVerificationRequests
    ): DeviceVerificationResult {
        loginActivity?.apply {
            requests.sms?.let {
                solveSms(bot, it)?.let { result -> return result }
            }
            requests.fallback?.let {
                return solveScan(bot, it)
            }
            error("User rejected SMS login while fallback login method not available.")
        }
        error("LoginActivity == null")
    }

    private suspend fun LoginActivity.solveScan(
        bot: Bot,
        fallback: DeviceVerificationRequests.FallbackRequest
    ): DeviceVerificationResult {
        val def = CompletableDeferred<Any>().also { scanDefList[bot.id] = it }
        runInUIThread {
            loginViewModel._scanRequest.value = ScanRequest(bot, fallback.url)
        }
        def.await()
        return fallback.solved()
    }

    suspend fun LoginActivity.solveSms(
        bot: Bot,
        sms: DeviceVerificationRequests.SmsRequest
    ): DeviceVerificationResult? {
        val def = CompletableDeferred<String?>().also { smsDefList[bot.id] = it }
        runInUIThread {
            loginViewModel._smsRequest.value = SmsRequest(bot, sms)
        }
        return def.await()?.let { sms.solved(it) }
    }

    /**
     * 二维码登录
     */
    override fun createQRCodeLoginListener(bot: Bot): QRCodeLoginListener = object : QRCodeLoginListener {
        override val qrCodeMargin: Int get() = 5
        override val qrCodeSize: Int get() = 2

        override fun onFetchQRCode(bot: Bot, data: ByteArray) {
            loginActivity?.runInUIThread {
                loginViewModel._qrloginRequest.value = QRLoginRequest(bot, data, null)
            }
        }

        override fun onStateChanged(bot: Bot, state: QRCodeLoginListener.State) {
            loginActivity?.runInUIThread {
                loginViewModel._qrloginRequest.value = QRLoginRequest(bot, null, state)
            }
        }
    }
}