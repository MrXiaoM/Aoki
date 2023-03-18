package top.mrxiaom.mirai.aoki.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


fun Activity.needPermission(requestId: Int, vararg permissions: String) {
    val deniedPerms = permissions.filter {
        ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }.ifEmpty { return }.toTypedArray()
    ActivityCompat.requestPermissions(this@needPermission, deniedPerms, requestId)
}

fun AlertDialog.Builder.buttonPositive(
    @StringRes textId: Int,
    onClick: DialogInterface.(Int) -> Unit
): AlertDialog.Builder =
    setPositiveButton(textId) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.onClick(i)
    }

fun AlertDialog.Builder.buttonPositive(text: String, onClick: DialogInterface.(Int) -> Unit = {}): AlertDialog.Builder =
    setPositiveButton(text) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.onClick(i)
    }

fun AlertDialog.Builder.buttonNegative(
    @StringRes textId: Int,
    onClick: DialogInterface.(Int) -> Unit = {}
): AlertDialog.Builder =
    setNegativeButton(textId) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.onClick(i)
    }

fun AlertDialog.Builder.buttonNegative(text: String, onClick: DialogInterface.(Int) -> Unit = {}): AlertDialog.Builder =
    setNegativeButton(text) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.onClick(i)
    }

fun AlertDialog.Builder.buttonNeutral(
    @StringRes textId: Int,
    onClick: DialogInterface.(Int) -> Unit = {}
): AlertDialog.Builder =
    setNeutralButton(textId) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.onClick(i)
    }

fun AlertDialog.Builder.buttonNeutral(text: String, onClick: DialogInterface.(Int) -> Unit = {}): AlertDialog.Builder =
    setNeutralButton(text) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.onClick(i)
    }

fun AlertDialog.Builder.setTitle(@StringRes messageId: Int, transform: (String) -> String): AlertDialog.Builder {
    return setTitle(transform(context.getText(messageId).toString()))
}

fun AlertDialog.Builder.setMessage(@StringRes messageId: Int, transform: (String) -> String): AlertDialog.Builder {
    return setMessage(transform(context.getText(messageId).toString()))
}
fun Context.text(@StringRes textId: Int): String = getText(textId).toString()

fun Spinner.onItemSelected(selected: (Int) -> Unit) {
    onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) = selected(i)
        override fun onNothingSelected(p0: AdapterView<*>?) { selected(-1) }
    }
}

inline fun <reified T> AppCompatActivity.startActivity(
    conf: Intent.() -> Unit = {}
) where T : AppCompatActivity {
    this.startActivity(Intent(this, T::class.java).also {
        it.conf()
    })
}

@Suppress("DEPRECATION")
inline fun <reified T> AppCompatActivity.startActivityForResult(
    requestId: Int,
    conf: Intent.() -> Unit = {}
) where T : AppCompatActivity {
    this.startActivityForResult(Intent(this, T::class.java).also {
        it.conf()
    }, requestId)
}
