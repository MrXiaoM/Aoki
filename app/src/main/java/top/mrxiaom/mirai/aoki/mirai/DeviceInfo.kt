package top.mrxiaom.mirai.aoki.mirai

import android.annotation.SuppressLint
import android.os.Build
import android.provider.Settings
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.*
import top.mrxiaom.mirai.aoki.MainApplication
import top.mrxiaom.mirai.aoki.util.mkdirsParent
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
    fun DeviceInfo.Companion.generateForAndroid(): DeviceInfo {
        return random().run {
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
                apn,
                androidId
            )
        }
    }

    /**
     * 相对于 AokiMirai/bots 目录读取设备信息文件
     */
    fun DeviceInfo.Companion.loadFromAoki(filepath: String): DeviceInfo {
        return File(MainApplication.externalRoot, "bots/$filepath").loadAsDeviceInfo { generateForAndroid() }
    }
    fun DeviceInfo.saveToAoki(filepath: String) {
        val file = File(MainApplication.externalRoot, "bots/$filepath")
        file.mkdirsParent()
        file.writeText(DeviceInfoManager.serialize(this, DeviceInfoManager.format))
    }
    fun DeviceInfo.saveV2ToAoki(filepath: String) {
        val file = File(MainApplication.externalRoot, "bots/$filepath")
        file.mkdirsParent()
        file.writeText(DeviceInfoManager.format.encodeToString(
            DeviceInfoManager.Wrapper.serializer(DeviceInfoManager.V2.serializer()),
            DeviceInfoManager.Wrapper(2, toV2Info())
        ))
    }
    private fun DeviceInfo.toV2Info(): DeviceInfoManager.V2 = DeviceInfoManager.V2(
        display.decodeToString(),
        product.decodeToString(),
        device.decodeToString(),
        board.decodeToString(),
        brand.decodeToString(),
        model.decodeToString(),
        bootloader.decodeToString(),
        fingerprint.decodeToString(),
        bootId.decodeToString(),
        procVersion.decodeToString(),
        DeviceInfoManager.HexString(baseBand),
        DeviceInfoManager.Version(
            version.incremental.decodeToString(),
            version.release.decodeToString(),
            version.codename.decodeToString(),
            version.sdk
        ),
        simInfo.decodeToString(),
        osType.decodeToString(),
        macAddress.decodeToString(),
        wifiBSSID.decodeToString(),
        wifiSSID.decodeToString(),
        DeviceInfoManager.HexString(imsiMd5),
        imei,
        apn.decodeToString(),
    )

    fun BotConfiguration.fileBasedDeviceInfoAndroid(filepath: String = "device.json") {
        deviceInfo = {
            workingDir.resolve(filepath).loadAsDeviceInfo { DeviceInfo.generateForAndroid() }
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
