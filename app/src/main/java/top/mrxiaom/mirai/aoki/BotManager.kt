package top.mrxiaom.mirai.aoki

import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.BotConfiguration
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfo.fileBasedDeviceInfoAndroid
import top.mrxiaom.mirai.aoki.util.mkdirsQuietly
import java.io.File

object BotManager {
    var defaultProtocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
    var defaultHbStrategy = BotConfiguration.HeartbeatStrategy.STAT_HB

    fun newBot(root: File, qq: Long, password: String, conf: BotConfiguration.() -> Unit = {}): Bot =
        BotFactory.newBot(qq, password) {
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
}