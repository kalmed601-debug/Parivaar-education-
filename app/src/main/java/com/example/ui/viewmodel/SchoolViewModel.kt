package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLog
import com.example.data.model.AttendanceRecord
import com.example.data.model.BookItem
import com.example.data.model.NoticeItem
import com.example.data.model.SchoolConstants
import com.example.data.model.StaffParentMember
import com.example.data.model.StationeryItem
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.example.data.model.UserProfile
import com.example.data.repository.SchoolRepository
import com.example.ui.i18n.AppLanguage
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataLoadState {
    data object Loading : DataLoadState
    data object Ready : DataLoadState
    data class Error(val message: String) : DataLoadState
}

data class DashboardSummary(
    val totalStudents: Int = 0,
    val totalClasses: Int = SchoolConstants.ALL_CLASSES.size,
    val totalBoys: Int = 0,
    val totalGirls: Int = 0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val totalPassTests: Int = 0,
    val totalFailTests: Int = 0,
    val classStudentCounts: Map<String, Int> = emptyMap(),
    val classTodayPresentCounts: Map<String, Int> = emptyMap(),
    val totalBooksStock: Long = 0L,
    val totalBooksAvailable: Long = 0L,
    val totalStationeryStock: Long = 0L,
    val totalStationeryAvailable: Long = 0L
)

data class SchoolUiState(
    val loadState: DataLoadState = DataLoadState.Loading,
    val profile: UserProfile = UserProfile(),
    val students: List<Student> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val tests: List<TestRecord> = emptyList(),
    val books: List<BookItem> = emptyList(),
    val stationery: List<StationeryItem> = emptyList(),
    val notices: List<NoticeItem> = emptyList(),
    val activities: List<ActivityLog> = emptyList(),
    val members: List<StaffParentMember> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val language: AppLanguage = AppLanguage.ENGLISH,
    val activeRole: String = "Admin"
)

