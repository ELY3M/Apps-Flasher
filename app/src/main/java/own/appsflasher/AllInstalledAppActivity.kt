package own.appsflasher

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log


class AllInstalledAppActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var installedAppsList: ArrayList<AppModel>
    private lateinit var installedAppAdapter: AppAdapter
    private lateinit var sp: SharedPreferences
    private lateinit var CheckedAppsList: ArrayList<AppModel>
    private var CheckedAppCount: Int = 0
    lateinit var mCameraImpl: CameraAccess
    var stroberunning: Boolean = false


    //perms
    private fun isNotificationServiceRunning(): Boolean {
        val contentResolver = contentResolver
        val enabledNotificationListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val packageName = packageName
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(
            packageName
        )
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_installed_app)
        recyclerView = findViewById(R.id.recycler_view)
        val hasFlash = this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (!hasFlash) {
            Toast.makeText(applicationContext, "You need new phone :(", Toast.LENGTH_LONG).show()
        }

        val isNotificationServiceRunning = isNotificationServiceRunning()
        if (!isNotificationServiceRunning) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        installedAppsList = ArrayList()
        Handler(Looper.getMainLooper()).postDelayed({
            getInstalledApps()
            findViewById<TextView>(R.id.totalInstalledApp).text =
                "${getString(R.string.total_Installed_Apps)} ${installedAppsList.size}"
            installedAppAdapter = AppAdapter(this, installedAppsList)
            recyclerView.adapter = installedAppAdapter
        }, 500)


        CheckedAppsList = ArrayList()
        Handler(Looper.getMainLooper()).postDelayed({
            sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val allPrefs: Map<String, *> = sp.getAll() //your sharedPreference
            val set = allPrefs.keys
            val checkedapps: Int
            for (s in set) {
                Log.i("appsflash", s + "<" + allPrefs[s]!!.javaClass.simpleName + "> =  "+ allPrefs[s].toString())
                if (allPrefs[s].toString() == "true") {
                    CheckedAppCount++
                    Log.i("appsflash", "Checked apps count: "+CheckedAppCount)
                }


            }



            findViewById<TextView>(R.id.totalCheckedApp).text =
                "${getString(R.string.total_Checked_Apps)} ${CheckedAppCount}"
            //installedAppAdapter = AppAdapter(this, installedAppsList)
            //recyclerView.adapter = installedAppAdapter
        }, 100)



        mCameraImpl = CameraAccess(applicationContext)


        //list all prefs
        sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val allPrefs: Map<String, *> = sp.getAll() //your sharedPreference

        val set = allPrefs.keys
        val checkedapps: Int
        for (s in set) {
            Log.i("appsflash", s + "<" + allPrefs[s]!!.javaClass.simpleName + "> =  "+ allPrefs[s].toString())


        }



    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): ArrayList<AppModel> {
        installedAppsList.clear()
        val packs = packageManager.getInstalledPackages(0)
        for (i in packs.indices) {
            val p = packs[i]
            if (!isSystemPackage(p)) {
                val appName = p.applicationInfo!!.loadLabel(packageManager).toString()
                val icon = p.applicationInfo!!.loadIcon(packageManager)
                val packages = p.applicationInfo!!.packageName
                installedAppsList.add(AppModel(appName, icon, packages))
            }
        }
        installedAppsList.sortBy { it.getName().capitalized() }
        return installedAppsList
    }
    private fun String.capitalized(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(Locale.getDefault())
            else it.toString()
        }
    }
    private fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        val search = menu.findItem(R.id.app_search)

        val searchView = search.actionView as SearchView
        searchView.maxWidth = android.R.attr.width
        searchView.queryHint = "Search app name or package"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(newText: String?): Boolean {
                val appModelArrayList: ArrayList<AppModel> = ArrayList()

                for (i in installedAppsList) {
                    if (i.getName().lowercase(Locale.getDefault()).contains(
                            newText!!.lowercase(
                                Locale.getDefault()
                            )
                        )
                        ||
                        i.getPackages().lowercase(Locale.getDefault()).contains(
                            newText.lowercase(
                                Locale.getDefault()
                            )
                        )
                    ) {
                        appModelArrayList.add(i)
                    }
                }
                installedAppAdapter =
                    AppAdapter(this@AllInstalledAppActivity, appModelArrayList)

                recyclerView.adapter = installedAppAdapter
                installedAppAdapter.notifyDataSetChanged()
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val strobeTime = sp.getInt("strobe_length", 5000)

        return when (item.itemId) {
            //R.id.app_search -> {
            //    Toast.makeText(applicationContext, "click on setting", Toast.LENGTH_LONG).show()
            //    true
            //}
            R.id.app_settings -> {
                val intent = Intent(this, own.appsflasher.Settings::class.java)
                startActivity(intent)
                return true
            }
            R.id.app_flashlight_on -> {

                mCameraImpl.startStroboscope()
                stroberunning = true

                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        mCameraImpl.stopStroboscope()
                        stroberunning = false
                    }
                }, strobeTime.toLong())

                return true
            }
            R.id.app_flashlight_off -> {

                mCameraImpl.stopStroboscope()
                stroberunning = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



}




