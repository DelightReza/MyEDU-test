package myedu.oshsu.kg.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import myedu.oshsu.kg.ScheduleItem
import myedu.oshsu.kg.SessionResponse

class MyEduRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val scheduleDao = database.scheduleDao()
    private val gradeDao = database.gradeDao()
    
    // Schedule operations
    fun getAllSchedules(): Flow<List<ScheduleItem>> {
        return scheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toScheduleItem() }
        }
    }
    
    suspend fun getAllSchedulesSync(): List<ScheduleItem> {
        return scheduleDao.getAllSchedulesSync().map { it.toScheduleItem() }
    }
    
    fun getSchedulesByDay(day: Int): Flow<List<ScheduleItem>> {
        return scheduleDao.getSchedulesByDay(day).map { entities ->
            entities.map { it.toScheduleItem() }
        }
    }
    
    suspend fun updateSchedules(schedules: List<ScheduleItem>) {
        val entities = schedules.map { it.toEntity() }
        scheduleDao.replaceAll(entities)
    }
    
    // Grade operations
    fun getAllGrades(): Flow<List<SessionResponse>> {
        return gradeDao.getAllGrades().map { entities ->
            listOf(entities.toSessionResponse())
        }
    }
    
    suspend fun getAllGradesSync(): SessionResponse {
        return gradeDao.getAllGradesSync().toSessionResponse()
    }
    
    suspend fun updateGrades(session: SessionResponse) {
        val entities = session.toEntities()
        gradeDao.replaceAll(entities)
    }
}
