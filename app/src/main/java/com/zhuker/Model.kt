package com.zhuker

import android.util.Log
import com.google.gson.Gson
import com.zhuker.MainActivity.Companion.TAG
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.lang.StringBuilder
import java.net.Socket
import java.util.*

fun ByteArray.drop4(limit: Int? = null): ByteArray = this.sliceArray(IntRange(4, (limit ?: this.size) - 1))

data class GateEvent(val id: String?, val type: String?, val data: String)
class SSEException(err: Throwable?, val response: Response?) : java.lang.RuntimeException(err)
data class GateStatus(val id: String, val state: String, val value: Boolean)

object Model {
    const val payload_query = "AAAAI9Dw0qHYq9+61/XPtJS20bTAn+yV5o/hh+jK8J7rh+vLtpbr"
    const val payload_on = "AAAAKtDygfiL/5r31e+UtsWg1Iv5nPCR6LfEsNGlwOLYo4HyhueT9tTu36Lfog=="
    const val payload_off = "AAAAKtDygfiL/5r31e+UtsWg1Iv5nPCR6LfEsNGlwOLYo4HyhueT9tTu3qPeow=="
    const val lights_ip_addr = "192.168.1.192"
    const val lights_port = 9999
    const val gate_ip = "192.168.1.244"

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

    private val client = OkHttpClient()

    fun gateEvents(): Observable<GateEvent> {
        return Observable.create { emitter ->
            val newEventSource = EventSources.createFactory(client)
                .newEventSource(
                    Request.Builder().url("http://$gate_ip:80/events").build(),
                    object : EventSourceListener() {
                        override fun onOpen(eventSource: EventSource, response: Response) {
                            super.onOpen(eventSource, response)
//                            println("onOpen")
                        }

                        override fun onClosed(eventSource: EventSource) {
                            super.onClosed(eventSource)
//                            println("onClosed")
                            emitter.onComplete()
                        }

                        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                            super.onEvent(eventSource, id, type, data)
//                            println("onEvent '$id' '$type' '$data'")
                            emitter.onNext(GateEvent(id, type, data))
                        }

                        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                            super.onFailure(eventSource, t, response)
                            if (!emitter.isDisposed) {
                                emitter.onError(SSEException(t, response))
                            } else {
                                Log.e(TAG, "gate event error $t")
                            }
                        }
                    })
            emitter.setCancellable {
                newEventSource.cancel()
            }
        }
    }

    fun gateStatus(): Observable<GateStatus> {
        return gateEvents().filter { it.type == "state" }.map { gson.fromJson(it.data, GateStatus::class.java)!! }
    }

    fun toggleGate(): Single<Response> {
        return Single.create { emitter ->
            val req = Request.Builder().url("http://$gate_ip/switch/open_for_30min/toggle")
                .method("POST", "".toRequestBody("text/plain".toMediaType())).build()
            val newCall = client.newCall(req)
            newCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    emitter.onSuccess(response)
                }
            })
            emitter.setCancellable {
                newCall.cancel()
            }
        }
    }
}