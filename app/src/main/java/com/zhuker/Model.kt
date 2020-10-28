package com.zhuker

import com.google.gson.Gson
import io.reactivex.rxjava3.core.Single
import java.lang.StringBuilder
import java.net.Socket
import java.util.*

fun ByteArray.drop4(limit: Int? = null): ByteArray = this.sliceArray(IntRange(4, (limit ?: this.size) - 1))

object Model {
    const val payload_query = "AAAAI9Dw0qHYq9+61/XPtJS20bTAn+yV5o/hh+jK8J7rh+vLtpbr"
    const val payload_on = "AAAAKtDygfiL/5r31e+UtsWg1Iv5nPCR6LfEsNGlwOLYo4HyhueT9tTu36Lfog=="
    const val payload_off = "AAAAKtDygfiL/5r31e+UtsWg1Iv5nPCR6LfEsNGlwOLYo4HyhueT9tTu3qPeow=="
    const val lights_ip_addr = "192.168.1.192"
    const val lights_port = 9999

    fun rxmsg(q: ByteArray): Single<ByteArray> {
        return Single.create { emitter ->
            Socket(lights_ip_addr, lights_port).use {
                it.soTimeout = 2000
                it.getOutputStream().write(q)
                val arr = ByteArray(1024)
                val r = it.getInputStream().read(arr)
                when {
                    r <= 0 -> emitter.onError(RuntimeException("cant read response $r"))
                    else -> emitter.onSuccess(arr.sliceArray(IntRange(0, r - 1)))
                }
            }
        }
    }

    fun decode(bytes: ByteArray): String {
        return bytes.fold((171u to StringBuilder())) { (code, sb), byte ->
            val element = byte.toUByte().toUInt()
            val output = element xor code
            val c = output.toUShort().toShort().toChar()
            sb.append(c)
            element to sb
        }.second.toString()
    }

    val gson = Gson()

    fun query(): Single<SwitchStatus> {
        return rxmsg(Base64.getDecoder().decode(payload_query)).map {
            val s = decode(it.drop4())
            gson.fromJson(s, SwitchStatus::class.java)
        }
    }

    fun turnOn(): Single<SwitchStatus> {
        return rxmsg(Base64.getDecoder().decode(payload_on)).map {
            val s = decode(it.drop4())
            gson.fromJson(s, SwitchStatus::class.java)
        }
    }

    fun turnOff(): Single<SwitchStatus> {
        return rxmsg(Base64.getDecoder().decode(payload_off)).map {
            val s = decode(it.drop4())
            gson.fromJson(s, SwitchStatus::class.java)
        }
    }
}