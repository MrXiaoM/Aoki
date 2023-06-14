package top.mrxiaom.mirai.aoki.util

import android.app.Activity
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ArrayRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.viewbinding.ViewBinding
import top.mrxiaom.mirai.aoki.R
import kotlin.reflect.KClass

abstract class AokiActivity<T : ViewBinding>(private val bindingClass: KClass<T>)  : AppCompatActivity() {
    lateinit var mHandler: Handler
    lateinit var binding: T

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler(mainLooper)
        @Suppress("unchecked_cast")
        binding = bindingClass.java.getDeclaredMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as T
        setContentView(binding.root)
    }
}
fun <A : AokiActivity<*>> A.runInUIThread(action: A.() -> Unit) {
    mHandler.post { action() }
}
fun Activity.needPermission(requestId: Int, vararg permissions: String) {
    val deniedPerms = permissions.filter {
        ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }.ifEmpty { return }.toTypedArray()
    ActivityCompat.requestPermissions(this@needPermission, deniedPerms, requestId)
}

fun AlertDialog.Builder.buttonPositive(
    @StringRes textId: Int,
    onClick: DialogInterface.(Int) -> Unit = {}
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

inline fun <reified T> Activity.startActivity(
    conf: Intent.() -> Unit = {}
) where T : Activity {
    this.startActivity(Intent(this, T::class.java).also {
        it.conf()
    })
}

inline fun <reified T> Activity.startActivityForResult(
    requestId: Int,
    conf: Intent.() -> Unit = {}
) where T : Activity {
    this.startActivityForResult(Intent(this, T::class.java).also {
        it.conf()
    }, requestId)
}
fun <T> Context.setupDropdownBox(
    dropdownBox: Spinner,
    @ArrayRes array: Int,
    vararg values: T,
    onSelected: (i: Int, value: T) -> Unit
) {
    dropdownBox.adapter =
        ArrayAdapter.createFromResource(this, array, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    dropdownBox.onItemSelected {
        if (it < 0) return@onItemSelected
        onSelected(it, values[it])
    }
}

fun ImageView.setImage(data: ByteArray) {
    setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.size))
}

fun Context.layout(@LayoutRes resId: Int, root: ViewGroup? = null, block: (View.() -> Unit)? = null): View {
    val view = LayoutInflater.from(this).inflate(resId, root)
    if (block != null) view.block()
    return view
}
fun Context.dialog(block: AlertDialog.Builder.() -> Unit): AlertDialog {
    val builder = AlertDialog.Builder(this)
    builder.block()
    return builder.create()
}
fun AokiActivity<*>.dialogInUIThread(block: AlertDialog.Builder.() -> Unit) {
    runInUIThread { dialog(block) }
}
val Context.packageInfo: PackageInfo
    @Suppress("DEPRECATION")
    get() =  packageManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS)
fun Context.copy(text: String) = copy(text, text)
fun Context.copy(label: String, text: String) {
    Toast.makeText(this, getSystemService<ClipboardManager>()?.let { clip ->
        clip.setPrimaryClip(ClipData.newPlainText(label, text))
        R.string.scan_copy_done
    } ?: R.string.scan_copy_failed, Toast.LENGTH_SHORT).show()
}
fun <T> LifecycleOwner.observe(data: LiveData<T>, block: T.() -> Unit) {
    data.observe(this) { it?.block() }
}
