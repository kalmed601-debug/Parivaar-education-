package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceRecord
import com.example.data.model.BookItem
import com.example.data.model.NoticeItem
import com.example.data.model.StationeryItem
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SchoolRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveStudentAndObserve_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = SchoolRepository(firestore, auth)

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveStudent(
                Student(
                    name = "Riya Sharma",
                    fatherName = "Suresh Sharma",
                    motherName = "Geeta Sharma",
                    className = "Class 5",
                    rollNumber = "501",
                    dob = "2015-06-15",
                    gender = "Girl",
                    village = "Chikalpat",
                    parentMobile = "9876543210"
                )
            )
        }
        assertTrue(saveRes.isSuccess)
        val studentId = saveRes.getOrThrow()

        val students = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudents().first { list -> list.any { it.id == studentId } }
        }
        val saved = students.first { it.id == studentId }
        assertEquals("Riya Sharma", saved.name)
        assertEquals("Class 5", saved.className)
    }

    @Test
    fun saveAttendanceTestsBooksStationeryNotices_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = SchoolRepository(firestore, auth)

        val studentId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveStudent(
                Student(
                    name = "Aman Verma",
                    className = "JNV",
                    rollNumber = "J01",
                    gender = "Boy"
                )
            ).getOrThrow()
        }

        val attRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveAttendanceBatch(
                date = "2026-11-12",
                className = "JNV",
                entries = listOf(
                    AttendanceRecord(
                        date = "2026-11-12",
                        className = "JNV",
                        studentId = studentId,
                        studentName = "Aman Verma",
                        rollNumber = "J01",
                        status = "PRESENT"
                    )
                ),
                existingMap = emptyMap()
            )
        }
        assertTrue(attRes.isSuccess)

        val testRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveTestRecord(
                TestRecord(
                    studentId = studentId,
                    studentName = "Aman Verma",
                    className = "JNV",
                    subject = "Arithmetic",
                    testTitle = "JNV Mock Test 1",
                    testType = "JNV Mock Test",
                    testDate = "2026-11-12",
                    totalMarks = 100.0,
                    obtainedMarks = 86.0
                )
            )
        }
        assertTrue(testRes.isSuccess)

        val bookRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveBook(
                BookItem(
                    bookName = "Navodaya Ganit",
                    className = "JNV",
                    subject = "Mathematics",
                    quantity = 30,
                    distributed = 12
                )
            )
        }
        assertTrue(bookRes.isSuccess)

        val statRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveStationery(
                StationeryItem(
                    itemName = "Geometry Box",
                    category = "Geometry & Art",
                    quantity = 50,
                    distributed = 10,
                    purchasePrice = 85.0
                )
            )
        }
        assertTrue(statRes.isSuccess)

        val noticeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveNotice(
                NoticeItem(
                    title = "JNV Mock Exam Tomorrow",
                    message = "All JNV students report at 9 AM.",
                    targetAudience = "Students",
                    priority = "Urgent",
                    dateStr = "2026-11-12",
                    authorName = "Admin"
                )
            )
        }
        assertTrue(noticeRes.isSuccess)
    }

    @Test
    fun crossUserRead_deniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SchoolRepository(firestore, auth)
        val studentId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveStudent(
                Student(
                    name = "Private Student",
                    className = "Class 1",
                    rollNumber = "1",
                    gender = "Boy"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("students").document(studentId)
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED for cross-user read")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe!!.code)
        }
    }

    @Test
    fun observeStudents_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document("some_uid")
                    .collection("students")
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated read")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe!!.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_parivar@test.com"
        const val BOB_EMAIL = "bob_parivar@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
