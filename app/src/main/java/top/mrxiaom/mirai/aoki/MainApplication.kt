package top.mrxiaom.mirai.aoki

import android.app.Application
import net.mamoe.mirai.utils.Services
import org.bouncycastle.jce.provider.BouncyCastleProvider
import top.mrxiaom.mirai.aoki.mirai.EncryptProvider
import xyz.cssxsh.mirai.tool.FixProtocolVersion
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
        setUpECDHEnvironment()
        FixProtocolVersion.update()
        Services.registerAsOverride("net.mamoe.mirai.internal.spi.EncryptService", EncryptProvider::class.jvmName) { EncryptProvider }
    }
}