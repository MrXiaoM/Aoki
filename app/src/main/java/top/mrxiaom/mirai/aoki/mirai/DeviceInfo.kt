package top.mrxiaom.mirai.aoki.mirai

import android.annotation.SuppressLint
import android.os.Build
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.*
import java.io.File

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
object AokiDeviceInfo {
    fun File.loadAsDeviceInfo(
        json: Json = DeviceInfoManager.format,
        default: () -> DeviceInfo = { DeviceInfo.random() }
    ): DeviceInfo {
        if (!this.exists() || this.length() == 0L) {
            return default().also {
                this.writeText(DeviceInfoManager.serialize(it, json))
            }
        }
        return DeviceInfoManager.deserialize(this.readText(), json)
    }

    fun BotConfiguration.fileBasedDeviceInfoAndroid(filepath: String = "device.json") {
        deviceInfo = {
            workingDir.resolve(filepath).loadAsDeviceInfo(default = {
                DeviceInfo.random().run {
                    DeviceInfo(
                        Build.DISPLAY.toByteArray(),
                        Build.PRODUCT.toByteArray(),
                        Build.DEVICE.toByteArray(),
                        Build.BOARD.toByteArray(),
                        Build.BRAND.toByteArray(),
                        Build.MODEL.toByteArray(),
                        Build.BOOTLOADER.toByteArray(),
                        Build.FINGERPRINT.toByteArray(),
                        bootId,
                        kernelInfo?.toByteArray() ?: procVersion,
                        if (baseBandVersion.isNotEmpty()) baseBandVersion.toByteArray() else baseBand,
                        DeviceInfo.Version(
                            Build.VERSION.INCREMENTAL.toByteArray(),
                            Build.VERSION.RELEASE.toByteArray(),
                            Build.VERSION.CODENAME.toByteArray(),
                            Build.VERSION.SDK_INT
                        ),
                        simInfo,
                        osType,
                        macAddress,
                        wifiBSSID,
                        wifiSSID,
                        imsiMd5,
                        imei,
                        apn
                    )
                }
            })
        }
    }
}

val baseBandVersion: String
    @SuppressLint("PrivateApi")
    get() = runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java, String::class.java).invoke(
                null, "gsm.version.baseband", ""
            )?.toString()
    }.getOrNull() ?: ""

val kernelInfo: String? = runCatching { MiraiFile.create("/proc/version").readText() }.getOrNull()
