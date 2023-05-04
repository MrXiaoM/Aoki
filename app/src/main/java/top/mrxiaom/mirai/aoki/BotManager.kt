package top.mrxiaom.mirai.aoki

import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.utils.BotConfiguration
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfo.fileBasedDeviceInfoAndroid
import top.mrxiaom.mirai.aoki.util.mkdirsQuietly
import java.io.File

object BotManager {
    var defaultProtocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
    var defaultHbStrategy = BotConfiguration.HeartbeatStrategy.STAT_HB
    private val tempLoginType = mutableMapOf<Long, String>()
    fun newBotQRLogin(root: File, qq: Long, conf: BotConfiguration.() -> Unit = {}): Bot =
        BotFactory.newBot(qq, BotAuthorization.byQRCode()) {
            tempLoginType[qq] = "QRCode"
            workingDir = File(root, "bots/$qq").also { it.mkdirsQuietly() }
            fileBasedDeviceInfoAndroid()
            protocol = defaultProtocol
            heartbeatStrategy = defaultHbStrategy
            cacheDir = workingDir.resolve("cache").also { it.mkdirsQuietly() }
            redirectBotLogToDirectory(File(workingDir, "logs").also { it.mkdirsQuietly() })
            redirectNetworkLogToDirectory(File(workingDir, "logs"))
            loginCacheEnabled = true
            loginSolver = AokiLoginSolver
            conf()
        }
    fun newBot(root: File, qq: Long, password: String, conf: BotConfiguration.() -> Unit = {}): Bot =
        BotFactory.newBot(qq, BotAuthorization.byPassword(password)) {
            tempLoginType[qq] = "Password"
            workingDir = File(root, "bots/$qq").also { it.mkdirsQuietly() }
            fileBasedDeviceInfoAndroid()
            protocol = defaultProtocol
            heartbeatStrategy = defaultHbStrategy
            cacheDir = workingDir.resolve("cache").also { it.mkdirsQuietly() }
            redirectBotLogToDirectory(File(workingDir, "logs").also { it.mkdirsQuietly() })
            redirectNetworkLogToDirectory(File(workingDir, "logs"))
            loginCacheEnabled = true
            loginSolver = AokiLoginSolver
            conf()
        }

    fun getLoginType(qq: Long): String = tempLoginType[qq] ?: "UNKNOWN"
}