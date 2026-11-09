package com.example.carlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private lateinit var grid: GridView

    // Mots-clés pour mettre CarPlay et tes applis favorites en premier
    private val priority = listOf("carplay", "zlink", "carlink", "maps", "spotify", "music", "phone")

    data class AppItem(val label: String, val pkg: String, val icon: android.graphics.drawable.Drawable)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0F14"))
            setPadding(dp(32), dp(20), dp(32), dp(16))
        }

        // Header : heure + date
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val clock = TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "HH:mm"
            textSize = 56f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        val date = TextClock(this).apply {
            format24Hour = "EEEE d MMMM"; format12Hour = "EEEE d MMMM"
            textSize = 20f; setTextColor(Color.parseColor("#8FA3B8"))
            setPadding(dp(24), 0, 0, 0)
        }
        header.addView(clock); header.addView(date)

        grid = GridView(this).apply {
            numColumns = 6
            verticalSpacing = dp(16)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }

        root.addView(header)
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(16) })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        grid.adapter = Adapter(loadApps())
    }

    private fun loadApps(): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = packageManager
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .sortedWith(compareBy({ app ->
                val i = priority.indexOfFirst { app.label.lowercase().contains(it) || app.pkg.lowercase().contains(it) }
                if (i == -1) Int.MAX_VALUE else i
            }, { it.label.lowercase() }))
    }

    inner class Adapter(private val apps: List<AppItem>) : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(p: Int) = apps[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(p: Int, convert: View?, parent: ViewGroup): View {
            val app = apps[p]
            val tile = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(12), dp(8), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.parseColor("#141B24"))
                }
            }
            tile.addView(ImageView(context).apply { setImageDrawable(app.icon) }, LinearLayout.LayoutParams(dp(72), dp(72)))
            tile.addView(TextView(context).apply {
                text = app.label; textSize = 14f; maxLines = 1
                setTextColor(Color.WHITE); gravity = Gravity.CENTER
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            tile.setOnClickListener {
                packageManager.getLaunchIntentForPackage(app.pkg)?.let { startActivity(it) }
            }
            return tile
        }
    }

    // Empêche le bouton retour de quitter le launcher
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}
}
