package top.mrxiaom.mirai.aoki.ui

import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import top.mrxiaom.mirai.aoki.R
import top.mrxiaom.mirai.aoki.databinding.ActivityEditDeviceInfoBinding

class EditDeviceInfoActivity : AppCompatActivity() {

    private lateinit var mHandler: Handler
    private lateinit var binding: ActivityEditDeviceInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler(mainLooper)
        binding = ActivityEditDeviceInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<EditDeviceFragment>(R.id.settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish();
        }
        return super.onOptionsItemSelected(item)
    }

    class EditDeviceFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_edit_device, rootKey)
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            return false
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            return false
        }
    }
}
