package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object SchoolConstants {
    val ALL_CLASSES = listOf(
        "Anganwadi",
        "Class 1",
        "Class 2",
        "Class 3",
        "Class 4",
        "Class 5",
        "Class 6",
        "Class 7",
        "Class 8",
        "JNV"
    )

    val REGULAR_CLASSES = listOf(
        "Anganwadi",
        "Class 1",
        "Class 2",
        "Class 3",
        "Class 4",
        "Class 5",
        "Class 6",
        "Class 7",
        "Class 8"
    )

    val GENDERS = listOf("Boy", "Girl", "Other")

    val TEST_TYPES = listOf(
        "Regular",
        "Unit Test",
        "Mid-Term",
        "Final",
        "JNV Subject Test",
        "JNV Mock Test",
        "JNV Entrance Prep"
    )

    val JNV_SUBJECTS = listOf(
        "Mental Ability (MAT)",
        "Arithmetic",
        "Language (Hindi/English)",
        "Full Mock Exam"
    )

    val SCHOOL_SUBJECTS = listOf(
        "Hindi",
        "English",
        "Mathematics",
        "Environmental Studies (EVS)",
        "Science",
        "Social Science",
        "Sanskrit",
        "General Knowledge",
        "Drawing & Craft",
        "Mental Ability (MAT)",
        "Arithmetic"
    )

    val STATIONERY_CATEGORIES = listOf(
        "Notebook",
        "Pen & Pencil",
        "Geometry & Art",
        "Educational Toy",
        "Sports & Games",
        "Teaching Aid",
        "Other"
    )

    val NOTICE_AUDIENCES = listOf("All", "Teachers", "Parents", "Students")
    val ROLES = listOf("Admin", "Staff", "Parent")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val schoolName: String = "Parivar Education Society",
    val branchLocation: String = "Chikalpat",
    val activeRole: String = "Admin",
    val language: String = "en",
    val academicYear: String = "2026-27",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val className: String = "Class 1",
    val rollNumber: String = "",
    val dob: String = "",
    val gender: String = "Boy",
    val village: String = "Chikalpat",
    val address: String = "",
    val parentMobile: String = "",
    val admissionDate: String = "",
    val photoBase64: String = "",
    val jnvTargetStream: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun calculatedAgeYears(): Int? {
        if (dob.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.isLenient = false
            val birthDate = sdf.parse(dob) ?: return null
            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age.coerceAtLeast(0)
        } catch (_: Exception) {
            null
        }
    }
}

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val className: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNumber: String = "",
    val status: String = "PRESENT", // PRESENT, ABSENT, HOLIDAY
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TestRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val className: String = "",
    val subject: String = "",
    val testTitle: String = "",
    val testType: String = "Regular",
    val testDate: String = "",
    val totalMarks: Double = 100.0,
    val obtainedMarks: Double = 0.0,
    val percentage: Double = 0.0,
    val grade: String = "C",
    val passed: Boolean = true,
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        fun calculatePercentage(obtained: Double, total: Double): Double {
            if (total <= 0.0) return 0.0
            val pct = (obtained / total) * 100.0
            return ((pct * 100.0).toInt() / 100.0).coerceIn(0.0, 100.0)
        }

        fun calculateGrade(percentage: Double): String = when {
            percentage >= 90.0 -> "A+"
            percentage >= 80.0 -> "A"
            percentage >= 70.0 -> "B+"
            percentage >= 60.0 -> "B"
            percentage >= 50.0 -> "C"
            percentage >= 33.0 -> "D"
            else -> "F"
        }

        fun isPass(percentage: Double): Boolean = percentage >= 33.0
    }
}

@IgnoreExtraProperties
data class BookItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookName: String = "",
    val className: String = "",
    val subject: String = "",
    val author: String = "",
    val publisher: String = "",
    val quantity: Long = 0L,
    val distributed: Long = 0L,
    val purchaseDate: String = "",
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val availableStock: Long
        get() = (quantity - distributed).coerceAtLeast(0L)
}

@IgnoreExtraProperties
data class StationeryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemName: String = "",
    val category: String = "Notebook",
    val details: String = "",
    val quantity: Long = 0L,
    val distributed: Long = 0L,
    val purchasePrice: Double = 0.0,
    val purchaseDate: String = "",
    val photoBase64: String = "",
    val stockHistorySummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val availableStock: Long
        get() = (quantity - distributed).coerceAtLeast(0L)
}

@IgnoreExtraProperties
data class NoticeItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val targetAudience: String = "All",
    val priority: String = "Normal",
    val dateStr: String = "",
    val authorName: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "STUDENT",
    val title: String = "",
    val description: String = "",
    val dateTimeLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StaffParentMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "Staff",
    val phone: String = "",
    val assignedClass: String = "",
    val linkedStudentId: String = "",
    val accessPin: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
