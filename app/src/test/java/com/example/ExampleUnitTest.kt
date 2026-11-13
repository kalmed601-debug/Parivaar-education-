package com.example

import com.example.data.model.BookItem
import com.example.data.model.StationeryItem
import com.example.data.model.Student
import com.example.data.model.TestRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testStudentAgeCalculation() {
        val student = Student(dob = "2018-05-15")
        val age = student.calculatedAgeYears()
        assertNotNull(age)
        assertTrue(age!! >= 5)
    }

    @Test
    fun testExamGradeCalculation() {
        assertEquals("A+", TestRecord.calculateGrade(95.0))
        assertEquals("A", TestRecord.calculateGrade(85.0))
        assertEquals("B+", TestRecord.calculateGrade(75.0))
        assertEquals("B", TestRecord.calculateGrade(65.0))
        assertEquals("C", TestRecord.calculateGrade(55.0))
        assertEquals("D", TestRecord.calculateGrade(40.0))
        assertEquals("F", TestRecord.calculateGrade(25.0))
    }

    @Test
    fun testExamPassStatus() {
        assertTrue(TestRecord.isPass(TestRecord.calculatePercentage(40.0, 50.0)))
        assertTrue(TestRecord.isPass(TestRecord.calculatePercentage(18.0, 50.0))) // 36% >= 35%
        assertFalse(TestRecord.isPass(TestRecord.calculatePercentage(10.0, 50.0))) // 20% < 35%
    }

    @Test
    fun testBookAvailabilityLogic() {
        val book = BookItem(
            id = "bk_test",
            bookName = "Test Book",
            className = "Class 5",
            quantity = 50,
            distributed = 20
        )
        assertEquals(30L, book.availableStock)
        assertEquals(50L, book.distributed + book.availableStock)
    }

    @Test
    fun testStationeryAvailabilityLogic() {
        val item = StationeryItem(
            id = "st_test",
            itemName = "Pencils",
            category = "Stationery",
            quantity = 100,
            distributed = 40
        )
        assertEquals(60L, item.availableStock)
    }
}
