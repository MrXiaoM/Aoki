package top.mrxiaom.mirai.aoki.mirai

import net.mamoe.mirai.internal.spi.EncryptService
import net.mamoe.mirai.internal.spi.EncryptServiceContext
import top.dsbbs2.t544.Tlv544Sign

object EncryptProvider : EncryptService {
    override fun encryptTlv(
        context: EncryptServiceContext,
        tlvType: Int,
        payload: ByteArray
    ): ByteArray? {
        if (tlvType != 0x544) return null
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]

        println("t544 command: $command")

        val bytes = if (payload.last().toInt() == 0)
            payload.copyInto(ByteArray(payload.size), 4, 4)
        else payload
        return Tlv544Sign.signBytes(bytes)
    }
}
