package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ActivityLog
import com.example.data.model.AttendanceRecord
import com.example.data.model.BookItem
import com.example.data.model.NoticeItem
import com.example.data.model.StaffParentMember
import com.example.data.model.StationeryItem
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchoolRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun nowDateTimeLabel(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    }

    private fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun <T> observeSubcollection(
        collectionName: String,
        mapper: (DocumentSnapshot) -> T?
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/$collectionName"
        emitAll(
            userDoc(uid).collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull(mapper)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.take(100),
                    "email" to email.take(150),
                    "schoolName" to "Parivar Education Society",
                    "branchLocation" to "Chikalpat",
                    "activeRole" to "Admin",
                    "language" to "en",
                    "academicYear" to "2026-27",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfileSettings(
        schoolName: String,
        branchLocation: String,
        activeRole: String,
        language: String,
        academicYear: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to (auth.currentUser?.displayName ?: "Administrator").take(100),
                    "email" to (auth.currentUser?.email ?: "").take(150),
                    "schoolName" to schoolName.ifBlank { "Parivar Education Society" }.take(120),
                    "branchLocation" to branchLocation.ifBlank { "Chikalpat" }.take(120),
                    "activeRole" to activeRole,
                    "language" to language,
                    "academicYear" to academicYear.ifBlank { "2026-27" }.take(20),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "schoolName" to schoolName.ifBlank { "Parivar Education Society" }.take(120),
                        "branchLocation" to branchLocation.ifBlank { "Chikalpat" }.take(120),
                        "activeRole" to activeRole,
                        "language" to language,
                        "academicYear" to academicYear.ifBlank { "2026-27" }.take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Real-Time Streams ---
    fun observeStudents(): Flow<List<Student>> =
        observeSubcollection("students") {
            it.toObject(Student::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeAttendance(): Flow<List<AttendanceRecord>> =
        observeSubcollection("attendance") {
            it.toObject(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeTests(): Flow<List<TestRecord>> =
        observeSubcollection("tests") {
            it.toObject(TestRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeBooks(): Flow<List<BookItem>> =
        observeSubcollection("books") {
            it.toObject(BookItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeStationery(): Flow<List<StationeryItem>> =
        observeSubcollection("stationery") {
            it.toObject(StationeryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeNotices(): Flow<List<NoticeItem>> =
        observeSubcollection("notices") {
            it.toObject(NoticeItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeActivities(): Flow<List<ActivityLog>> =
        observeSubcollection("activities") {
            it.toObject(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeMembers(): Flow<List<StaffParentMember>> =
        observeSubcollection("members") {
            it.toObject(StaffParentMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    // --- Activity Logging ---
    suspend fun logActivity(type: String, title: String, description: String) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = userDoc(uid).collection("activities").document()
        val payload = mapOf(
            "userId" to uid,
            "type" to type,
            "title" to title.take(120),
            "description" to description.take(300),
            "dateTimeLabel" to nowDateTimeLabel(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    // --- Students ---
    suspend fun saveStudent(student: Student): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = student.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("students").document()
        } else {
            userDoc(uid).collection("students").document(student.id)
        }

        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to student.name.trim().take(100),
                    "fatherName" to student.fatherName.trim().take(100),
                    "motherName" to student.motherName.trim().take(100),
                    "className" to student.className,
                    "rollNumber" to student.rollNumber.trim().take(30),
                    "dob" to student.dob.trim().take(20),
                    "gender" to student.gender,
                    "village" to student.village.trim().take(100),
                    "address" to student.address.trim().take(300),
                    "parentMobile" to student.parentMobile.trim().take(20),
                    "admissionDate" to student.admissionDate.ifBlank { todayIsoDate() }.take(20),
                    "photoBase64" to student.photoBase64.take(120000),
                    "jnvTargetStream" to student.jnvTargetStream.trim().take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                logActivity("STUDENT", "New Student Added", "${student.name} (${student.className})")
            } else {
                val updates = mapOf(
                    "name" to student.name.trim().take(100),
                    "fatherName" to student.fatherName.trim().take(100),
                    "motherName" to student.motherName.trim().take(100),
                    "className" to student.className,
                    "rollNumber" to student.rollNumber.trim().take(30),
                    "dob" to student.dob.trim().take(20),
                    "gender" to student.gender,
                    "village" to student.village.trim().take(100),
                    "address" to student.address.trim().take(300),
                    "parentMobile" to student.parentMobile.trim().take(20),
                    "admissionDate" to student.admissionDate.trim().take(20),
                    "photoBase64" to student.photoBase64.take(120000),
                    "jnvTargetStream" to student.jnvTargetStream.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                logActivity("STUDENT", "Student Profile Updated", "${student.name} (${student.className})")
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStudent(studentId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("students").document(studentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Attendance ---
    suspend fun saveAttendanceBatch(
        date: String,
        className: String,
        entries: List<AttendanceRecord>,
        existingMap: Map<String, AttendanceRecord>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val col = userDoc(uid).collection("attendance")
        try {
            // Firestore batches allow up to 500 writes
            entries.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { entry ->
                    val safeDocId = "${date}_${entry.studentId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    val docRef = col.document(safeDocId)
                    val existing = existingMap[safeDocId]
                    if (existing == null) {
                        batch.set(
                            docRef,
                            mapOf(
                                "userId" to uid,
                                "date" to date,
                                "className" to className,
                                "studentId" to entry.studentId,
                                "studentName" to entry.studentName.take(100),
                                "rollNumber" to entry.rollNumber.take(30),
                                "status" to entry.status,
                                "remarks" to entry.remarks.take(200),
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    } else {
                        batch.update(
                            docRef,
                            mapOf(
                                "date" to date,
                                "className" to className,
                                "studentId" to entry.studentId,
                                "studentName" to entry.studentName.take(100),
                                "rollNumber" to entry.rollNumber.take(30),
                                "status" to entry.status,
                                "remarks" to entry.remarks.take(200),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                batch.commit().await()
            }
            val presentCount = entries.count { it.status == "PRESENT" }
            val holidayCount = entries.count { it.status == "HOLIDAY" }
            val summary = if (holidayCount == entries.size && entries.isNotEmpty()) {
                "$className marked Holiday on $date"
            } else {
                "$className on $date: $presentCount/${entries.size} Present"
            }
            logActivity("ATTENDANCE", "Attendance Updated", summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/attendance")
            throw e
        }
    }

    // --- Tests & Exams ---
    suspend fun saveTestRecord(test: TestRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = test.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("tests").document()
        } else {
            userDoc(uid).collection("tests").document(test.id)
        }
        val pct = TestRecord.calculatePercentage(test.obtainedMarks, test.totalMarks)
        val grade = TestRecord.calculateGrade(pct)
        val passed = TestRecord.isPass(pct)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "studentId" to test.studentId,
                    "studentName" to test.studentName.trim().take(100),
                    "className" to test.className,
                    "subject" to test.subject.trim().take(80),
                    "testTitle" to test.testTitle.trim().take(120),
                    "testType" to test.testType,
                    "testDate" to test.testDate.ifBlank { todayIsoDate() }.take(15),
                    "totalMarks" to test.totalMarks,
                    "obtainedMarks" to test.obtainedMarks,
                    "percentage" to pct,
                    "grade" to grade,
                    "passed" to passed,
                    "remarks" to test.remarks.trim().take(250),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                logActivity(
                    "TEST",
                    "Test Result Added",
                    "${test.studentName} - ${test.subject}: ${test.obtainedMarks.toInt()}/${test.totalMarks.toInt()} ($grade)"
                )
            } else {
                val updates = mapOf(
                    "studentId" to test.studentId,
                    "studentName" to test.studentName.trim().take(100),
                    "className" to test.className,
                    "subject" to test.subject.trim().take(80),
                    "testTitle" to test.testTitle.trim().take(120),
                    "testType" to test.testType,
                    "testDate" to test.testDate.trim().take(15),
                    "totalMarks" to test.totalMarks,
                    "obtainedMarks" to test.obtainedMarks,
                    "percentage" to pct,
                    "grade" to grade,
                    "passed" to passed,
                    "remarks" to test.remarks.trim().take(250),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                logActivity("TEST", "Test Result Updated", "${test.studentName} - ${test.subject} ($grade)")
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTestRecord(testId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("tests").document(testId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Books ---
    suspend fun saveBook(book: BookItem): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = book.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("books").document()
        } else {
            userDoc(uid).collection("books").document(book.id)
        }
        val safeQty = book.quantity.coerceAtLeast(0L)
        val safeDist = book.distributed.coerceIn(0L, safeQty)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "bookName" to book.bookName.trim().take(120),
                    "className" to book.className.trim().take(40),
                    "subject" to book.subject.trim().take(80),
                    "author" to book.author.trim().take(100),
                    "publisher" to book.publisher.trim().take(100),
                    "quantity" to safeQty,
                    "distributed" to safeDist,
                    "purchaseDate" to book.purchaseDate.ifBlank { todayIsoDate() }.take(20),
                    "photoBase64" to book.photoBase64.take(120000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                logActivity("BOOK", "Book Stock Added", "${book.bookName} (Qty: $safeQty)")
            } else {
                val updates = mapOf(
                    "bookName" to book.bookName.trim().take(120),
                    "className" to book.className.trim().take(40),
                    "subject" to book.subject.trim().take(80),
                    "author" to book.author.trim().take(100),
                    "publisher" to book.publisher.trim().take(100),
                    "quantity" to safeQty,
                    "distributed" to safeDist,
                    "purchaseDate" to book.purchaseDate.trim().take(20),
                    "photoBase64" to book.photoBase64.take(120000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                logActivity("BOOK", "Book Stock Updated", "${book.bookName} (Remaining: ${safeQty - safeDist})")
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBook(bookId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("books").document(bookId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Stationery & Toys ---
    suspend fun saveStationery(item: StationeryItem): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = item.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("stationery").document()
        } else {
            userDoc(uid).collection("stationery").document(item.id)
        }
        val safeQty = item.quantity.coerceAtLeast(0L)
        val safeDist = item.distributed.coerceIn(0L, safeQty)
        val historyEntry = "${nowDateTimeLabel()}: Qty=$safeQty, Dist=$safeDist, Avail=${safeQty - safeDist}"
        val updatedHistory = if (item.stockHistorySummary.isBlank()) {
            historyEntry
        } else {
            "$historyEntry\n${item.stockHistorySummary}".take(1000)
        }
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "itemName" to item.itemName.trim().take(120),
                    "category" to item.category.trim().take(60),
                    "details" to item.details.trim().take(300),
                    "quantity" to safeQty,
                    "distributed" to safeDist,
                    "purchasePrice" to item.purchasePrice.coerceAtLeast(0.0),
                    "purchaseDate" to item.purchaseDate.ifBlank { todayIsoDate() }.take(20),
                    "photoBase64" to item.photoBase64.take(120000),
                    "stockHistorySummary" to updatedHistory,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                logActivity("STATIONERY", "Stationery Stock Added", "${item.itemName} (Qty: $safeQty)")
            } else {
                val updates = mapOf(
                    "itemName" to item.itemName.trim().take(120),
                    "category" to item.category.trim().take(60),
                    "details" to item.details.trim().take(300),
                    "quantity" to safeQty,
                    "distributed" to safeDist,
                    "purchasePrice" to item.purchasePrice.coerceAtLeast(0.0),
                    "purchaseDate" to item.purchaseDate.trim().take(20),
                    "photoBase64" to item.photoBase64.take(120000),
                    "stockHistorySummary" to updatedHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                logActivity("STATIONERY", "Stationery Stock Updated", "${item.itemName} (Avail: ${safeQty - safeDist})")
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStationery(itemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("stationery").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Notices ---
    suspend fun saveNotice(notice: NoticeItem): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = notice.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("notices").document()
        } else {
            userDoc(uid).collection("notices").document(notice.id)
        }
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to notice.title.trim().take(120),
                    "message" to notice.message.trim().take(1000),
                    "targetAudience" to notice.targetAudience,
                    "priority" to notice.priority,
                    "dateStr" to notice.dateStr.ifBlank { todayIsoDate() }.take(30),
                    "authorName" to notice.authorName.ifBlank { "Admin" }.take(80),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                logActivity("NOTICE", "Notice Posted", notice.title.take(100))
            } else {
                val updates = mapOf(
                    "title" to notice.title.trim().take(120),
                    "message" to notice.message.trim().take(1000),
                    "targetAudience" to notice.targetAudience,
                    "priority" to notice.priority,
                    "dateStr" to notice.dateStr.trim().take(30),
                    "authorName" to notice.authorName.trim().take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                logActivity("NOTICE", "Notice Updated", notice.title.take(100))
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("notices").document(noticeId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Members (Role-Based Staff & Parent Profiles) ---
    suspend fun saveMember(member: StaffParentMember): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = member.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("members").document()
        } else {
            userDoc(uid).collection("members").document(member.id)
        }
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to member.name.trim().take(100),
                    "role" to member.role,
                    "phone" to member.phone.trim().take(20),
                    "assignedClass" to member.assignedClass.trim().take(40),
                    "linkedStudentId" to member.linkedStudentId.trim().take(128),
                    "accessPin" to member.accessPin.trim().take(10),
                    "active" to member.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to member.name.trim().take(100),
                        "role" to member.role,
                        "phone" to member.phone.trim().take(20),
                        "assignedClass" to member.assignedClass.trim().take(40),
                        "linkedStudentId" to member.linkedStudentId.trim().take(128),
                        "accessPin" to member.accessPin.trim().take(10),
                        "active" to member.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMember(memberId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("members").document(memberId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Backup & Restore (JSON Export / Import preserving existing records) ---
    fun exportAllToJson(
        students: List<Student>,
        attendance: List<AttendanceRecord>,
        tests: List<TestRecord>,
        books: List<BookItem>,
        stationery: List<StationeryItem>,
        notices: List<NoticeItem>,
        members: List<StaffParentMember>
    ): String {
        val root = JSONObject()
        root.put("society", "Parivar Education Society, Chikalpat")
        root.put("exportedAt", nowDateTimeLabel())
        root.put("version", 1)

        root.put("students", JSONArray().apply {
            students.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("fatherName", s.fatherName)
                    put("motherName", s.motherName)
                    put("className", s.className)
                    put("rollNumber", s.rollNumber)
                    put("dob", s.dob)
                    put("gender", s.gender)
                    put("village", s.village)
                    put("address", s.address)
                    put("parentMobile", s.parentMobile)
                    put("admissionDate", s.admissionDate)
                    put("photoBase64", s.photoBase64)
                    put("jnvTargetStream", s.jnvTargetStream)
                })
            }
        })

        root.put("attendance", JSONArray().apply {
            attendance.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("date", a.date)
                    put("className", a.className)
                    put("studentId", a.studentId)
                    put("studentName", a.studentName)
                    put("rollNumber", a.rollNumber)
                    put("status", a.status)
                    put("remarks", a.remarks)
                })
            }
        })

        root.put("tests", JSONArray().apply {
            tests.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("studentId", t.studentId)
                    put("studentName", t.studentName)
                    put("className", t.className)
                    put("subject", t.subject)
                    put("testTitle", t.testTitle)
                    put("testType", t.testType)
                    put("testDate", t.testDate)
                    put("totalMarks", t.totalMarks)
                    put("obtainedMarks", t.obtainedMarks)
                    put("percentage", t.percentage)
                    put("grade", t.grade)
                    put("passed", t.passed)
                    put("remarks", t.remarks)
                })
            }
        })

        root.put("books", JSONArray().apply {
            books.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("bookName", b.bookName)
                    put("className", b.className)
                    put("subject", b.subject)
                    put("author", b.author)
                    put("publisher", b.publisher)
                    put("quantity", b.quantity)
                    put("distributed", b.distributed)
                    put("purchaseDate", b.purchaseDate)
                    put("photoBase64", b.photoBase64)
                })
            }
        })

        root.put("stationery", JSONArray().apply {
            stationery.forEach { st ->
                put(JSONObject().apply {
                    put("id", st.id)
                    put("itemName", st.itemName)
                    put("category", st.category)
                    put("details", st.details)
                    put("quantity", st.quantity)
                    put("distributed", st.distributed)
                    put("purchasePrice", st.purchasePrice)
                    put("purchaseDate", st.purchaseDate)
                    put("photoBase64", st.photoBase64)
                    put("stockHistorySummary", st.stockHistorySummary)
                })
            }
        })

        root.put("notices", JSONArray().apply {
            notices.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("message", n.message)
                    put("targetAudience", n.targetAudience)
                    put("priority", n.priority)
                    put("dateStr", n.dateStr)
                    put("authorName", n.authorName)
                })
            }
        })

        root.put("members", JSONArray().apply {
            members.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("role", m.role)
                    put("phone", m.phone)
                    put("assignedClass", m.assignedClass)
                    put("linkedStudentId", m.linkedStudentId)
                    put("accessPin", m.accessPin)
                    put("active", m.active)
                })
            }
        })

        return root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String): Result<Int> = runCatching {
        val uid = requireUserId()
        val root = JSONObject(jsonString)
        var restoredCount = 0

        suspend fun restoreArray(
            arrayKey: String,
            collectionName: String,
            builder: (JSONObject) -> Map<String, Any>
        ) {
            val arr = root.optJSONArray(arrayKey) ?: return
            val col = userDoc(uid).collection(collectionName)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawId = obj.optString("id", "").replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                val docRef = if (rawId.isNotBlank()) col.document(rawId) else col.document()
                val existingSnap = docRef.get().await()
                val basePayload = builder(obj).toMutableMap()
                if (existingSnap.exists()) {
                    basePayload["updatedAt"] = FieldValue.serverTimestamp()
                    docRef.update(basePayload).await()
                } else {
                    basePayload["userId"] = uid
                    basePayload["createdAt"] = FieldValue.serverTimestamp()
                    basePayload["updatedAt"] = FieldValue.serverTimestamp()
                    docRef.set(basePayload).await()
                }
                restoredCount++
            }
        }

        restoreArray("students", "students") { o ->
            mapOf(
                "name" to o.optString("name", "Student").take(100),
                "fatherName" to o.optString("fatherName", "").take(100),
                "motherName" to o.optString("motherName", "").take(100),
                "className" to o.optString("className", "Class 1"),
                "rollNumber" to o.optString("rollNumber", "1").take(30),
                "dob" to o.optString("dob", "").take(20),
                "gender" to o.optString("gender", "Boy"),
                "village" to o.optString("village", "Chikalpat").take(100),
                "address" to o.optString("address", "").take(300),
                "parentMobile" to o.optString("parentMobile", "").take(20),
                "admissionDate" to o.optString("admissionDate", "").take(20),
                "photoBase64" to o.optString("photoBase64", "").take(120000),
                "jnvTargetStream" to o.optString("jnvTargetStream", "").take(60)
            )
        }

        restoreArray("attendance", "attendance") { o ->
            mapOf(
                "date" to o.optString("date", todayIsoDate()),
                "className" to o.optString("className", "Class 1"),
                "studentId" to o.optString("studentId", ""),
                "studentName" to o.optString("studentName", "Student").take(100),
                "rollNumber" to o.optString("rollNumber", "").take(30),
                "status" to o.optString("status", "PRESENT"),
                "remarks" to o.optString("remarks", "").take(200)
            )
        }

        restoreArray("tests", "tests") { o ->
            val total = o.optDouble("totalMarks", 100.0).coerceAtLeast(1.0)
            val obtained = o.optDouble("obtainedMarks", 0.0).coerceIn(0.0, total)
            val pct = TestRecord.calculatePercentage(obtained, total)
            mapOf(
                "studentId" to o.optString("studentId", ""),
                "studentName" to o.optString("studentName", "Student").take(100),
                "className" to o.optString("className", "Class 1"),
                "subject" to o.optString("subject", "General").take(80),
                "testTitle" to o.optString("testTitle", "Test").take(120),
                "testType" to o.optString("testType", "Regular"),
                "testDate" to o.optString("testDate", todayIsoDate()),
                "totalMarks" to total,
                "obtainedMarks" to obtained,
                "percentage" to pct,
                "grade" to TestRecord.calculateGrade(pct),
                "passed" to TestRecord.isPass(pct),
                "remarks" to o.optString("remarks", "").take(250)
            )
        }

        restoreArray("books", "books") { o ->
            val qty = o.optLong("quantity", 0L).coerceAtLeast(0L)
            val dist = o.optLong("distributed", 0L).coerceIn(0L, qty)
            mapOf(
                "bookName" to o.optString("bookName", "Book").take(120),
                "className" to o.optString("className", "Class 1").take(40),
                "subject" to o.optString("subject", "General").take(80),
                "author" to o.optString("author", "").take(100),
                "publisher" to o.optString("publisher", "").take(100),
                "quantity" to qty,
                "distributed" to dist,
                "purchaseDate" to o.optString("purchaseDate", "").take(20),
                "photoBase64" to o.optString("photoBase64", "").take(120000)
            )
        }

        restoreArray("stationery", "stationery") { o ->
            val qty = o.optLong("quantity", 0L).coerceAtLeast(0L)
            val dist = o.optLong("distributed", 0L).coerceIn(0L, qty)
            mapOf(
                "itemName" to o.optString("itemName", "Item").take(120),
                "category" to o.optString("category", "Notebook").take(60),
                "details" to o.optString("details", "").take(300),
                "quantity" to qty,
                "distributed" to dist,
                "purchasePrice" to o.optDouble("purchasePrice", 0.0).coerceAtLeast(0.0),
                "purchaseDate" to o.optString("purchaseDate", "").take(20),
                "photoBase64" to o.optString("photoBase64", "").take(120000),
                "stockHistorySummary" to o.optString("stockHistorySummary", "").take(1000)
            )
        }

        restoreArray("notices", "notices") { o ->
            mapOf(
                "title" to o.optString("title", "Notice").take(120),
                "message" to o.optString("message", "").take(1000),
                "targetAudience" to o.optString("targetAudience", "All"),
                "priority" to o.optString("priority", "Normal"),
                "dateStr" to o.optString("dateStr", todayIsoDate()).take(30),
                "authorName" to o.optString("authorName", "Admin").take(80)
            )
        }

        restoreArray("members", "members") { o ->
            mapOf(
                "name" to o.optString("name", "Member").take(100),
                "role" to o.optString("role", "Staff"),
                "phone" to o.optString("phone", "").take(20),
                "assignedClass" to o.optString("assignedClass", "").take(40),
                "linkedStudentId" to o.optString("linkedStudentId", "").take(128),
                "accessPin" to o.optString("accessPin", "").take(10),
                "active" to o.optBoolean("active", true)
            )
        }

        logActivity("BACKUP", "Backup Restored", "Restored $restoredCount records safely")
        restoredCount
    }
}
