package com.banuba.sdk.example.ivs.amazon

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.view.View
import com.banuba.sdk.example.ivs.R
import kotlinx.coroutines.*

const val IVS_TAG = "AmazonIVS"

private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun launchMain(block: suspend CoroutineScope.() -> Unit) = mainScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Log.d(
            IVS_TAG,
            "Coroutine failed ${e.localizedMessage}"
        )
    },
    block = block
)


fun Activity.showErrorDialog(title: String, message: String) {

    AlertDialog.Builder(this)
        .setTitle(getString(R.string.error_happened_template, title))
        .setMessage(message)
        .setPositiveButton(
            android.R.string.ok, null
        )
        .show()

}


fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.enable() {
    isEnabled = true
}

fun View.enable(enable: Boolean) {
    isEnabled = enable
}


