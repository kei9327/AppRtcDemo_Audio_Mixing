package io.github.kei9327.webrtc.util;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import java.util.concurrent.CopyOnWriteArraySet

class NetworkConnectionChecker(context: Context) {
    private val mConnectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mListeners: MutableSet<OnNetworkConnectivityChangeListener> =
        CopyOnWriteArraySet()

    fun registerListener(listener: OnNetworkConnectivityChangeListener) {
        mListeners.add(listener)
        listener.networkConnectivityChanged(isConnectedNow)
    }

    fun unregisterListener(listener: OnNetworkConnectivityChangeListener?) {
        mListeners.remove(listener)
    }

    val isConnectedNow: Boolean
        get() {
            val activeNetworkInfo = mConnectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    interface OnNetworkConnectivityChangeListener {
        fun networkConnectivityChanged(availableNow: Boolean)
    }

    private inner class NetworkStateReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val isConnectedNow = isConnectedNow
            for (listener in mListeners) {
                listener.networkConnectivityChanged(isConnectedNow)
            }
        }
    }

    init {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(NetworkStateReceiver(), intentFilter)
    }
}
