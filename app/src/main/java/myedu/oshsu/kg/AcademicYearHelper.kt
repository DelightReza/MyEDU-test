package myedu.oshsu.kg

import java.util.Calendar

object AcademicYearHelper {
    /**
     * Calculates the default active year ID based on the current date.
     * Academic year starts in September.
     * 
     * Examples:
     * - January 2026 → returns 25 (still in 2025-2026 academic year)
     * - September 2026 → returns 26 (new 2026-2027 academic year starts)
     * - August 2026 → returns 25 (still in 2025-2026 academic year)
     * 
     * @return Last 2 digits of the academic year
     */
    fun getDefaultActiveYearId(): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based (0 = January, 8 = September)
        
        // If we're in September (month 8) or later, use current year
        // Otherwise, use previous year
        val academicYear = if (currentMonth >= Calendar.SEPTEMBER) {
            currentYear
        } else {
            currentYear - 1
        }
        
        // Return last 2 digits
        return academicYear % 100
    }
}
