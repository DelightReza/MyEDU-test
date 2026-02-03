package myedu.oshsu.kg.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
    
    // Define different size modes for the widget
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            // Small: 2x2 cells (compact view, 1-2 classes)
            androidx.glance.appwidget.unit.DpSize(180.dp, 110.dp),
            // Medium: 4x2 cells (normal view, 2-3 classes)
            androidx.glance.appwidget.unit.DpSize(250.dp, 110.dp),
            // Large: 4x3 cells (expanded view, more classes)
            androidx.glance.appwidget.unit.DpSize(250.dp, 200.dp),
            // Extra Large: 4x4+ cells (full view, all classes)
            androidx.glance.appwidget.unit.DpSize(250.dp, 300.dp)
        )
    )
    
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
        val size = LocalSize.current
        val prefs = PrefsManager(context)
        
        // Determine widget size category
        val widgetHeight = size.height.value
        val widgetWidth = size.width.value
        val isSmall = widgetHeight < 150 && widgetWidth < 220
        val isMedium = widgetHeight < 180 && !isSmall
        val isLarge = widgetHeight < 280 && !isSmall && !isMedium
        // Extra large is anything bigger
        
        // Get display metrics to adapt to screen size (tablet vs phone)
        val displayMetrics = context.resources.displayMetrics
        val isTablet = displayMetrics.widthPixels >= 600 * displayMetrics.density
        
        // Scale font sizes based on both widget size and device type
        val scaleFactor = when {
            isSmall -> 0.85f
            isMedium -> 1.0f
            isLarge -> 1.1f
            else -> 1.2f
        }
        
        val tabletMultiplier = if (isTablet) 1.3f else 1.0f
        val totalScale = scaleFactor * tabletMultiplier
        
        val headerSize = (14 * totalScale).sp
        val timeSize = (11 * totalScale).sp
        val subjectSize = (12 * totalScale).sp
        val locationSize = (10 * totalScale).sp
        val noClassSize = (12 * totalScale).sp
        
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
        
        // Limit classes shown based on widget size
        val maxClassesToShow = when {
            isSmall -> 1  // Compact: show only next class
            isMedium -> 2  // Normal: show 2 classes
            isLarge -> 4   // Large: show up to 4 classes
            else -> Int.MAX_VALUE  // Extra large: show all classes
        }
        val classesToDisplay = todayClasses.take(maxClassesToShow)
        val hasMoreClasses = todayClasses.size > maxClassesToShow
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header with count indicator
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.todays_classes),
                    style = TextStyle(
                        fontSize = headerSize,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary
                    )
                )
                if (!isSmall && todayClasses.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "(${todayClasses.size})",
                        style = TextStyle(
                            fontSize = (headerSize.value * 0.8).sp,
                            color = GlanceTheme.colors.secondary
                        )
                    )
                }
            }
            
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
                // Show limited classes based on widget size
                classesToDisplay.forEachIndexed { index, classItem ->
                    if (index > 0) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                    
                    ClassRow(classItem, timeMap, language, context, timeSize, subjectSize, locationSize, isSmall)
                }
                
                // Show "and X more" indicator if there are hidden classes
                if (hasMoreClasses && !isSmall) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.and_more, todayClasses.size - maxClassesToShow),
                        style = TextStyle(
                            fontSize = (locationSize.value * 0.9).sp,
                            color = GlanceTheme.colors.secondary
                        )
                    )
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
        locationSize: androidx.compose.ui.unit.TextUnit,
        isCompact: Boolean = false
    ) {
        val timeString = timeMap[classItem.id_lesson] ?: "N/A"
        val subjectName = classItem.subject?.get(language) ?: "Unknown"
        val roomName = classItem.room?.name_en ?: "?"
        
        if (isCompact) {
            // Compact layout for small widgets - show all in one line
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    style = TextStyle(
                        fontSize = timeSize,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary
                    )
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = subjectName,
                        style = TextStyle(
                            fontSize = subjectSize,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onBackground
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = roomName,
                        style = TextStyle(
                            fontSize = (locationSize.value * 0.9).sp,
                            color = GlanceTheme.colors.secondary
                        ),
                        maxLines = 1
                    )
                }
            }
        } else {
            // Normal layout for larger widgets
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
