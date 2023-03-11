package top.mrxiaom.mirai.aoki.mirai

import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*

/**
 * [cssxsh/fix-protocol-version](https://github.com/cssxsh/fix-protocol-version/blob/main/src/main/kotlin/xyz/cssxsh/mirai/tool/FixProtocolVersion.kt)
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
internal fun hotfixProtocolVersion() {
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PHONE] = MiraiProtocolInternal(
        "com.tencent.mobileqq",
        537151682,
        "8.9.33.10335",
        "6.0.0.2534",
        150470524,
        0x10400,
        16724722,
        "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
        1673599898L,
        19,
    )
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PAD] = MiraiProtocolInternal(
        "com.tencent.mobileqq",
        537151218,
        "8.9.33.10335",
        "6.0.0.2534",
        150470524,
        0x10400,
        16724722,
        "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
        1673599898L,
        19,
    )
    MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.MACOS] = MiraiProtocolInternal(
        "com.tencent.minihd.qq",
        537128930,
        "5.8.9",
        "6.0.0.2433",
        150470524,
        66560,
        1970400,
        "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
        1595836208L,
        12,
    )
}
