package xyz.cssxsh.mirai.tool

import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.io.*
import java.net.*
import java.time.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object FixProtocolVersion {

    private val clazz = MiraiProtocolInternal::class.java

    private val constructor = clazz.constructors.single()

    @PublishedApi
    internal fun <T> MiraiProtocolInternal.field(name: String, default: T): T {
        @Suppress("UNCHECKED_CAST")
        return kotlin.runCatching {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            field.get(this) as T
        }.getOrElse {
            default
        }
    }

    @PublishedApi
    internal fun MiraiProtocolInternal.change(block: MiraiProtocolInternalBuilder.() -> Unit): MiraiProtocolInternal {
        val builder = MiraiProtocolInternalBuilder(this).apply(block)
        return when (constructor.parameterCount) {
            10 -> constructor.newInstance(
                builder.apkId,
                builder.id,
                builder.ver,
                builder.sdkVer,
                builder.miscBitMap,
                builder.subSigMap,
                builder.mainSigMap,
                builder.sign,
                builder.buildTime,
                builder.ssoVersion
            )
            11 -> constructor.newInstance(
                builder.apkId,
                builder.id,
                builder.ver,
                builder.sdkVer,
                builder.miscBitMap,
                builder.subSigMap,
                builder.mainSigMap,
                builder.sign,
                builder.buildTime,
                builder.ssoVersion,
                builder.supportsQRLogin
            )
            else -> this
        } as MiraiProtocolInternal
    }

    @PublishedApi
    internal class MiraiProtocolInternalBuilder(impl: MiraiProtocolInternal) {
        var apkId: String = impl.field("apkId", "")
        var id: Long = impl.field("id", 0)
        var ver: String = impl.field("ver", "")
        var sdkVer: String = impl.field("sdkVer", "")
        var miscBitMap: Int = impl.field("miscBitMap", 0)
        var subSigMap: Int = impl.field("subSigMap", 0)
        var mainSigMap: Int = impl.field("mainSigMap", 0)
        var sign: String = impl.field("sign", "")
        var buildTime: Long = impl.field("buildTime", 0)
        var ssoVersion: Int = impl.field("ssoVersion", 0)
        var supportsQRLogin: Boolean = impl.field("supportsQRLogin", false)
    }

    @JvmStatic
    public fun update() {
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.ANDROID_PHONE) { _, impl ->
            when {
                null == impl -> null
                impl.runCatching { id }.isFailure -> impl.change {
                    apkId = "com.tencent.mobileqq"
                    id = 537163098
                    ver = "8.9.58.11170"
                    sdkVer = "6.0.0.2545"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x0214_10E0
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D"
                    buildTime = 1684467300L
                    ssoVersion = 20
                }
                impl.id < 537163098 -> impl.apply {
                    apkId = "com.tencent.mobileqq"
                    id = 537163098
                    ver = "8.9.58"
                    buildVer = "8.9.58.11170"
                    sdkVer = "6.0.0.2545"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x0214_10E0
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D"
                    buildTime = 1684467300L
                    ssoVersion = 20
                    appKey = "0S200MNJT807V3GE"
                    supportsQRLogin = false
                }
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.ANDROID_PAD) { _, impl ->
            when {
                null == impl -> null
                impl.runCatching { id }.isFailure -> impl.change {
                    apkId = "com.tencent.mobileqq"
                    id = 537161402
                    ver = "8.9.58.11170"
                    sdkVer = "6.0.0.2545"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x0214_10E0
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D"
                    buildTime = 1684467300L
                    ssoVersion = 20
                }
                impl.id < 537161402 -> impl.apply {
                    apkId = "com.tencent.mobileqq"
                    id = 537161402
                    ver = "8.9.58"
                    buildVer = "8.9.58.11170"
                    sdkVer = "6.0.0.2545"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x0214_10E0
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D"
                    buildTime = 1684467300L
                    ssoVersion = 20
                    appKey = "0S200MNJT807V3GE"
                    supportsQRLogin = false
                }
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.ANDROID_WATCH) { _, impl ->
            when {
                null == impl -> null
                impl.runCatching { id }.isFailure -> impl.change {
                    apkId = "com.tencent.qqlite"
                    id = 537065138
                    ver = "2.0.8"
                    sdkVer = "6.0.0.2365"
                    miscBitMap = 0x00F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x00FF_32F2
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D"
                    buildTime = 1559564731L
                    ssoVersion = 5
                    supportsQRLogin = true
                }
                impl.id < 537065138 -> impl.apply {
                    apkId = "com.tencent.qqlite"
                    id = 537065138
                    ver = "2.0.8"
                    buildVer = "2.0.8"
                    sdkVer = "6.0.0.2365"
                    miscBitMap = 0x00F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x00FF_32F2
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D"
                    buildTime = 1559564731L
                    ssoVersion = 5
                    supportsQRLogin = true
                }
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.IPAD) { _, impl ->
            when {
                null == impl -> null
                impl.runCatching { id }.isFailure -> impl.change {
                    apkId = "com.tencent.minihd.qq"
                    id = 537151363
                    ver = "8.9.33.614"
                    sdkVer = "6.0.0.2433"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x001E_10E0
                    sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7"
                    buildTime = 1640921786L
                    ssoVersion = 19
                }
                impl.id < 537151363 -> impl.apply {
                    apkId = "com.tencent.minihd.qq"
                    id = 537151363
                    ver = "8.9.33"
                    buildVer = "8.9.33.614"
                    sdkVer = "6.0.0.2433"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x001E_10E0
                    sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7"
                    buildTime = 1640921786L
                    ssoVersion = 19
                    supportsQRLogin = false
                }
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.MACOS) { _, impl ->
            when {
                null == impl -> null
                impl.runCatching { id }.isFailure -> impl.change {
                    if (supportsQRLogin) return@change
                    apkId = "com.tencent.minihd.qq"
                    id = 537128930
                    ver = "5.8.9"
                    sdkVer = "6.0.0.2433"
                    miscBitMap = 0x08F7_FF7C
                    subSigMap = 0x0001_0400
                    mainSigMap = 0x001E_10E0
                    sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7"
                    buildTime = 1595836208L
                    ssoVersion = 12
                }
                impl.id < 537128930 -> impl.apply {
                    // TODO
                }
                else -> impl
            }
        }
    }

    @JvmStatic
    public fun sync(protocol: BotConfiguration.MiraiProtocol) {
        val (file, url) = when (protocol) {
            BotConfiguration.MiraiProtocol.ANDROID_PHONE -> {
                File("android_phone.json") to
                        URL("https://raw.githubusercontent.com/RomiChan/protocol-versions/master/android_phone.json")
            }
            BotConfiguration.MiraiProtocol.ANDROID_PAD -> {
                File("android_pad.json") to
                        URL("https://raw.githubusercontent.com/RomiChan/protocol-versions/master/android_pad.json")
            }
            else -> {
                throw IllegalArgumentException("不支持同步的协议: ${protocol.name}")
            }
        }
        val system = System.getProperty("java.net.useSystemProxies", "false")
        val proxy = try {
            System.setProperty("java.net.useSystemProxies", "true")
            ProxySelector.getDefault()
                .select(URI("https://www.google.com/"))
                .firstOrNull() ?: Proxy.NO_PROXY
        } finally {
            System.setProperty("java.net.useSystemProxies", system)
        }

        val json: JsonObject = kotlin.runCatching {
            url.openConnection(proxy)
                .getInputStream().use { it.readBytes() }
                .decodeToString()
        }.fold(
            onSuccess = { text ->
                val online = Json.parseToJsonElement(text).jsonObject
                if (file.isFile) {
                    val local = Json.parseToJsonElement(file.readText()).jsonObject
                    if (local.getValue("dump_time").jsonPrimitive.long <
                        online.getValue("dump_time").jsonPrimitive.long) {
                        file.writeText(text)
                        online
                    } else {
                        local
                    }
                } else {
                    online
                }
            },
            onFailure = { cause ->
                if (file.isFile) {
                    Json.parseToJsonElement(file.readText()).jsonObject
                } else {
                    throw FileNotFoundException(file.path).initCause(cause)
                }
            }
        )

        MiraiProtocolInternal.protocols.compute(protocol) { _, impl ->
            when {
                null == impl -> null
                impl.runCatching { id }.isFailure -> impl.change {
                    apkId = json.getValue("apk_id").jsonPrimitive.content
                    id = json.getValue("app_id").jsonPrimitive.long
                    ver = json.getValue("sort_version_name").jsonPrimitive.content
                    sdkVer = json.getValue("sdk_version").jsonPrimitive.content
                    miscBitMap = json.getValue("misc_bitmap").jsonPrimitive.int
                    subSigMap = json.getValue("sub_sig_map").jsonPrimitive.int
                    mainSigMap = json.getValue("main_sig_map").jsonPrimitive.int
                    sign = json.getValue("apk_sign").jsonPrimitive.content.hexToBytes().toUHexString(" ")
                    buildTime = json.getValue("build_time").jsonPrimitive.long
                    ssoVersion = json.getValue("sso_version").jsonPrimitive.int
                }
                else -> impl.apply {
                    apkId = json.getValue("apk_id").jsonPrimitive.content
                    id = json.getValue("app_id").jsonPrimitive.long
                    buildVer = json.getValue("sort_version_name").jsonPrimitive.content
                    ver = buildVer.substringBeforeLast(".")
                    sdkVer = json.getValue("sdk_version").jsonPrimitive.content
                    miscBitMap = json.getValue("misc_bitmap").jsonPrimitive.int
                    subSigMap = json.getValue("sub_sig_map").jsonPrimitive.int
                    mainSigMap = json.getValue("main_sig_map").jsonPrimitive.int
                    sign = json.getValue("apk_sign").jsonPrimitive.content.hexToBytes().toUHexString(" ")
                    buildTime = json.getValue("build_time").jsonPrimitive.long
                    ssoVersion = json.getValue("sso_version").jsonPrimitive.int
                    appKey = json.getValue("app_key").jsonPrimitive.content
                }
            }
        }
    }

    @JvmStatic
    public fun info(): Map<BotConfiguration.MiraiProtocol, String> {
        return MiraiProtocolInternal.protocols.mapValues { (protocol, info) ->
            val version = info.field("buildVer", null as String?) ?: info.field("ver", "???")
            val epochSecond = info.field("buildTime", 0L)
            val datetime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault())

            "%-13s  %-12s  %s".format(protocol, version, datetime)
        }
    }
}