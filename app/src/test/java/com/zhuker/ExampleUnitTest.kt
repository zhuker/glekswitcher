package com.zhuker

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.zhuker.Model.decode
import io.reactivex.rxjava3.core.Single
import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.lang.StringBuilder
import java.net.Socket
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
fun ByteArray.drop4(limit: Int? = null): ByteArray = this.sliceArray(IntRange(4, (limit ?: this.size) - 1))

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    fun base64(str: String): ByteArray = Base64.getEncoder().encode(str.toByteArray())

    @Test
    fun parseGson() {
        val str = File("src/test/java/com/zhuker/status.json").readText()
        println(str)
        val status = Gson().fromJson(str, SwitchStatus::class.java)!!
        println(status)
        val str64 = Base64.getEncoder().encode("xx".toByteArray()).decodeToString()
        println(str64)
        val query = SwitchStatus(com.zhuker.System(null))
        val message = GsonBuilder().serializeNulls().create().toJson(query)
        println(message)
        println(base64(message).decodeToString())
        println(base64("{ \"system\":{ \"get_sysinfo\":null } }").decodeToString())
        //val sock = Socket("192.168.1.192", 9999)
//        sock.getOutputStream().write("")
        val readBytes = File("/home/zhukov/tmp/xxx").readBytes()

        val ss = decode(readBytes.drop4())
        println(ss)
        Model.query().subscribe { ss: SwitchStatus -> println(ss) }

        Model.rxmsg(Base64.getDecoder().decode(Model.payload_on)).subscribe { bytes: ByteArray ->
            println(decode(bytes.drop4()))
        }


//        println(readBytes.map { (it.toInt() xor 171).toByte() }
//            .toByteArray().decodeToString())
    }


}