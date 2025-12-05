package own.appslister

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class NotificationService : NotificationListenerService() {

    private lateinit var mContext: Context
    private lateinit var sp: SharedPreferences
    lateinit var mCameraImpl: CameraAccess
    private val notificationScope = MainScope()
    private var stroberunning = false

    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        mCameraImpl = CameraAccess(mContext)
    }

    private fun load(key: String): Boolean {
        sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        return sp.getBoolean(key, false)
    }

    private fun StrobeLight() {
        Log.i("appsflash", "Strobelight()")
        sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        val smartHubURL = sp.getString("url", "http://192.168.1.100/script.php")
        val connection = URL(smartHubURL).openConnection() as HttpURLConnection
        try {
            val data = connection.inputStream.bufferedReader().use { it.readText()
                Log.d("appsflash", "opened url: "+smartHubURL)
            }
        } catch (e: Exception) {
            Log.d("appsflash", "failed to open url: "+smartHubURL)
            Log.d("appsflash", "connection crashed: "+e.toString())
        } finally {
            Log.d("appsflash", "disconnected")
            connection.disconnect()
        }

    }



    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pack = sbn.packageName
        Log.i("appsflash", "onNotificationPosted: "+pack)
        sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        val strobeTime = sp.getInt("strobe_length", 5000)


        Log.i("appsflash", "strobe load:  "+pack+" "+load(pack))


        notificationScope.launch(Dispatchers.IO)  {

            if (load(pack)) {


                    Log.i("appsflash","Strobe Time: "+strobeTime)

                    //start strobe
                    mCameraImpl.startStroboscope()
                    Log.i("appsflash", "FLASH STARTED => "+pack)
                    stroberunning = true
                    //stop strobe at set timer
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            stroberunning = false
                            mCameraImpl.stopStroboscope()
                            StrobeLight()
                        Log.i("appsflash", "FLASH STOPPED => "+pack)
                    }
                    }, strobeTime.toLong())
                    //start script
                    //StrobeLight()


            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.i("appsflash", "Notification Removed ${sbn.packageName}")
        if (!stroberunning) {
            Log.i("appsflash","Flash stopped on Notification Removed: "+sbn.packageName)
            mCameraImpl.stopStroboscope()
        }

    }

}
