package com.example

import com.example.data.model.BookItem
import com.example.data.model.StationeryItem
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolLogicUnitTest {

    @Test
    fun student_ageCalculation_returnsExpectedAge() {
        val student = Student(dob = "2016-01-01")
        val age = student.calculatedAgeYears()
        assertNotNull(age)
        assertTrue(age!! >= 9)
    }

    @Test
    fun testRecord_gradeAndPassFail_calculatedAccurately() {
        val pctAPlus = TestRecord.calculatePercentage(92.0, 100.0)
        assertEquals(92.0, pctAPlus, 0.01)
        assertEquals("A+", TestRecord.calculateGrade(pctAPlus))
        assertTrue(TestRecord.isPass(pctAPlus))

        val pctFail = TestRecord.calculatePercentage(25.0, 100.0)
        assertEquals("F", TestRecord.calculateGrade(pctFail))
        assertFalse(TestRecord.isPass(pctFail))
    }

    @Test
    fun inventory_availableStock_calculatesRemainingAccurately() {
        val book = BookItem(quantity = 40, distributed = 15)
        assertEquals(25L, book.availableStock)

        val item = StationeryItem(quantity = 30, distributed = 35)
        assertEquals(0L, item.availableStock)
    }

    @Test
    fun dateUtils_weekAndMonthMatching_worksCorrectly() {
        val (start, end) = DateUtils.currentWeekRange()
        assertTrue(start <= end)
        assertTrue(DateUtils.isDateInMonth("2026-11-12", 11, 2026))
        assertFalse(DateUtils.isDateInMonth("2026-10-12", 11, 2026))
    }
}
