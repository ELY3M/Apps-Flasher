package own.appslister

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.widget.CheckBox
import android.widget.Toast
import android.preference.PreferenceManager
import android.util.Log


class AppAdapter(private val context: Context, private var appModelList: ArrayList<AppModel>) :
    RecyclerView.Adapter<AppAdapter.ViewHolder>() {


    private lateinit var sp: SharedPreferences
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val appNameTxt: TextView = itemView.findViewById(R.id.list_app_name)
        val appPackageNameTxt: TextView = itemView.findViewById(R.id.app_package)
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val toggle: CheckBox = itemView.findViewById(R.id.app_toggle)

    }


    private fun load(key: String): Boolean {
        sp = PreferenceManager.getDefaultSharedPreferences(context)
        return sp.getBoolean(key, false)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.installed_app_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.appNameTxt.text = appModelList[position].getName()
        holder.appIcon.setImageDrawable(appModelList[position].getIcon())
        holder.appPackageNameTxt.text = appModelList[position].getPackages()
        holder.toggle.setChecked(load(holder.appPackageNameTxt.text.toString()))


        holder.itemView.setOnClickListener {
            val dialogListTitle = arrayOf("Open App", "App Info")
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setTitle("Choose Action")
                .setItems(
                    dialogListTitle
                ) { _, which ->
                    when (which) {
                        0 -> {
                            val intent =
                                context.packageManager.getLaunchIntentForPackage(appModelList[position].getPackages())
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context,"System app is not open for any reason.",Toast.LENGTH_LONG).show()
                            }
                        }
                        1 -> {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            intent.data =
                                Uri.parse("package:${appModelList[position].getPackages()}")
                            context.startActivity(intent)
                        }
                    }
                }
            builder.show()
        }


        fun save(isChecked: Boolean, key: String) {
            sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.putBoolean(key, isChecked)
            editor.apply()
        }

        holder.toggle.setOnClickListener { view ->

            Toast.makeText(context, holder.appPackageNameTxt.text.toString()+"  "+holder.toggle.isChecked.toString(), Toast.LENGTH_SHORT).show()

            sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()

            if (holder.toggle.isChecked) {
                save(holder.toggle.isChecked, holder.appPackageNameTxt.text.toString())
                Toast.makeText(context, holder.appPackageNameTxt.text.toString()+"  added to Notification Alert", Toast.LENGTH_SHORT).show()
            } else {
                editor.remove(holder.appPackageNameTxt.text.toString()).apply()
                Toast.makeText(context, holder.appPackageNameTxt.text.toString()+"  removed from Notification Alert", Toast.LENGTH_SHORT).show()
                Log.i("appsflash", "app deleted: "+holder.appPackageNameTxt.text.toString())
            }


        }


    }

    override fun getItemCount(): Int {
        return appModelList.size
    }
}
