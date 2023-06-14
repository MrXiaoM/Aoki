package top.mrxiaom.mirai.aoki.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import net.mamoe.mirai.utils.DeviceInfo
import net.mamoe.mirai.utils.hexToBytes
import net.mamoe.mirai.utils.toUHexString
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.databinding.ActivityEditDeviceInfoBinding
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfo.generateForAndroid
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfo.loadFromAoki
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfo.saveToAoki
import top.mrxiaom.mirai.aoki.util.AokiActivity
import top.mrxiaom.mirai.aoki.util.startActivity
import top.mrxiaom.mirai.aoki.util.text
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class EditDeviceInfoActivity : AokiActivity<ActivityEditDeviceInfoBinding>(ActivityEditDeviceInfoBinding::class) {
    private var qq: Long = 0
    private lateinit var deviceInfo: DeviceInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)

        qq = intent.getLongExtra("qq", 0)
        if (qq == 0L) finish().let { return }

        deviceInfo = DeviceInfo.loadFromAoki("$qq/device.json")

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.toolbar_load -> {
                    deviceInfo = DeviceInfo.loadFromAoki("$qq/device.json")
                    resetAll()
                    Toast.makeText(this, text(R.string.edit_device_reloaded).replace("\$qq", qq.toString()), Toast.LENGTH_SHORT).show()
                }
                R.id.toolbar_save -> {
                    deviceInfo.saveToAoki("$qq/device.json")
                    Toast.makeText(this, text(R.string.edit_device_saved).replace("\$qq", qq.toString()), Toast.LENGTH_SHORT).show()
                }
            }
            return@setOnMenuItemClickListener false
        }
        binding.editDisplay.setup(DeviceInfo::display)
        binding.editProduct.setup(DeviceInfo::product)
        binding.editDevice.setup(DeviceInfo::device)
        binding.editBoard.setup(DeviceInfo::board)
        binding.editBrand.setup(DeviceInfo::brand)
        binding.editModel.setup(DeviceInfo::model)
        binding.editBootloader.setup(DeviceInfo::bootloader)
        binding.editFingerprint.setup(DeviceInfo::fingerprint)
        binding.editBootId.setup(DeviceInfo::bootId)
        binding.editProcVersion.setup(DeviceInfo::procVersion)
        binding.editBaseBand.setup(DeviceInfo::baseBand)

        binding.editVersionIncremental.setupVersion(DeviceInfo.Version::incremental)
        binding.editVersionRelease.setupVersion(DeviceInfo.Version::release)
        binding.editVersionCodename.setupVersion(DeviceInfo.Version::codename)
        binding.editVersionSdk.setupVersion(DeviceInfo.Version::sdk)

        binding.editSimInfo.setup(DeviceInfo::simInfo)
        binding.editOsType.setup(DeviceInfo::osType)
        binding.editMacAddress.setup(DeviceInfo::macAddress)
        binding.editWifiBSSID.setup(DeviceInfo::wifiBSSID)
        binding.editWifiSSID.setup(DeviceInfo::wifiSSID)
        binding.editImsiMd5.setupHex(DeviceInfo::imsiMd5)
        binding.editImei.setup(DeviceInfo::imei)
        binding.editApn.setup(DeviceInfo::apn)
        binding.editAndroidId.setup(DeviceInfo::androidId)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_device_menu, menu)
        return true
    }

    private inline fun <reified T: Any> EditText.setup(field: KProperty1<DeviceInfo, T>) {
        val isString: Boolean = T::class.isSubclassOf(String::class)
        tag = {
            val value = field.get(deviceInfo).run {
                if (this is ByteArray) toString(Charsets.UTF_8)
                else this.toString()
            }
            setText(value, TextView.BufferType.NORMAL)
        }
        reset()
        addTextChangedListener {
            val text = it?.toString()
            if (text == "\$GEN") {
                deviceInfo = deviceInfo.edit(field, field.get(DeviceInfo.generateForAndroid()))
                reset()
                Toast.makeText(context, R.string.edit_device_reset, Toast.LENGTH_SHORT).show();
                return@addTextChangedListener
            }
            val newValue = (if (isString) text else text?.toByteArray()) as T?
            if (newValue == null) {
                Toast.makeText(context, R.string.edit_device_invaild, Toast.LENGTH_SHORT).show();
                return@addTextChangedListener
            }
            deviceInfo = deviceInfo.edit(field, newValue)
        }
    }
    private fun EditText.setupHex(field: KProperty1<DeviceInfo, ByteArray>) {
        tag = { setText(field.get(deviceInfo).toUHexString(" "), TextView.BufferType.NORMAL) }
        reset()
        addTextChangedListener {
            val text = it?.toString()
            if (text == "\$GEN") {
                deviceInfo = deviceInfo.edit(field, field.get(DeviceInfo.generateForAndroid()))
                reset()
                Toast.makeText(context, R.string.edit_device_reset, Toast.LENGTH_SHORT).show();
                return@addTextChangedListener
            }
            val newValue = text?.hexToBytes()
            if (newValue == null) {
                Toast.makeText(context, R.string.edit_device_invaild, Toast.LENGTH_SHORT).show();
                return@addTextChangedListener
            }
            deviceInfo = deviceInfo.edit(field, newValue)
        }
    }
    private inline fun <reified T: Any> EditText.setupVersion(field: KProperty1<DeviceInfo.Version, T>) {
        val isInteger: Boolean = T::class.isSubclassOf(Int::class)
        tag = {
            val value = field.get(deviceInfo.version).run {
                if (this is ByteArray) toString(Charsets.UTF_8)
                else this.toString()
            }
            setText(value, TextView.BufferType.NORMAL)
        }
        reset()
        addTextChangedListener {
            val text = it?.toString()
            if (text == "\$GEN") {
                val version = deviceInfo.version.edit(field, field.get(DeviceInfo.generateForAndroid().version))
                deviceInfo = deviceInfo.edit(DeviceInfo::version, version)
                reset()
                Toast.makeText(context, R.string.edit_device_reset, Toast.LENGTH_SHORT).show();
                return@addTextChangedListener
            }
            val newValue = (if (isInteger) text else text?.toIntOrNull()) as T?
            if (newValue == null) {
                Toast.makeText(context, R.string.edit_device_invaild, Toast.LENGTH_SHORT).show();
                return@addTextChangedListener
            }
            val version = deviceInfo.version.edit(field, newValue)
            deviceInfo = deviceInfo.edit(DeviceInfo::version, version)
        }
    }
    private fun resetAll() {
        arrayOf(binding.editDisplay, binding.editProduct,
            binding.editDevice, binding.editBoard, binding.editBrand,
            binding.editModel, binding.editBootloader, binding.editFingerprint,
            binding.editBootId, binding.editProcVersion, binding.editBaseBand,
            binding.editVersionIncremental, binding.editVersionRelease, binding.editVersionCodename,
            binding.editVersionSdk, binding.editSimInfo, binding.editOsType, binding.editMacAddress,
            binding.editWifiBSSID, binding.editWifiSSID, binding.editImsiMd5,
            binding.editImei, binding.editApn, binding.editAndroidId).forEach {
            it.reset()
        }
    }
    private fun EditText.reset() {
        runCatching {
            @Suppress("UNCHECKED_CAST")
            (tag!! as () -> Unit)()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private inline fun <reified T : Any, reified R : Any> R.edit(property: KProperty1<R, T>, value: T): R {
        val list = mutableListOf<Any>()
        for (param in R::class.primaryConstructor!!.parameters) {
            if (param.name == property.name) {
                list.add(value)
                continue
            }
            list.add(R::class.memberProperties.first { it.name == param.name }.call(this)!!)
        }
        return R::class.primaryConstructor!!.call(*list.toTypedArray())
    }

}