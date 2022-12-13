package top.mrxiaom.mirai.aoki.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import top.mrxiaom.mirai.aoki.BotManager
import top.mrxiaom.mirai.aoki.ScanRequest
import top.mrxiaom.mirai.aoki.SlideRequest
import top.mrxiaom.mirai.aoki.SmsRequest
import java.io.File

data class LoginResult(
    val success: Bot? = null,
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
    suspend fun login(root: File, qq: Long, password: String) {
        try {
            val bot = BotManager.newBot(root, qq, password).alsoLogin()
            _loginResult.value = LoginResult(success = bot)
        } catch (t: Throwable) {
            _loginResult.value = LoginResult(error = t)
        }
    }
}