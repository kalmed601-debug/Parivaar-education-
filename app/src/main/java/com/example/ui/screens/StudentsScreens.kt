package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SchoolConstants
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.DateFieldWithPicker
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudentAvatar
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.ClassCardColors
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MonthlyPurple
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils
import com.example.util.MediaCodecUtils
import java.util.Locale

@Composable
fun StudentManagementScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    selectedClassFilter: String,
    openAddImmediately: Boolean,
    onClassFilterChange: (String) -> Unit,
    onSelectStudent: (Student) -> Unit,
    onSaveStudent: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var genderFilter by remember { mutableStateOf("All") }
    var showFormDialog by remember(openAddImmediately) { mutableStateOf(openAddImmediately) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }

    val canEdit = state.activeRole != "Parent"
    val canDelete = state.activeRole == "Admin"

    val filtered = remember(state.students, selectedClassFilter, genderFilter, searchQuery) {
        state.students.filter { s ->
            val classOk = selectedClassFilter == "All" || s.className == selectedClassFilter
            val genderOk = genderFilter == "All" || s.gender == genderFilter
            val q = searchQuery.trim().lowercase()
            val queryOk = q.isEmpty() ||
                s.name.lowercase().contains(q) ||
                s.rollNumber.lowercase().contains(q) ||
                s.village.lowercase().contains(q) ||
                s.fatherName.lowercase().contains(q)
            classOk && genderOk && queryOk
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        editingStudent = null
                        showFormDialog = true
                    },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_student_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.addStudent)
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
                    Text(
                        text = if (selectedClassFilter == "All") strings.studentManagement else "$selectedClassFilter • Students",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "${filtered.size} Students Listed",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                }
                if (canEdit) {
                    Button(
                        onClick = {
                            editingStudent = null
                            showFormDialog = true
                        },
                        modifier = Modifier.testTag("add_student_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = strings.addStudent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addStudent)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_search_input")
            )

            Spacer(Modifier.height(8.dp))

            // Class chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All").plus(SchoolConstants.ALL_CLASSES).forEach { cls ->
                    FilterChip(
                        selected = selectedClassFilter == cls,
                        onClick = { onClassFilterChange(cls) },
                        label = { Text(cls) }
                    )
                }
            }

            // Gender chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Boy", "Girl", "Other").forEach { g ->
                    FilterChip(
                        selected = genderFilter == g,
                        onClick = { genderFilter = g },
                        label = { Text(if (g == "All") "All Genders" else g) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Groups,
                    title = strings.noDataYet,
                    subtitle = "Tap '${strings.addStudent}' to enroll a student in ${if (selectedClassFilter == "All") "school" else selectedClassFilter}.",
                    actionLabel = if (canEdit) strings.addStudent else null,
                    onAction = if (canEdit) {
                        {
                            editingStudent = null
                            showFormDialog = true
                        }
                    } else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(filtered, key = { it.id }) { student ->
                        val colorIdx = SchoolConstants.ALL_CLASSES.indexOf(student.className).coerceAtLeast(0)
                        val accent = ClassCardColors[colorIdx % ClassCardColors.size]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectStudent(student) }
                                .testTag("student_item_${student.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(
                                    name = student.name,
                                    photoBase64 = student.photoBase64,
                                    accentColor = accent,
                                    size = 52.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${student.className} • Roll #${student.rollNumber} • ${student.gender}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IndigoAccent
                                    )
                                    val ageText = student.calculatedAgeYears()?.let { "Age $it yrs" } ?: ""
                                    Text(
                                        text = listOfNotNull(
                                            student.fatherName.takeIf { it.isNotBlank() }?.let { "S/o D/o $it" },
                                            student.village.takeIf { it.isNotBlank() },
                                            ageText.takeIf { it.isNotBlank() }
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (canEdit) {
                                    IconButton(onClick = {
                                        editingStudent = student
                                        showFormDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Student", tint = IndigoAccent)
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteStudent(student) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = AbsentCrimson)
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
        StudentFormDialog(
            initial = editingStudent,
            defaultClass = if (selectedClassFilter == "All") "Class 1" else selectedClassFilter,
            onDismiss = { showFormDialog = false },
            onSave = {
                onSaveStudent(it)
                showFormDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    initial: Student?,
    defaultClass: String,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var fatherName by remember { mutableStateOf(initial?.fatherName ?: "") }
    var motherName by remember { mutableStateOf(initial?.motherName ?: "") }
    var className by remember { mutableStateOf(initial?.className ?: defaultClass) }
    var rollNumber by remember { mutableStateOf(initial?.rollNumber ?: "") }
    var dob by remember { mutableStateOf(initial?.dob ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: "Boy") }
    var village by remember { mutableStateOf(initial?.village ?: "Chikalpat") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var parentMobile by remember { mutableStateOf(initial?.parentMobile ?: "") }
    var admissionDate by remember { mutableStateOf(initial?.admissionDate?.ifBlank { DateUtils.todayIso() } ?: DateUtils.todayIso()) }
    var photoBase64 by remember { mutableStateOf(initial?.photoBase64 ?: "") }
    var classExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            MediaCodecUtils.uriToCompressedBase64(context, uri)?.let { photoBase64 = it }
        }
    }

    val liveAge = remember(dob) {
        Student(dob = dob).calculatedAgeYears()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add New Student" else "Edit Student") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudentAvatar(name = name.ifBlank { "S" }, photoBase64 = photoBase64, size = 58.dp)
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Pick Student Photo", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoBase64.isBlank()) "Upload Photo" else "Change Photo")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_name_input")
                )
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text("Mother's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = classExpanded,
                    onExpandedChange = { classExpanded = !classExpanded }
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = classExpanded,
                        onDismissRequest = { classExpanded = false }
                    ) {
                        SchoolConstants.ALL_CLASSES.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    className = cls
                                    classExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it },
                    label = { Text("Roll Number*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_roll_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SchoolConstants.GENDERS.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = g },
                            label = { Text(g) }
                        )
                    }
                }

                DateFieldWithPicker(
                    label = "Date of Birth (YYYY-MM-DD)",
                    value = dob,
                    onDateSelected = { dob = it },
                    modifier = Modifier.fillMaxWidth()
                )
                if (liveAge != null) {
                    Text(
                        text = "Calculated Age: $liveAge years",
                        style = MaterialTheme.typography.labelMedium,
                        color = WeeklyGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = parentMobile,
                    onValueChange = { parentMobile = it },
                    label = { Text("Parent Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DateFieldWithPicker(
                    label = "Admission Date",
                    value = admissionDate,
                    onDateSelected = { admissionDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (initial ?: Student()).copy(
                            name = name,
                            fatherName = fatherName,
                            motherName = motherName,
                            className = className,
                            rollNumber = rollNumber,
                            dob = dob,
                            gender = gender,
                            village = village,
                            address = address,
                            parentMobile = parentMobile,
                            admissionDate = admissionDate,
                            photoBase64 = photoBase64
                        )
                    )
                },
                enabled = name.isNotBlank() && rollNumber.isNotBlank(),
                modifier = Modifier.testTag("save_student_confirm_button")
            ) {
                Text("Save Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StudentProfileScreen(
    student: Student,
    state: SchoolUiState,
    onEditStudent: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onSaveTest: (TestRecord) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    val canEdit = state.activeRole != "Parent"
    val canDelete = state.activeRole == "Admin"

    val studentAttendance = remember(state.attendance, student.id) {
        state.attendance.filter { it.studentId == student.id }.sortedByDescending { it.date }
    }
    val studentTests = remember(state.tests, student.id) {
        state.tests.filter { it.studentId == student.id }.sortedByDescending { it.testDate }
    }

    val tabs = listOf(
        "Personal Information",
        "Daily Attendance",
        "Test & Examination",
        "Weekly Report",
        "Monthly Report",
        "Academic Performance"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Profile Banner Header
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    StudentAvatar(
                        name = student.name,
                        photoBase64 = student.photoBase64,
                        accentColor = NavyPrimary,
                        size = 64.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Text(
                            text = "${student.className} • Roll #${student.rollNumber} • ${student.gender}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoAccent
                        )
                        val age = student.calculatedAgeYears()
                        if (age != null) {
                            Text("Age: $age yrs • DOB: ${student.dob}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                    }
                    if (canEdit) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = IndigoAccent)
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = {
                            onDeleteStudent(student)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = AbsentCrimson)
                        }
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProfileDetailRow("Student Name", student.name)
                                ProfileDetailRow("Father's Name", student.fatherName.ifBlank { "-" })
                                ProfileDetailRow("Mother's Name", student.motherName.ifBlank { "-" })
                                ProfileDetailRow("Class", student.className)
                                ProfileDetailRow("Roll Number", student.rollNumber)
                                ProfileDetailRow("Date of Birth", student.dob.ifBlank { "-" })
                                ProfileDetailRow("Calculated Age", student.calculatedAgeYears()?.let { "$it Years" } ?: "-")
                                ProfileDetailRow("Gender", student.gender)
                                ProfileDetailRow("Village", student.village.ifBlank { "-" })
                                ProfileDetailRow("Full Address", student.address.ifBlank { "-" })
                                ProfileDetailRow("Parent Mobile", student.parentMobile.ifBlank { "-" })
                                ProfileDetailRow("Admission Date", student.admissionDate.ifBlank { "-" })
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        val present = studentAttendance.count { it.status == "PRESENT" }
                        val absent = studentAttendance.count { it.status == "ABSENT" }
                        val holiday = studentAttendance.count { it.status == "HOLIDAY" }
                        val pct = if (present + absent > 0) (present * 100f / (present + absent)) else 0f
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                ProfileStatCol("Present", present.toString(), WeeklyGreen)
                                ProfileStatCol("Absent", absent.toString(), AbsentCrimson)
                                ProfileStatCol("Holiday", holiday.toString(), Color(0xFFD97706))
                                ProfileStatCol("Rate", "${String.format(Locale.US, "%.1f", pct)}%", NavyPrimary)
                            }
                        }
                    }
                    if (studentAttendance.isEmpty()) {
                        item {
                            Text("No attendance records saved yet for ${student.name}.", color = Color(0xFF64748B))
                        }
                    } else {
                        items(studentAttendance, key = { it.id }) { rec ->
                            val badgeColor = when (rec.status) {
                                "PRESENT" -> WeeklyGreen
                                "ABSENT" -> AbsentCrimson
                                else -> Color(0xFFD97706)
                            }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rec.date, fontWeight = FontWeight.SemiBold)
                                    Surface(shape = RoundedCornerShape(8.dp), color = badgeColor.copy(alpha = 0.14f)) {
                                        Text(rec.status, color = badgeColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (studentTests.isEmpty()) {
                        item { Text("No test records yet for ${student.name}.", color = Color(0xFF64748B)) }
                    } else {
                        items(studentTests, key = { it.id }) { t ->
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
                                        Text("${t.subject} • ${t.testTitle}", fontWeight = FontWeight.Bold)
                                        Text("${t.testDate} • ${t.testType}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${t.obtainedMarks.toInt()}/${t.totalMarks.toInt()} (${t.percentage.toInt()}%)", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        Text(
                                            text = "Grade ${t.grade} • ${if (t.passed) "PASS" else "FAIL"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (t.passed) WeeklyGreen else AbsentCrimson,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    val (wStart, wEnd) = DateUtils.currentWeekRange()
                    val wAtt = studentAttendance.filter { DateUtils.isDateInRange(it.date, wStart, wEnd) }
                    val wTests = studentTests.filter { DateUtils.isDateInRange(it.testDate, wStart, wEnd) }
                    val wPres = wAtt.count { it.status == "PRESENT" }
                    val wAbs = wAtt.count { it.status == "ABSENT" }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7EE)),
                            border = BorderStroke(1.dp, WeeklyGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Weekly Report ($wStart to $wEnd)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WeeklyGreen)
                                ProfileDetailRow("Days Present", wPres.toString())
                                ProfileDetailRow("Days Absent", wAbs.toString())
                                ProfileDetailRow("Tests This Week", wTests.size.toString())
                                val avg = if (wTests.isNotEmpty()) wTests.map { it.percentage }.average() else 0.0
                                ProfileDetailRow("Average Score", "${String.format(Locale.US, "%.1f", avg)}%")
                            }
                        }
                    }
                }

                4 -> {
                    val m = DateUtils.currentMonth()
                    val y = DateUtils.currentYear()
                    val mAtt = studentAttendance.filter { DateUtils.isDateInMonth(it.date, m, y) }
                    val mTests = studentTests.filter { DateUtils.isDateInMonth(it.testDate, m, y) }
                    val mPres = mAtt.count { it.status == "PRESENT" }
                    val mAbs = mAtt.count { it.status == "ABSENT" }
                    val pct = if (mPres + mAbs > 0) (mPres * 100.0 / (mPres + mAbs)) else 0.0
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EBFE)),
                            border = BorderStroke(1.dp, MonthlyPurple.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Monthly Report (${DateUtils.monthName(m)} $y)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MonthlyPurple)
                                ProfileDetailRow("Present Days", mPres.toString())
                                ProfileDetailRow("Absent Days", mAbs.toString())
                                ProfileDetailRow("Monthly Attendance %", "${String.format(Locale.US, "%.1f", pct)}%")
                                ProfileDetailRow("Monthly Tests", mTests.size.toString())
                                val avg = if (mTests.isNotEmpty()) mTests.map { it.percentage }.average() else 0.0
                                ProfileDetailRow("Monthly Avg Score", "${String.format(Locale.US, "%.1f", avg)}%")
                            }
                        }
                    }
                }

                5 -> {
                    item {
                        val subjectGroups = studentTests.groupBy { it.subject }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Academic Performance by Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Spacer(Modifier.height(10.dp))
                                if (subjectGroups.isEmpty()) {
                                    Text("No exam results recorded yet to plot performance.", color = Color(0xFF64748B))
                                } else {
                                    val chartEntries = subjectGroups.map { (subj, list) ->
                                        subj.take(8) to list.map { it.percentage }.average().toFloat()
                                    }
                                    BarChartCanvas(entries = chartEntries, barColors = ClassCardColors, maxVal = 100f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        StudentFormDialog(
            initial = student,
            defaultClass = student.className,
            onDismiss = { showEditDialog = false },
            onSave = {
                onEditStudent(it)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
    }
}

@Composable
private fun ProfileStatCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
    }
}
