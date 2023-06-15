package top.mrxiaom.mirai.aoki

import android.app.Application
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.Services
import org.bouncycastle.jce.provider.BouncyCastleProvider
import top.mrxiaom.mirai.aoki.mirai.EncryptProvider
import top.mrxiaom.mirai.aoki.util.mkdirsQuietly
import xyz.cssxsh.mirai.tool.FixProtocolVersion
import java.io.File
import java.security.Security
import kotlin.reflect.jvm.jvmName

class MainApplication : Application() {
    private fun setUpECDHEnvironment() {
        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (provider != null) {
            if (provider.javaClass != BouncyCastleProvider::class.java) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        externalRoot = File(getExternalFilesDir(null), "AokiMirai").also { it.mkdirsQuietly() }
        setUpECDHEnvironment()
        FixProtocolVersion.update()
        syncProtocolVersions()
        Services.registerAsOverride("net.mamoe.mirai.internal.spi.EncryptService", EncryptProvider::class.jvmName) { EncryptProvider }
    }
    companion object {
        internal lateinit var externalRoot: File
        fun syncProtocolVersions(): Int {
            var count = 0
            for (protocol in BotConfiguration.MiraiProtocol.values()) {
                val file = File(externalRoot, "$protocol.json")
                if (!file.exists()) continue
                try {
                    val json = Json.parseToJsonElement(file.readText()).jsonObject
                    FixProtocolVersion.sync(protocol, json)
                    count ++
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            return count
        }
    }
}