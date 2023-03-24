package top.mrxiaom.mirai.aoki.mirai

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
                        kernelInfo.toByteArray(),
                        baseBandVersion?.toByteArray() ?: baseBand,
                        version,
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

val baseBandVersion: String?
    get() = runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java, String::class.java).invoke(
                null, "gsm.version.baseband", null
            )?.toString()
    }.getOrNull()

val kernelInfo: String = android.system.Os.uname().release
