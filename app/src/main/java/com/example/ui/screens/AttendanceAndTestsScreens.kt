package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.SchoolConstants
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.DateFieldWithPicker
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudentAvatar
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ClassCardColors
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun AttendanceScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    initialClass: String = "Class 1",
    onSaveAttendance: (date: String, className: String, records: List<AttendanceRecord>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedClass by remember { mutableStateOf(initialClass) }
    var selectedDate by remember { mutableStateOf(DateUtils.todayIso()) }

    val canEdit = state.activeRole != "Parent"

    val classStudents = remember(state.students, selectedClass) {
        state.students.filter { it.className == selectedClass }
    }

    val statusMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(selectedClass, selectedDate, state.attendance, classStudents) {
        statusMap.clear()
        val savedForDay = state.attendance.filter { it.date == selectedDate && it.className == selectedClass }
            .associateBy { it.studentId }
        classStudents.forEach { stu ->
            statusMap[stu.id] = savedForDay[stu.id]?.status ?: "PRESENT"
        }
    }

    val curMonth = DateUtils.currentMonth()
    val curYear = DateUtils.currentYear()
    val monthlyClassAtt = remember(state.attendance, selectedClass, curMonth, curYear) {
        state.attendance.filter { it.className == selectedClass && DateUtils.isDateInMonth(it.date, curMonth, curYear) }
    }
    val mPresent = monthlyClassAtt.count { it.status == "PRESENT" }
    val mAbsent = monthlyClassAtt.count { it.status == "ABSENT" }
    val monthlyPct = if (mPresent + mAbsent > 0) (mPresent * 100f / (mPresent + mAbsent)) else 0f

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.attendance, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text(
                    text = "$selectedClass • Monthly Att: ${String.format(Locale.US, "%.1f", monthlyPct)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = WeeklyGreen
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Mark / Edit Attendance") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Date-wise History") })
        }

        Spacer(Modifier.height(10.dp))

        // Class selector chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SchoolConstants.ALL_CLASSES.forEach { cls ->
                FilterChip(
                    selected = selectedClass == cls,
                    onClick = { selectedClass = cls },
                    label = { Text(cls) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedTab == 0) {
            DateFieldWithPicker(
                label = "Attendance Date",
                value = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            if (canEdit && classStudents.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { classStudents.forEach { statusMap[it.id] = "PRESENT" } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WeeklyGreen),
                        modifier = Modifier.testTag("mark_all_present_button")
                    ) {
                        Text(strings.markAllPresent)
                    }
                    OutlinedButton(
                        onClick = { classStudents.forEach { statusMap[it.id] = "ABSENT" } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AbsentCrimson),
                        modifier = Modifier.testTag("mark_all_absent_button")
                    ) {
                        Text(strings.markAllAbsent)
                    }
                    OutlinedButton(
                        onClick = { classStudents.forEach { statusMap[it.id] = "HOLIDAY" } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        modifier = Modifier.testTag("mark_holiday_button")
                    ) {
                        Text(strings.markHoliday)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (classStudents.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Checklist,
                    title = "No Students in $selectedClass",
                    subtitle = "Add students to $selectedClass first to mark daily attendance."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classStudents, key = { it.id }) { stu ->
                        val st = statusMap[stu.id] ?: "PRESENT"
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(name = stu.name, photoBase64 = stu.photoBase64, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stu.name, fontWeight = FontWeight.SemiBold)
                                    Text("Roll #${stu.rollNumber}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    StatusToggleChip("P", st == "PRESENT", WeeklyGreen, enabled = canEdit) {
                                        statusMap[stu.id] = "PRESENT"
                                    }
                                    StatusToggleChip("A", st == "ABSENT", AbsentCrimson, enabled = canEdit) {
                                        statusMap[stu.id] = "ABSENT"
                                    }
                                    StatusToggleChip("H", st == "HOLIDAY", Color(0xFFD97706), enabled = canEdit) {
                                        statusMap[stu.id] = "HOLIDAY"
                                    }
                                }
                            }
                        }
                    }
                }

                if (canEdit) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val entries = classStudents.map { stu ->
                                AttendanceRecord(
                                    date = selectedDate,
                                    className = selectedClass,
                                    studentId = stu.id,
                                    studentName = stu.name,
                                    rollNumber = stu.rollNumber,
                                    status = statusMap[stu.id] ?: "PRESENT"
                                )
                            }
                            onSaveAttendance(selectedDate, selectedClass, entries)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WeeklyGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_attendance_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = strings.saveAttendance)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.saveAttendance, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val groupedByDate = remember(state.attendance, selectedClass) {
                state.attendance.filter { it.className == selectedClass }
                    .groupBy { it.date }
                    .toSortedMap(compareByDescending { it })
            }
            if (groupedByDate.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Checklist,
                    title = "No Attendance History",
                    subtitle = "Saved attendance dates for $selectedClass will appear here."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(groupedByDate.keys.toList(), key = { it }) { dateKey ->
                        val dayRecs = groupedByDate[dateKey].orEmpty()
                        val p = dayRecs.count { it.status == "PRESENT" }
                        val a = dayRecs.count { it.status == "ABSENT" }
                        val h = dayRecs.count { it.status == "HOLIDAY" }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder),
                            onClick = {
                                selectedDate = dateKey
                                selectedTab = 0
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dateKey, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    Text("Tap to view or edit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (h == dayRecs.size && h > 0) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = AmberGold.copy(alpha = 0.2f)) {
                                            Text("HOLIDAY", color = Color(0xFF92400E), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    } else {
                                        Text("P: $p", color = WeeklyGreen, fontWeight = FontWeight.Bold)
                                        Text("A: $a", color = AbsentCrimson, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusToggleChip(
    label: String,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color else Color(0xFFF1F5F9),
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(width = 38.dp, height = 36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Color(0xFF475569),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun TestExamScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    initialClass: String = "All",
    onSaveTest: (TestRecord) -> Unit,
    onDeleteTest: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var classFilter by remember { mutableStateOf(initialClass) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingTest by remember { mutableStateOf<TestRecord?>(null) }

    val canEdit = state.activeRole != "Parent"
    val canDelete = state.activeRole == "Admin"

    val filteredTests = remember(state.tests, classFilter) {
        if (classFilter == "All") state.tests else state.tests.filter { it.className == classFilter }
    }

    Scaffold(
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        editingTest = null
                        showFormDialog = true
                    },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_test_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Test")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.testAndExam, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text("${filteredTests.size} Results Saved", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                }
                if (canEdit) {
                    Button(
                        onClick = {
                            editingTest = null
                            showFormDialog = true
                        },
                        modifier = Modifier.testTag("add_test_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Test", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Test")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All").plus(SchoolConstants.ALL_CLASSES).forEach { cls ->
                    FilterChip(
                        selected = classFilter == cls,
                        onClick = { classFilter = cls },
                        label = { Text(cls) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (filteredTests.isNotEmpty()) {
                val subjectAvg = filteredTests.groupBy { it.subject }.map { (subj, list) ->
                    subj.take(7) to list.map { it.percentage }.average().toFloat()
                }.take(8)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Subject-wise Average (%)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Spacer(Modifier.height(6.dp))
                        BarChartCanvas(
                            entries = subjectAvg,
                            barColors = ClassCardColors,
                            maxVal = 100f,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (filteredTests.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Quiz,
                    title = "No Test Results Yet",
                    subtitle = "Tap 'Add Test' to record exam scores, auto-calculate percentage, grade, and pass/fail status.",
                    actionLabel = if (canEdit) "Add New Test" else null,
                    onAction = if (canEdit) {
                        {
                            editingTest = null
                            showFormDialog = true
                        }
                    } else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(filteredTests, key = { it.id }) { test ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${test.studentName} (${test.className})", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    Text("${test.subject} • ${test.testTitle}", style = MaterialTheme.typography.bodyMedium)
                                    Text("${test.testDate} • ${test.testType}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${test.obtainedMarks.toInt()}/${test.totalMarks.toInt()} (${String.format(Locale.US, "%.1f", test.percentage)}%)",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Grade ${test.grade} • ${if (test.passed) "PASS" else "FAIL"}",
                                        color = if (test.passed) WeeklyGreen else AbsentCrimson,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (canEdit) {
                                    IconButton(onClick = {
                                        editingTest = test
                                        showFormDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Test", tint = IndigoAccent)
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteTest(test.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Test", tint = AbsentCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        TestFormDialog(
            initial = editingTest,
            students = state.students,
            defaultClass = if (classFilter == "All") "Class 1" else classFilter,
            onDismiss = { showFormDialog = false },
            onSave = {
                onSaveTest(it)
                showFormDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestFormDialog(
    initial: TestRecord?,
    students: List<Student>,
    defaultClass: String,
    onDismiss: () -> Unit,
    onSave: (TestRecord) -> Unit
) {
    var selectedClass by remember { mutableStateOf(initial?.className ?: defaultClass) }
    val classStudents = remember(students, selectedClass) { students.filter { it.className == selectedClass } }
    var selectedStudentId by remember { mutableStateOf(initial?.studentId ?: classStudents.firstOrNull()?.id.orEmpty()) }
    var selectedStudentName by remember { mutableStateOf(initial?.studentName ?: classStudents.firstOrNull()?.name.orEmpty()) }
    var subject by remember { mutableStateOf(initial?.subject ?: if (selectedClass == "JNV") "Arithmetic" else "Mathematics") }
    var testTitle by remember { mutableStateOf(initial?.testTitle ?: "Unit Test") }
    var testType by remember { mutableStateOf(initial?.testType ?: if (selectedClass == "JNV") "JNV Mock Test" else "Regular") }
    var testDate by remember { mutableStateOf(initial?.testDate ?: DateUtils.todayIso()) }
    var totalMarksStr by remember { mutableStateOf(initial?.totalMarks?.toInt()?.toString() ?: "100") }
    var obtainedMarksStr by remember { mutableStateOf(initial?.obtainedMarks?.toInt()?.toString() ?: "") }
    var remarks by remember { mutableStateOf(initial?.remarks ?: "") }

    var classMenu by remember { mutableStateOf(false) }
    var studentMenu by remember { mutableStateOf(false) }
    var subjectMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }

    val total = totalMarksStr.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
    val obtained = obtainedMarksStr.toDoubleOrNull()?.coerceIn(0.0, total) ?: 0.0
    val pct = TestRecord.calculatePercentage(obtained, total)
    val grade = TestRecord.calculateGrade(pct)
    val passed = TestRecord.isPass(pct)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Test Result" else "Edit Test Result") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(expanded = classMenu, onExpandedChange = { classMenu = !classMenu }) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Class*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                        SchoolConstants.ALL_CLASSES.forEach { cls ->
                            DropdownMenuItem(text = { Text(cls) }, onClick = {
                                selectedClass = cls
                                val firstStu = students.firstOrNull { it.className == cls }
                                selectedStudentId = firstStu?.id.orEmpty()
                                selectedStudentName = firstStu?.name.orEmpty()
                                classMenu = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = studentMenu, onExpandedChange = { studentMenu = !studentMenu }) {
                    OutlinedTextField(
                        value = selectedStudentName.ifBlank { "No student in $selectedClass" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Student*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = studentMenu, onDismissRequest = { studentMenu = false }) {
                        classStudents.forEach { stu ->
                            DropdownMenuItem(text = { Text("${stu.name} (Roll #${stu.rollNumber})") }, onClick = {
                                selectedStudentId = stu.id
                                selectedStudentName = stu.name
                                studentMenu = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = subjectMenu, onExpandedChange = { subjectMenu = !subjectMenu }) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = subjectMenu, onDismissRequest = { subjectMenu = false }) {
                        (if (selectedClass == "JNV") SchoolConstants.JNV_SUBJECTS else SchoolConstants.SCHOOL_SUBJECTS).forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = {
                                subject = s
                                subjectMenu = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = testTitle,
                    onValueChange = { testTitle = it },
                    label = { Text("Test Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = !typeMenu }) {
                    OutlinedTextField(
                        value = testType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Test Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        SchoolConstants.TEST_TYPES.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                testType = t
                                typeMenu = false
                            })
                        }
                    }
                }

                DateFieldWithPicker(
                    label = "Test Date",
                    value = testDate,
                    onDateSelected = { testDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalMarksStr,
                        onValueChange = { totalMarksStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Total Marks*") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = obtainedMarksStr,
                        onValueChange = { obtainedMarksStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Obtained*") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (passed) WeeklyGreen else AbsentCrimson).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Percentage: ${String.format(Locale.US, "%.1f", pct)}%", fontWeight = FontWeight.Bold)
                        Text("Grade: $grade", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (passed) "PASS" else "FAIL",
                            fontWeight = FontWeight.Bold,
                            color = if (passed) WeeklyGreen else AbsentCrimson
                        )
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (initial ?: TestRecord()).copy(
                            studentId = selectedStudentId,
                            studentName = selectedStudentName,
                            className = selectedClass,
                            subject = subject,
                            testTitle = testTitle,
                            testType = testType,
                            testDate = testDate,
                            totalMarks = total,
                            obtainedMarks = obtained,
                            percentage = pct,
                            grade = grade,
                            passed = passed,
                            remarks = remarks
                        )
                    )
                },
                enabled = selectedStudentId.isNotBlank() && subject.isNotBlank() && testTitle.isNotBlank() && obtainedMarksStr.isNotBlank()
            ) {
                Text("Save Result")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
