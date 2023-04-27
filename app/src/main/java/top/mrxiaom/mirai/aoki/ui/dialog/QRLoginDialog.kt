package top.mrxiaom.mirai.aoki.ui.dialog

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.network.CustomLoginFailedException
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.ui.model.LoginViewModel
import top.mrxiaom.mirai.aoki.ui.model.QRLoginRequest
import top.mrxiaom.mirai.aoki.util.*

class QRLoginDialog(
    val activity: AppCompatActivity,
    val loginViewModel: LoginViewModel
) {
    init {
        activity.observe(loginViewModel.qrloginRequest) { pushInfo(this) }
    }
    private lateinit var image: ImageView
    private lateinit var info: TextView
    var imageRaw: ByteArray? = null
    val dialog: AlertDialog = activity.dialog {
        setTitle(R.string.qrlogin_title)
        setCancelable(false)
        setView(context.layout(R.layout.dialog_qrlogin) {
            image = findViewById<ImageView>(R.id.dialog_qrlogin_image).apply {
                setOnLongClickListener {
                    val image = imageRaw ?: return@setOnLongClickListener true.also {
                        Toast.makeText(context, R.string.qrlogin_save_failed, Toast.LENGTH_SHORT).show()
                    }
                    activity.requireExternalPermission {
                        saveQRCode(contentDescription.toString(), image)
                        Toast.makeText(context, R.string.qrlogin_saved, Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
            info = findViewById(R.id.dialog_qrlogin_info)
        })
        buttonNegative(R.string.cancel) {
            image.contentDescription.toString().toLongOrNull()?.also {
                val bot = Bot.getInstanceOrNull(it) ?: return@also
                loginViewModel.cancelLogin(bot)
            }
            dismiss()
        }
    }
    fun pushInfo(qrLoginRequest: QRLoginRequest) = qrLoginRequest.apply {
        qrcode?.apply {
            imageRaw = this
            image.setImage(this)
            image.contentDescription = bot.id.toString()
            if (!dialog.isShowing) dialog.show()
        }
        state?.apply {
            val message = when (this) {
                QRCodeLoginListener.State.WAITING_FOR_SCAN -> R.string.qrlogin_state_WAITING_FOR_SCAN
                QRCodeLoginListener.State.WAITING_FOR_CONFIRM -> R.string.qrlogin_state_WAITING_FOR_CONFIRM
                QRCodeLoginListener.State.CANCELLED -> R.string.qrlogin_state_CANCELLED
                QRCodeLoginListener.State.TIMEOUT -> R.string.qrlogin_state_TIMEOUT
                QRCodeLoginListener.State.CONFIRMED -> R.string.qrlogin_state_CONFIRMED.also { dialog.dismiss() }
                else -> null
            }
            if (message != null) info.text = activity.text(message)
        }
    }
}