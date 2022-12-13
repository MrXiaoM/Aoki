package top.mrxiaom.mirai.aoki

import android.graphics.BitmapFactory
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CompletableDeferred
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.DeviceVerificationRequests
import net.mamoe.mirai.utils.DeviceVerificationResult
import net.mamoe.mirai.utils.LoginSolver
import top.mrxiaom.mirai.aoki.U.buttonPositive
import top.mrxiaom.mirai.aoki.ui.LoginActivity

class SlideRequest(
    val bot: Bot,
    val url: String
)

class SmsRequest(
    val bot: Bot,
    val sms: DeviceVerificationRequests.SmsRequest
)

class ScanRequest(
    val bot: Bot,
    val url: String
)

object AokiLoginSolver : LoginSolver() {
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
}