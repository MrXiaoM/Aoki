package top.mrxiaom.mirai.aoki.ui

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import net.mamoe.mirai.utils.DeviceInfo
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.databinding.ActivityEditDeviceInfoBinding
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfo.loadFromAoki
import top.mrxiaom.mirai.aoki.util.AokiActivity
import top.mrxiaom.mirai.aoki.util.text
import java.io.File
import kotlin.reflect.KProperty1

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

        binding.editDisplay.setup(DeviceInfo::display)
        binding.editProduct.setup(DeviceInfo::product)
        binding.editDevice.setup(DeviceInfo::device)
        binding.editBoard.setup(DeviceInfo::board)
        binding.editBrand.setup(DeviceInfo::brand)
        binding.editModel.setup(DeviceInfo::model)
        binding.editBootloader.setup(DeviceInfo::bootloader)
        binding.editFigerprint.setup(DeviceInfo::fingerprint)
        binding.editBootId.setup(DeviceInfo::bootId)
        binding.editProcVersion.setup(DeviceInfo::procVersion)
        binding.editBaseBand.setup(DeviceInfo::baseBand)

        binding.editVersionIncremental.setup(DeviceInfo.Version::incremental)
        binding.editVersionRelease.setup(DeviceInfo.Version::release)
        binding.editVersionCodename.setup(DeviceInfo.Version::codename)
        binding.editVersionSdk.setup(DeviceInfo.Version::sdk)

        binding.editSimInfo.setup(DeviceInfo::simInfo)
        binding.editOsType.setup(DeviceInfo::osType)
        binding.editMacAddress.setup(DeviceInfo::macAddress)
        binding.editWifiBSSID.setup(DeviceInfo::wifiBSSID)
        binding.editWifiSSID.setup(DeviceInfo::wifiSSID)
        binding.editApn.setup(DeviceInfo::apn)
        binding.editAndroidId.setup(DeviceInfo::androidId)
    }
    private fun EditText.setup(field: KProperty1<DeviceInfo, ByteArray>) {
        addTextChangedListener {

        }
    }
    private inline fun <reified T> EditText.setup(field: KProperty1<DeviceInfo.Version, T>) {

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}