package com.zhuker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers.io
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit.SECONDS


class MainActivity : AppCompatActivity() {
    private fun onLightsError(err: Throwable) {
        progressBar.visibility = INVISIBLE
        lights.isEnabled = false
        lights.isClickable = false
        Log.e(TAG, "error reading status $err")
    }

    private fun onLightsUpdated(status: SwitchStatus) {
        lights.isEnabled = true
        lights.isClickable = true
        progressBar.visibility = INVISIBLE
        lights.isChecked = status.isOn()
        lights.text = if (status.isOn()) "Lights ON" else "Lights OFF"
    }

    private fun onGateUpdated(gateStatus: GateStatus) {
        //D/GlekSwitcher: gate GateStatus(id=switch-gateonclosed, state=OFF, value=false)
        //D/GlekSwitcher: gate GateStatus(id=switch-gateonclosed, state=ON, value=true)
        progressBar.visibility = INVISIBLE
        gate.isEnabled = true
        gate.isClickable = true
        gate.isChecked = gateStatus.value
        gate.text = if (gateStatus.value) "Gate CLOSED" else "Gate OPEN"
    }

    private val subs = CompositeDisposable()
    private var lightToggleSub: Disposable = Disposable.empty()
    private var gateToggleSub: Disposable = Disposable.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar.visibility = VISIBLE
        subs.clear()
        lights.setOnClickListener {
            progressBar.visibility = VISIBLE
            lightToggleSub.dispose()
            lightToggleSub = Model.query()
                .flatMap { if (it.isOn()) Model.turnOff() else Model.turnOn() }
                .flatMap { Model.query() }
                .timeout(4, SECONDS)
                .subscribeOn(io()).observeOn(mainThread())
                .subscribe({ onLightsUpdated(it) }, { onLightsError(it) })
            subs.add(lightToggleSub)
        }
        val lightsSub = Model.query().timeout(2, SECONDS).subscribeOn(io()).observeOn(mainThread()).subscribe({
            onLightsUpdated(it)
        }, {
            onLightsError(it)
        })
        subs.add(lightsSub)

        val gateStatusSub =
            Model.gateStatus().filter { it.id == "switch-gateonclosed" }.subscribeOn(io()).observeOn(mainThread())
                .subscribe({
                    Log.d(TAG, "gate $it")
                    onGateUpdated(it)
                }, { Log.e(TAG, "gate error $it") })

        gate.setOnClickListener {
            progressBar.visibility = VISIBLE
            gateToggleSub.dispose()
            gateToggleSub =
                Model.toggleGate().subscribe({ Log.d(TAG, "gate toggle ok") }, { Log.e(TAG, "gate toggle error $it") })
            subs.add(gateToggleSub)
        }

        subs.add(gateStatusSub)
    }

    override fun onDestroy() {
        super.onDestroy()
        subs.clear()
    }

    companion object {
        const val TAG = "GlekSwitcher";
    }
}