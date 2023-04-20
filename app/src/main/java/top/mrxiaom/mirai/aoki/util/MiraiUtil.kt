@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.mrxiaom.mirai.aoki.util

import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.internal.utils.MiraiProtocolInternal;

val BotConfiguration.MiraiProtocol.isSupportQRLogin: Boolean
    get() = MiraiProtocolInternal.protocols[this]?.supportsQRLogin ?: false
