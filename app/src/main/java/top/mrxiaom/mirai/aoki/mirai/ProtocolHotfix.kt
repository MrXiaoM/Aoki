package top.mrxiaom.mirai.aoki.mirai

import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*

/**
 * [cssxsh/fix-protocol-version](https://github.com/cssxsh/fix-protocol-version/blob/main/src/main/kotlin/xyz/cssxsh/mirai/tool/FixProtocolVersion.kt)
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
internal fun hotfixProtocolVersion() {
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PHONE] = MiraiProtocolInternal(
        apkId = "com.tencent.mobileqq",
        id = 537151682,
        ver = "8.9.33.10335",
        sdkVer = "6.0.0.2534",
        miscBitMap = 150470524,
        subSigMap = 0x10400,
        mainSigMap = 16724722,
        sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
        buildTime = 1673599898L,
        ssoVersion = 19,
    )
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PAD] = MiraiProtocolInternal(
        apkId = "com.tencent.mobileqq",
        id = 537151218,
        ver = "8.9.33.10335",
        sdkVer = "6.0.0.2534",
        miscBitMap = 150470524,
        subSigMap = 0x10400,
        mainSigMap = 16724722,
        sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
        buildTime = 1673599898L,
        ssoVersion = 19,
    )
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.IPAD] = MiraiProtocolInternal(
        apkId = "com.tencent.minihd.qq",
        id = 537151363,
        ver = "8.9.33.614",
        sdkVer = "6.0.0.2433",
        miscBitMap = 150470524,
        subSigMap = 66560,
        mainSigMap = 1970400,
        sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
        buildTime = 1640921786L,
        ssoVersion = 12,
    )
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.MACOS] = MiraiProtocolInternal(
        apkId = "com.tencent.minihd.qq",
        id = 537128930,
        ver = "5.8.9",
        sdkVer = "6.0.0.2433",
        miscBitMap = 150470524,
        subSigMap = 66560,
        mainSigMap = 1970400,
        sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
        buildTime = 1595836208L,
        ssoVersion = 12,
    )
}
