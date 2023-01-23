package top.mrxiaom.mirai.aoki

import net.mamoe.mirai.network.LoginFailedException

object ExceptionAnalyzer {
    fun Throwable.analyze(): String {
        val msg = message ?: ""
        return when {
            msg.contains("code=235") -> """
                出现了 235 错误，请尝试以下方法解决:
                * 先使用官方QQ客户端登录机器人账号
                * 更换协议
                * 到「账号管理」删除所有数据
                * 在官方QQ客户端，到「设置→账号安全」开启或关闭「安全登录检查」、「登录设备管理→登录保护」等
            """.trimIndent()
            msg.contains("code=237") -> """
                出现了 237 错误，请尝试以下方法解决:
                * 先使用官方QQ客户端登录机器人账号
                * 更换协议
                * 到「账号管理」删除所有数据
                * 在官方QQ客户端，到「设置→账号安全」开启或关闭「安全登录检查」、「登录设备管理→登录保护」等
                * 在官方QQ客户端，到「设置→账号安全→登录设备管理」中删除历史登录设备
            """.trimIndent()
            msg.contains("returnCode = -10003") -> """
                登录返回码 -10003，可能是登录会话已过期。
                请尝试到「账号管理」删除登录会话
            """.trimIndent()
            else -> ""
        }
    }
}