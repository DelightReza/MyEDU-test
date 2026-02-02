package myedu.oshsu.kg.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import myedu.oshsu.kg.MainActivity
import myedu.oshsu.kg.PrefsManager
import myedu.oshsu.kg.R

class ScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }
    
    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current
        val prefs = PrefsManager(context)
        
        // Load schedule and timeMap with fallback to SharedPreferences
        // Note: In widgets, we can't use suspend functions directly, so we use blocking calls
        // This is acceptable in widgets as they run in a background process
        val schedule = try {
            // Try Room Database first
            prefs.loadList<myedu.oshsu.kg.ScheduleItem>("schedule_list")
        } catch (e: Exception) {
            prefs.loadList<myedu.oshsu.kg.ScheduleItem>("schedule_list")
        }
        
        val timeMap = try {
            // Try Room Database via SharedPreferences cache
            val timeMapJson = prefs.loadData("time_map", String::class.java)
            parseTimeMap(timeMapJson)
        } catch (e: Exception) {
            emptyMap()
        }
        
        val language = prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en"
        
        val nextClassInfo = WidgetHelper.findNextClass(schedule, timeMap)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (nextClassInfo != null) {
                val (nextClass, timeString) = nextClassInfo
                
                Text(
                    text = context.getString(R.string.next_class),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(GlanceTheme.colors.onBackground)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = nextClass.subject?.get(language) ?: "Unknown",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(GlanceTheme.colors.primary)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Text(
                    text = timeString,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(GlanceTheme.colors.onBackground)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                val roomName = nextClass.room?.name_en ?: "?"
                val buildingName = nextClass.classroom?.building?.getName(language) ?: ""
                val location = if (buildingName.isNotBlank()) "$buildingName, $roomName" else roomName
                
                Text(
                    text = location,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(GlanceTheme.colors.onBackground)
                    )
                )
            } else {
                Text(
                    text = context.getString(R.string.no_classes),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(GlanceTheme.colors.onBackground)
                    )
                )
            }
        }
    }
    
    private fun parseTimeMap(json: String?): Map<Int, String> {
        if (json == null) return emptyMap()
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<Map<Int, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