class SchoolViewModel(
    private val repository: SchoolRepository,
    private val currentUserId: String,
    initialDisplayName: String = "",
    initialEmail: String = ""
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _localLanguageOverride = MutableStateFlow<AppLanguage?>(null)
    private val _localRoleOverride = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = initialDisplayName.ifBlank { "Administrator" },
                email = initialEmail
            )
        }
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private val coreStreams = combine(
        repository.observeProfile(),
        repository.observeStudents(),
        repository.observeAttendance(),
        repository.observeTests(),
        repository.observeBooks()
    ) { profile, students, attendance, tests, books ->
        CoreBundle(profile, students, attendance, tests, books)
    }

    private val secondaryStreams = combine(
        repository.observeStationery(),
        repository.observeNotices(),
        repository.observeActivities(),
        repository.observeMembers()
    ) { stationery, notices, activities, members ->
        SecondaryBundle(stationery, notices, activities, members)
    }

    val uiState: StateFlow<SchoolUiState> = combine(
        coreStreams,
        secondaryStreams,
        _localLanguageOverride,
        _localRoleOverride
    ) { core, sec, langOverride, roleOverride ->
        val profile = core.profile ?: UserProfile(userId = currentUserId)
        val today = DateUtils.todayIso()
        val students = core.students.sortedWith(compareBy({ it.className }, { it.rollNumber }, { it.name }))
        val attendance = core.attendance.sortedByDescending { it.date }
        val tests = core.tests.sortedByDescending { it.testDate }
        val books = core.books.sortedBy { it.bookName }
        val stationery = sec.stationery.sortedBy { it.itemName }
        val notices = sec.notices.sortedByDescending { it.dateStr }
        val activities = sec.activities.sortedByDescending { it.createdAt?.seconds ?: 0L }
        val members = sec.members.sortedBy { it.name }

        val todayAtt = attendance.filter { it.date == today }
        val classCounts = SchoolConstants.ALL_CLASSES.associateWith { cls ->
            students.count { it.className == cls }
        }
        val classTodayPresent = SchoolConstants.ALL_CLASSES.associateWith { cls ->
            todayAtt.count { it.className == cls && it.status == "PRESENT" }
        }

        val summary = DashboardSummary(
            totalStudents = students.size,
            totalClasses = SchoolConstants.ALL_CLASSES.size,
            totalBoys = students.count { it.gender == "Boy" },
            totalGirls = students.count { it.gender == "Girl" },
            todayPresent = todayAtt.count { it.status == "PRESENT" },
            todayAbsent = todayAtt.count { it.status == "ABSENT" },
            totalPassTests = tests.count { it.passed },
            totalFailTests = tests.count { !it.passed },
            classStudentCounts = classCounts,
            classTodayPresentCounts = classTodayPresent,
            totalBooksStock = books.sumOf { it.quantity },
            totalBooksAvailable = books.sumOf { it.availableStock },
            totalStationeryStock = stationery.sumOf { it.quantity },
            totalStationeryAvailable = stationery.sumOf { it.availableStock }
        )

        val resolvedLang = langOverride ?: if (profile.language == "hi") AppLanguage.HINDI else AppLanguage.ENGLISH
        val resolvedRole = roleOverride ?: profile.activeRole.ifBlank { "Admin" }

        SchoolUiState(
            loadState = DataLoadState.Ready,
            profile = profile,
            students = students,
            attendance = attendance,
            tests = tests,
            books = books,
            stationery = stationery,
            notices = notices,
            activities = activities,
            members = members,
            summary = summary,
            language = resolvedLang,
            activeRole = resolvedRole
        )
    }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing school data", root)
            emit(
                SchoolUiState(
                    loadState = DataLoadState.Error(root.message ?: "Failed to sync school records")
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SchoolUiState(loadState = DataLoadState.Loading)
        )

    private fun emitMessage(msg: String) {
        _events.tryEmit(msg)
    }

    fun toggleLanguage() {
        val current = uiState.value.language
        val next = if (current == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        setLanguage(next)
    }

    fun setLanguage(language: AppLanguage) {
        _localLanguageOverride.value = language
        val prof = uiState.value.profile
        viewModelScope.launch {
            repository.updateUserProfileSettings(
                schoolName = prof.schoolName,
                branchLocation = prof.branchLocation,
                activeRole = uiState.value.activeRole,
                language = language.code,
                academicYear = prof.academicYear
            )
        }
    }

    fun setActiveRole(role: String) {
        _localRoleOverride.value = role
        val prof = uiState.value.profile
        viewModelScope.launch {
            repository.updateUserProfileSettings(
                schoolName = prof.schoolName,
                branchLocation = prof.branchLocation,
                activeRole = role,
                language = uiState.value.language.code,
                academicYear = prof.academicYear
            ).onSuccess {
                emitMessage("Switched role view to $role")
            }
        }
    }

    fun updateSchoolSettings(schoolName: String, branchLocation: String, academicYear: String) {
        viewModelScope.launch {
            repository.updateUserProfileSettings(
                schoolName = schoolName,
                branchLocation = branchLocation,
                activeRole = uiState.value.activeRole,
                language = uiState.value.language.code,
                academicYear = academicYear
            ).onSuccess {
                emitMessage("School settings saved")
            }.onFailure {
                emitMessage(it.message ?: "Failed to save settings")
            }
        }
    }

    fun saveStudent(student: Student) {
        viewModelScope.launch {
            repository.saveStudent(student)
                .onSuccess { emitMessage("Student saved: ${student.name}") }
                .onFailure { emitMessage(it.message ?: "Failed to save student") }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student.id)
                .onSuccess { emitMessage("Deleted student: ${student.name}") }
                .onFailure { emitMessage(it.message ?: "Failed to delete student") }
        }
    }

    fun saveAttendance(date: String, className: String, records: List<AttendanceRecord>) {
        viewModelScope.launch {
            val existingById = uiState.value.attendance.associateBy { it.id }
            repository.saveAttendanceBatch(date, className, records, existingById)
                .onSuccess { emitMessage("Attendance saved for $className ($date)") }
                .onFailure { emitMessage(it.message ?: "Failed to save attendance") }
        }
    }

    fun saveTestRecord(test: TestRecord) {
        viewModelScope.launch {
            repository.saveTestRecord(test)
                .onSuccess { emitMessage("Test result saved for ${test.studentName}") }
                .onFailure { emitMessage(it.message ?: "Failed to save test result") }
        }
    }

    fun deleteTestRecord(testId: String) {
        viewModelScope.launch {
            repository.deleteTestRecord(testId)
                .onSuccess { emitMessage("Test record deleted") }
                .onFailure { emitMessage(it.message ?: "Failed to delete test") }
        }
    }

    fun saveBook(book: BookItem) {
        viewModelScope.launch {
            repository.saveBook(book)
                .onSuccess { emitMessage("Book saved: ${book.bookName}") }
                .onFailure { emitMessage(it.message ?: "Failed to save book") }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
                .onSuccess { emitMessage("Book removed") }
                .onFailure { emitMessage(it.message ?: "Failed to delete book") }
        }
    }

    fun saveStationery(item: StationeryItem) {
        viewModelScope.launch {
            repository.saveStationery(item)
                .onSuccess { emitMessage("Stationery item saved: ${item.itemName}") }
                .onFailure { emitMessage(it.message ?: "Failed to save stationery") }
        }
    }

    fun deleteStationery(itemId: String) {
        viewModelScope.launch {
            repository.deleteStationery(itemId)
                .onSuccess { emitMessage("Stationery item removed") }
                .onFailure { emitMessage(it.message ?: "Failed to delete stationery") }
        }
    }

    fun saveNotice(notice: NoticeItem) {
        viewModelScope.launch {
            repository.saveNotice(notice)
                .onSuccess { emitMessage("Notice saved") }
                .onFailure { emitMessage(it.message ?: "Failed to save notice") }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            repository.deleteNotice(noticeId)
                .onSuccess { emitMessage("Notice deleted") }
                .onFailure { emitMessage(it.message ?: "Failed to delete notice") }
        }
    }

    fun saveMember(member: StaffParentMember) {
        viewModelScope.launch {
            repository.saveMember(member)
                .onSuccess { emitMessage("Member saved: ${member.name}") }
                .onFailure { emitMessage(it.message ?: "Failed to save member") }
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
                .onSuccess { emitMessage("Member removed") }
                .onFailure { emitMessage(it.message ?: "Failed to remove member") }
        }
    }

    fun buildBackupJson(): String {
        val state = uiState.value
        return repository.exportAllToJson(
            students = state.students,
            attendance = state.attendance,
            tests = state.tests,
            books = state.books,
            stationery = state.stationery,
            notices = state.notices,
            members = state.members
        )
    }

    fun restoreFromJson(jsonString: String) {
        viewModelScope.launch {
            repository.restoreFromJson(jsonString)
                .onSuccess { count -> emitMessage("Restored $count records from backup") }
                .onFailure { emitMessage("Restore failed: ${it.message}") }
        }
    }

    private data class CoreBundle(
        val profile: UserProfile?,
        val students: List<Student>,
        val attendance: List<AttendanceRecord>,
        val tests: List<TestRecord>,
        val books: List<BookItem>
    )

    private data class SecondaryBundle(
        val stationery: List<StationeryItem>,
        val notices: List<NoticeItem>,
        val activities: List<ActivityLog>,
        val members: List<StaffParentMember>
    )

    private companion object {
        const val TAG = "SchoolViewModel"
    }
}
