package top.mrxiaom.mirai.aoki.ui.dialog

import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.DeviceVerificationRequests
import top.mrxiaom.mirai.aoki.AokiLoginSolver
import top.mrxiaom.mirai.aoki.AokiLoginSolver.userAgent
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.ui.ScanActivity
import top.mrxiaom.mirai.aoki.ui.SlideActivity
import top.mrxiaom.mirai.aoki.ui.model.LoginViewModel
import top.mrxiaom.mirai.aoki.ui.model.ScanRequest
import top.mrxiaom.mirai.aoki.ui.model.SlideRequest
import top.mrxiaom.mirai.aoki.ui.model.SmsRequest
import top.mrxiaom.mirai.aoki.util.*
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class LoginSolverDialog(
    val activity: AppCompatActivity,
    loginViewModel: LoginViewModel,
    private val scope: CoroutineScope = loginViewModel.viewModelScope
) {
    init {
        activity.observe(loginViewModel.slideRequest) { onSlideRequest(this) }
        activity.observe(loginViewModel.scanRequest) { onScanRequest(this) }
        activity.observe(loginViewModel.smsRequest) { onSmsRequrst(this) }
    }

    fun onSlideRequest(slideRequest: SlideRequest) = slideRequest.apply {
        activity.startActivity<SlideActivity> {
            putExtra("qq", bot.id)
            putExtra("url", url)
            putExtra("ua", bot.configuration.protocol.userAgent)
        }
    }

    fun onScanRequest(scanRequest: ScanRequest) = scanRequest.apply {
        activity.startActivityForResult<ScanActivity>(1) {
            putExtra("qq", bot.id)
            putExtra("url", url)
            putExtra("ua", bot.configuration.protocol.userAgent)
        }
    }

    fun onScanResult(data: Intent) {
        AokiLoginSolver.scanDefList[data.getLongExtra("qq", 0)]?.complete(1)
    }

    fun onSmsRequrst(smsRequest: SmsRequest) = smsRequest.apply {
        val def = AokiLoginSolver.smsDefList[bot.id] ?: return@apply
        val phoneNumberFull = sms.run {
            if (countryCode != null && phoneNumber != null)
                activity.text(R.string.captcha_sms_request_phoneNumber)
                    .replace("\$countryCode", countryCode.toString())
                    .replace("\$phoneNumber", phoneNumber.toString())
            else activity.text(R.string.captcha_sms_request_phoneNumberNull)
        }
        activity.dialog {
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

    /**
     * 处理发送短信验证码
     */
    private fun smsSent(def: CompletableDeferred<String?>, sms: DeviceVerificationRequests.SmsRequest) {
        var lastRequested = 0L
        fun requestSms() {
            lastRequested = System.currentTimeMillis()
            scope.launch {
                try {
                    sms.requestSms()
                } catch (t: Throwable) {
                    activity.dialog {
                        setTitle(R.string.captcha_sms_send_fail_title)
                        setCancelable(false)
                        setMessage(t.stackTraceToString())
                        buttonPositive(R.string.ok)
                    }.show()
                }
            }
        }
        activity.dialog {
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
                    val retry = activity.text(R.string.captcha_sms_send_retry)
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

}