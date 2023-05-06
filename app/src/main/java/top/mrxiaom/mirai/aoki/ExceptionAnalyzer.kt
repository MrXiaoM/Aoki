package top.mrxiaom.mirai.aoki

import top.mrxiaom.mirai.aoki.ui.model.UserCancelledLoginException

object ExceptionAnalyzer {
    fun Throwable.analyze(): String {
        // 拆箱
        // Since 2.15.0-M1 #2502: net.mamoe.mirai.internal.network.auth.ProducerFailureException
        // Since 2.15.0? #2645: net.mamoe.mirai.utils.channels.ProducerFailureException
        cause?.also {
            if (this::class.java.name.contains("ProducerFailure"))
                return it.analyze()
        }
        val msg = message ?: ""
        var stacktrace = true
        val analyze = when {
            this is UserCancelledLoginException -> "用户主动取消了登录操作".also { stacktrace = false }
            msg.contains("code=235") -> """
                出现了 235 错误，你的账号可能已被风控，请尝试以下方法解决:
                * 检查密码是否错误，密码长度是否在16位以内
                * 先使用官方QQ客户端登录机器人账号
                * 更换协议
                * 到「账号管理」删除所有数据
                * 在官方QQ客户端，到「设置→账号安全」开启或关闭「安全登录检查」、「登录设备管理→登录保护」等
            """.trimIndent()
            msg.contains("code=237") -> """
                出现了 237 错误，你的账号可能已被风控，请尝试以下方法解决:
                * 先使用官方QQ客户端登录机器人账号
                * 更换协议
                * 到「账号管理」删除所有数据
                * 在官方QQ客户端，到「设置→账号安全」开启或关闭「安全登录检查」、「登录设备管理→登录保护」等
                * 在官方QQ客户端，到「设置→账号安全→登录设备管理」中删除历史登录设备
            """.trimIndent()
            msg.contains("code=45") -> """
                出现了 45 错误，你的账号已被风控，请尝试以下方法解决:
                * 检查密码是否错误，密码长度是否在16位以内
                * 先使用官方QQ客户端登录机器人账号
                * 更换协议
                * 到「账号管理」删除所有数据
                * 使用官方QQ客户端登录机器人账号，跟正常用户一样聊天、活跃，使你的设备被服务器认同。
                * 在 mirai 协议版本更新之前，安装需 fix-protocol-version 插件，且必须选择手机协议、平板协议或 MacOS 协议。
                * Aoki 现已整合 fix-protocol-version 的协议版本变更。
            """.trimIndent()
            msg.contains("code=6") -> """
                出现了 6 错误，该错误可能是由新注册的账号未在官方客户端登录过引起的。
                请先在官方QQ客户端登录该账号，最好跟普通用户一样正常聊天几天后再试。
            """.trimIndent()
            msg.contains("code=238") -> """
                出现了 238 错误，请尝试更换到手机协议或平板协议再试，或者使用扫码登录。
            """.trimIndent()
            msg.contains("which may mean session expired.") -> """
                登录出现异常返回码，可能是登录会话已过期。
                请尝试到「账号管理」删除登录会话
            """.trimIndent()
            msg.contains("The login protocol must be") && msg.contains("while enabling qrcode login") -> """
                你选择的协议不支持进行扫码登录。
            """.trimIndent()
            else -> null
        }?.run { if (stacktrace) (this + "\n\n" +
            "==============================\n" +
            "以下为原始异常信息，非开发者无需阅读 \n" +
            "==============================\n\n") else this } ?: ""
        return analyze + (if (stacktrace) stackTraceToString() else "")
    }
}
