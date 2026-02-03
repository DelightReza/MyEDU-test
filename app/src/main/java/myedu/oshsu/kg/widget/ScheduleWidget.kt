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
        
        // Get display metrics to adapt to screen size
        val displayMetrics = context.resources.displayMetrics
        val isTablet = displayMetrics.widthPixels >= 600 * displayMetrics.density
        
        // Scale font sizes for tablets
        val headerSize = if (isTablet) 18.sp else 14.sp
        val timeSize = if (isTablet) 15.sp else 11.sp
        val subjectSize = if (isTablet) 16.sp else 12.sp
        val locationSize = if (isTablet) 14.sp else 10.sp
        val noClassSize = if (isTablet) 16.sp else 12.sp
        
        // Load schedule and timeMap with fallback to SharedPreferences
        // Note: In widgets, we can't use suspend functions directly, so we use blocking calls
        // This is acceptable in widgets as they run in a background process
        val schedule = try {
            prefs.loadList<myedu.oshsu.kg.ScheduleItem>("schedule_list")
        } catch (e: Exception) {
            emptyList()
        }
        
        val timeMap = try {
            // Get raw JSON string from SharedPreferences - use application context to ensure consistency
            val appPrefs = context.applicationContext.getSharedPreferences("myedu_offline_cache", Context.MODE_PRIVATE)
            val timeMapJson = appPrefs.getString("time_map", null)
            parseTimeMap(timeMapJson)
        } catch (e: Exception) {
            emptyMap()
        }
        
        val language = try {
            val appPrefs = context.applicationContext.getSharedPreferences("myedu_offline_cache", Context.MODE_PRIVATE)
            appPrefs.getString("language_pref", "\"en\"")?.replace("\"", "") ?: "en"
        } catch (e: Exception) {
            "en"
        }
        
        // Get all classes for today (with 8 PM logic)
        val todayClasses = WidgetHelper.getTodayClasses(schedule)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Text(
                text = context.getString(R.string.todays_classes),
                style = TextStyle(
                    fontSize = headerSize,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            if (todayClasses.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_classes),
                    style = TextStyle(
                        fontSize = noClassSize,
                        color = GlanceTheme.colors.onBackground
                    )
                )
            } else {
                // Show all classes
                todayClasses.forEachIndexed { index, classItem ->
                    if (index > 0) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                    
                    ClassRow(classItem, timeMap, language, context, timeSize, subjectSize, locationSize)
                }
            }
        }
    }
    
    @Composable
    private fun ClassRow(
        classItem: myedu.oshsu.kg.ScheduleItem,
        timeMap: Map<Int, String>,
        language: String,
        context: Context,
        timeSize: androidx.compose.ui.unit.TextUnit,
        subjectSize: androidx.compose.ui.unit.TextUnit,
        locationSize: androidx.compose.ui.unit.TextUnit
    ) {
        val timeString = timeMap[classItem.id_lesson] ?: "N/A"
        val subjectName = classItem.subject?.get(language) ?: "Unknown"
        val roomName = classItem.room?.name_en ?: "?"
        
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Time
            Text(
                text = timeString,
                style = TextStyle(
                    fontSize = timeSize,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.primary
                )
            )
            
            Spacer(modifier = GlanceModifier.height(2.dp))
            
            // Subject
            Text(
                text = subjectName,
                style = TextStyle(
                    fontSize = subjectSize,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onBackground
                )
            )
            
            Spacer(modifier = GlanceModifier.height(2.dp))
            
            // Location
            val buildingName = classItem.classroom?.building?.getName(language) ?: ""
            val location = if (buildingName.isNotBlank()) "$buildingName, $roomName" else "Room $roomName"
            
            Text(
                text = location,
                style = TextStyle(
                    fontSize = locationSize,
                    color = GlanceTheme.colors.onBackground
                )
            )
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
