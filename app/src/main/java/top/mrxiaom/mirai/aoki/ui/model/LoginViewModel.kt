package top.mrxiaom.mirai.aoki.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.utils.DeviceVerificationRequests

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
class QRLoginRequest(
    val bot: Bot,
    val qrcode: ByteArray?,
    val state: QRCodeLoginListener.State?
)
data class LoginResult(
    val success: Boolean,
    val bot: Bot,
    val error: Throwable? = null
)

class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult
    internal val _slideRequest = MutableLiveData<SlideRequest>()
    val slideRequest: LiveData<SlideRequest> = _slideRequest
    internal val _smsRequest = MutableLiveData<SmsRequest>()
    val smsRequest: LiveData<SmsRequest> = _smsRequest
    internal val _scanRequest = MutableLiveData<ScanRequest>()
    val scanRequest: LiveData<ScanRequest> = _scanRequest
    internal val _qrloginRequest = MutableLiveData<QRLoginRequest>()
    val qrloginRequest: LiveData<QRLoginRequest> = _qrloginRequest
    fun login(bot: Bot) = viewModelScope.launch {
        try {
            bot.login()
            _loginResult.value = LoginResult(true, bot)
        } catch (t: Throwable) {
            _loginResult.value = LoginResult(false, bot, t)
        }
    }
    fun cancelLogin(bot: Bot) {
        _loginResult.value = LoginResult(false, bot, UserCancelledLoginException())
        bot.close()
    }
}
class UserCancelledLoginException : InterruptedException()