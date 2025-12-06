package own.appsflasher

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.preference.*



class Settings : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        val mFragmentManager = supportFragmentManager
        val mFragmentTransaction = mFragmentManager.beginTransaction()
        val mPrefsFragment = PrefsFragment(applicationContext)
        mFragmentTransaction.replace(android.R.id.content, mPrefsFragment)
        mFragmentTransaction.commit()

    }


    class PrefsFragment(mContext: Context) : PreferenceFragmentCompat() {

        val sp = PreferenceManager.getDefaultSharedPreferences(mContext)

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.prefs_settings)
            val strobeLength = findPreference<SeekBarPreference>("strobe_length")
            val smartHubURL = findPreference<EditTextPreference>("url")
            strobeLength?.summary = (sp.getInt("strobe_length", 10000)).toString()
            smartHubURL?.summary = (sp.getString("url", "https://192.168.1.100/script.php"))


            val strobeLenghtPreference: SeekBarPreference? =
                findPreference("strobe_length") as SeekBarPreference?
            strobeLenghtPreference!!.summary = sp.getInt("strobe_length", 10000).toString()
            strobeLenghtPreference!!.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference, o ->
                val strobeTime = o.toString()
                sp.edit().putInt("strobe_length", strobeTime.toInt()).apply()
                strobeLenghtPreference!!.summary = strobeTime
                true
            })


            val smartHubURLPreference: EditTextPreference? =
                findPreference("url") as EditTextPreference?
            smartHubURLPreference!!.summary = sp.getString("url", "https://192.168.1.100/script.php")
            smartHubURLPreference!!.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference, o ->
                val URL = o.toString()
                sp.edit().putString("url", URL).apply()
                smartHubURLPreference!!.summary = URL
                true
            })


        }

    }

}
