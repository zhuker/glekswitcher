package com.zhuker

data class NextAction(val type: Int, val id: String, val schd_sec: Int, val action: Int)

data class SysInfo(
    val sw_ver: String,
    val hw_ver: String,
    val mic_type: String,
    val model: String,
    val mac: String,
    val dev_name: String,
    val alias: String,
    val relay_state: Int,
    val on_time: Long,
    val active_mode: String,
    val feature: String,
    val updating: Int,
    val icon_hash: String,
    val rssi: Int,
    val led_off: Int,
    val longitude_i: Int,
    val latitude_i: Int,
    val hwId: String,
    val fwId: String,
    val deviceId: String,
    val oemId: String,
    val next_action: NextAction,
    val err_code: Int
)

data class SetRelayState(val err_code: Int)

data class System(val get_sysinfo: SysInfo?, val set_relay_state: SetRelayState?)

data class SwitchStatus(val system: System) {
    fun isOn(): Boolean = (system.get_sysinfo?.relay_state ?: 0) != 0
}
