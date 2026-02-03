package myedu.oshsu.kg

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

class CalendarSyncHelper(private val context: Context) {
    
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Syncs the schedule to the system calendar
     * Returns the number of events added or -1 on error
     */
    fun syncScheduleToCalendar(
        schedule: List<ScheduleItem>,
        timeMap: Map<Int, String>,
        language: String = "en"
    ): Int {
        if (!hasCalendarPermission()) {
            return -1
        }
        
        try {
            val calendarId = getOrCreateMyEduCalendar() ?: return -1
            
            // Clear existing MyEDU events
            clearMyEduEvents(calendarId)
            
            // Add new recurring events (once per class, RRULE handles repetition)
            var addedCount = 0
            val currentWeek = Calendar.getInstance()
            
            for (item in schedule) {
                val timeString = timeMap[item.id_lesson] ?: continue
                val times = parseTimeString(timeString) ?: continue
                
                val eventCalendar = getCalendarForScheduleItem(item, times, currentWeek, 0)
                if (eventCalendar != null) {
                    try {
                        insertEvent(calendarId, item, eventCalendar, language)
                        addedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with other events even if one fails
                    }
                }
            }
            
            return addedCount
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }
    
    private fun getOrCreateMyEduCalendar(): Long? {
        // Try to find existing MyEDU calendar
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME
        )
        
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf("MyEDU", CalendarContract.ACCOUNT_TYPE_LOCAL),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        
        // Create new calendar
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "MyEDU")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "MyEDU Schedule")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "MyEDU Schedule")
            put(CalendarContract.Calendars.CALENDAR_COLOR, -14575885) // Blue color
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "MyEDU")
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0)
            put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1)
        }
        
        val uri = try {
            context.contentResolver.insert(CalendarContract.Calendars.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        return uri?.lastPathSegment?.toLongOrNull()
    }
    
    private fun clearMyEduEvents(calendarId: Long) {
        context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString())
        )
    }
    
    private fun insertEvent(
        calendarId: Long,
        item: ScheduleItem,
        eventCalendar: Calendar,
        language: String
    ) {
        val endCalendar = eventCalendar.clone() as Calendar
        endCalendar.add(Calendar.MINUTE, 90) // Assume 90 minute class duration
        
        val subjectName = item.subject?.get(language) ?: "Class"
        val teacherName = item.teacher?.get() ?: ""
        val roomName = item.room?.name_en ?: ""
        val buildingName = item.classroom?.building?.getName(language) ?: ""
        
        val title = subjectName
        val location = if (buildingName.isNotBlank()) "$buildingName, Room $roomName" else "Room $roomName"
        val description = buildString("Teacher: $teacherName", 
            "Type: ${item.subject_type?.get(language) ?: ""}")
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, eventCalendar.timeInMillis)
            put(CalendarContract.Events.DTEND, endCalendar.timeInMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            
            // Set recurring rule for weekly events (1 week only)
            put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=1")
        }
        
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }
    
    private fun getCalendarForScheduleItem(
        item: ScheduleItem,
        times: Pair<Int, Int>,
        currentWeek: Calendar,
        weekOffset: Int
    ): Calendar? {
        val cal = currentWeek.clone() as Calendar
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        
        // Set to beginning of week (Monday)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        // Add days based on schedule day (0 = Monday, 1 = Tuesday, etc.)
        cal.add(Calendar.DAY_OF_WEEK, item.day)
        
        // Set time
        cal.set(Calendar.HOUR_OF_DAY, times.first)
        cal.set(Calendar.MINUTE, times.second)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        // Only add events that are in the future
        if (cal.timeInMillis < System.currentTimeMillis() && weekOffset == 0) {
            return null
        }
        
        return cal
    }
    
    private fun parseTimeString(timeString: String): Pair<Int, Int>? {
        val startTime = timeString.split("-").firstOrNull()?.trim() ?: return null
        val parts = startTime.split(":")
        if (parts.size < 2) return null
        
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        
        return Pair(hour, minute)
    }
    
    private fun buildString(vararg parts: String): String {
        return parts.filter { it.isNotBlank() }.joinToString("\n")
    }
}
