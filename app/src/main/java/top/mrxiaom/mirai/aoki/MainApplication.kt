package top.mrxiaom.mirai.aoki

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import top.mrxiaom.mirai.aoki.mirai.hotfixProtocolVersion
import java.security.Security

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
        hotfixProtocolVersion()
    }
}