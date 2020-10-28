package com.zhuker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers.io
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit.SECONDS


class MainActivity : AppCompatActivity() {
    private fun onError(err: Throwable) {
        progressBar.visibility = INVISIBLE
        lights.isEnabled = false
        Log.e(TAG, "error reading status $err")
    }

    private fun onStatusUpdated(status: SwitchStatus) {
        lights.isEnabled = true
        progressBar.visibility = INVISIBLE
        lights.isChecked = status.isOn()
    }

    private val subs = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar.visibility = VISIBLE
        subs.clear()
        lights.setOnClickListener {
            progressBar.visibility = VISIBLE
            val subscribe = Model.query()
                .flatMap { if (it.isOn()) Model.turnOff() else Model.turnOn() }
                .flatMap { Model.query() }
                .timeout(4, SECONDS)
                .subscribeOn(io()).observeOn(mainThread())
                .subscribe({ onStatusUpdated(it) }, { onError(it) })
            subs.add(subscribe)
        }
        val sub = Model.query().timeout(2, SECONDS).subscribeOn(io()).observeOn(mainThread()).subscribe({
            onStatusUpdated(it)
        }, {
            onError(it)
        })
        subs.add(sub)
    }

    override fun onDestroy() {
        super.onDestroy()
        subs.clear()
    }

    companion object {
        const val TAG = "GlekSwitcher";
    }
}