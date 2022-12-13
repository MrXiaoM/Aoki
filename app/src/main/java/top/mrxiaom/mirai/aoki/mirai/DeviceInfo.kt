package top.mrxiaom.mirai.aoki.mirai

import android.os.Build
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.utils.*
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfoManager.Version.Companion.trans
import java.io.File

/**
 * https://github.com/mamoe/mirai/blob/eab14647e13b95689d3b96c41dc5a3b5b5f11065/mirai-core-api/src/commonMain/kotlin/utils/DeviceInfo.kt#L268-L483
 */
object AokiDeviceInfoManager {
    sealed interface Info {
        fun toDeviceInfo(): DeviceInfo
    }

    @Serializable(HexStringSerializer::class)
    @JvmInline
    value class HexString(
        val data: ByteArray
    )

    object HexStringSerializer : KSerializer<HexString> by String.serializer().map(
        String.serializer().descriptor.copy("HexString"),
        deserialize = { HexString(it.hexToBytes()) },
        serialize = { it.data.toUHexString("").lowercase() }
    )

    // Note: property names must be kept intact during obfuscation process if applied.
    @Serializable
    class Wrapper<T : Info>(
        @Suppress("unused") val deviceInfoVersion: Int, // used by plain jsonObject
        val data: T
    )

    private object DeviceInfoVersionSerializer : KSerializer<DeviceInfo.Version> by SerialData.serializer().map(
        resultantDescriptor = SerialData.serializer().descriptor.copy("Version"),
        deserialize = {
            DeviceInfo.Version(incremental, release, codename, sdk)
        },
        serialize = {
            SerialData(incremental, release, codename, sdk)
        }
    ) {
        @Serializable
        private class SerialData(
            val incremental: ByteArray = "5891938".toByteArray(),
            val release: ByteArray = "10".toByteArray(),
            val codename: ByteArray = "REL".toByteArray(),
            val sdk: Int = 29
        )
    }

    @Serializable
    class V1(
        val display: ByteArray,
        val product: ByteArray,
        val device: ByteArray,
        val board: ByteArray,
        val brand: ByteArray,
        val model: ByteArray,
        val bootloader: ByteArray,
        val fingerprint: ByteArray,
        val bootId: ByteArray,
        val procVersion: ByteArray,
        val baseBand: ByteArray,
        val version: @Serializable(DeviceInfoVersionSerializer::class) DeviceInfo.Version,
        val simInfo: ByteArray,
        val osType: ByteArray,
        val macAddress: ByteArray,
        val wifiBSSID: ByteArray,
        val wifiSSID: ByteArray,
        val imsiMd5: ByteArray,
        val imei: String,
        val apn: ByteArray
    ) : Info {
        override fun toDeviceInfo(): DeviceInfo {
            return DeviceInfo(
                display = display,
                product = product,
                device = device,
                board = board,
                brand = brand,
                model = model,
                bootloader = bootloader,
                fingerprint = fingerprint,
                bootId = bootId,
                procVersion = procVersion,
                baseBand = baseBand,
                version = version,
                simInfo = simInfo,
                osType = osType,
                macAddress = macAddress,
                wifiBSSID = wifiBSSID,
                wifiSSID = wifiSSID,
                imsiMd5 = imsiMd5,
                imei = imei,
                apn = apn
            )
        }
    }


    @Serializable
    class V2(
        val display: String,
        val product: String,
        val device: String,
        val board: String,
        val brand: String,
        val model: String,
        val bootloader: String,
        val fingerprint: String,
        val bootId: String,
        val procVersion: String,
        val baseBand: HexString,
        val version: Version,
        val simInfo: String,
        val osType: String,
        val macAddress: String,
        val wifiBSSID: String,
        val wifiSSID: String,
        val imsiMd5: HexString,
        val imei: String,
        val apn: String
    ) : Info {
        override fun toDeviceInfo(): DeviceInfo = DeviceInfo(
            this.display.toByteArray(),
            this.product.toByteArray(),
            this.device.toByteArray(),
            this.board.toByteArray(),
            this.brand.toByteArray(),
            this.model.toByteArray(),
            this.bootloader.toByteArray(),
            this.fingerprint.toByteArray(),
            this.bootId.toByteArray(),
            this.procVersion.toByteArray(),
            this.baseBand.data,
            this.version.trans(),
            this.simInfo.toByteArray(),
            this.osType.toByteArray(),
            this.macAddress.toByteArray(),
            this.wifiBSSID.toByteArray(),
            this.wifiSSID.toByteArray(),
            this.imsiMd5.data,
            this.imei,
            this.apn.toByteArray()
        )
    }

    @Serializable
    class Version(
        val incremental: String,
        val release: String,
        val codename: String,
        val sdk: Int = 29
    ) {
        companion object {
            fun DeviceInfo.Version.trans(): Version {
                return Version(incremental.decodeToString(), release.decodeToString(), codename.decodeToString(), sdk)
            }

            fun Version.trans(): DeviceInfo.Version {
                return DeviceInfo.Version(incremental.toByteArray(), release.toByteArray(), codename.toByteArray(), sdk)
            }
        }
    }

    fun DeviceInfo.toCurrentInfo(): V2 = V2(
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
        HexString(baseBand),
        version.trans(),
        simInfo.decodeToString(),
        osType.decodeToString(),
        macAddress.decodeToString(),
        wifiBSSID.decodeToString(),
        wifiSSID.decodeToString(),
        HexString(imsiMd5),
        imei,
        apn.decodeToString()
    )

    internal val format = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Throws(IllegalArgumentException::class, NumberFormatException::class) // in case malformed
    fun deserialize(string: String, format: Json = AokiDeviceInfoManager.format): DeviceInfo {
        val element = format.parseToJsonElement(string)

        return when (val version = element.jsonObject["deviceInfoVersion"]?.jsonPrimitive?.content?.toInt() ?: 1) {
            /**
             * @since 2.0
             */
            1 -> format.decodeFromJsonElement(V1.serializer(), element)
            /**
             * @since 2.9
             */
            2 -> format.decodeFromJsonElement(Wrapper.serializer(V2.serializer()), element).data
            else -> throw IllegalArgumentException("Unsupported deviceInfoVersion: $version")
        }.toDeviceInfo()
    }

    fun serialize(info: DeviceInfo, format: Json = AokiDeviceInfoManager.format): String {
        return format.encodeToString(
            Wrapper.serializer(V2.serializer()),
            Wrapper(2, info.toCurrentInfo())
        )
    }

    fun toJsonElement(info: DeviceInfo, format: Json = AokiDeviceInfoManager.format): JsonElement {
        return format.encodeToJsonElement(
            Wrapper.serializer(V2.serializer()),
            Wrapper(2, info.toCurrentInfo())
        )
    }
}

object AokiDeviceInfo {
    fun File.loadAsDeviceInfo(
        json: Json = AokiDeviceInfoManager.format,
        default: () -> DeviceInfo = { DeviceInfo.random() }
    ): DeviceInfo {
        if (!this.exists() || this.length() == 0L) {
            return default().also {
                this.writeText(AokiDeviceInfoManager.serialize(it, json))
            }
        }
        return AokiDeviceInfoManager.deserialize(this.readText(), json)
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
                        procVersion,
                        baseBand,
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