package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SchoolConstants
import com.example.data.model.Student
import com.example.data.model.TestRecord
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ClassCardColors
import com.example.ui.theme.MonthlyPurple
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun JnvDashboardScreen(
    state: SchoolUiState,
    onSelectStudent: (Student) -> Unit,
    onSaveStudent: (Student) -> Unit,
    onSaveTest: (TestRecord) -> Unit,
    onOpenJnvAttendance: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStudent by remember { mutableStateOf(false) }
    var showAddTest by remember { mutableStateOf(false) }

    val canEdit = state.activeRole != "Parent"

    val jnvStudents = remember(state.students) { state.students.filter { it.className == "JNV" } }
    val jnvTests = remember(state.tests) { state.tests.filter { it.className == "JNV" } }
    val jnvAtt = remember(state.attendance) { state.attendance.filter { it.className == "JNV" } }
    val today = remember { DateUtils.todayIso() }
    val todayJnvPresent = jnvAtt.count { it.date == today && it.status == "PRESENT" }
    val jnvPassCount = jnvTests.count { it.passed }
    val jnvAvgPct = if (jnvTests.isNotEmpty()) jnvTests.map { it.percentage }.average().toFloat() else 0f

    val tabs = listOf(
        "JNV Students (${jnvStudents.size})",
        "Entrance Prep & Subjects",
        "Mock Tests & History (${jnvTests.size})",
        "JNV Weekly & Monthly"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // JNV Dedicated Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFB45309), AmberGold)))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Icon(Icons.Default.EmojiEvents, contentDescription = "JNV Emblem", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("JNV Management System", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Jawahar Navodaya Vidyalaya Entrance Wing", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFEF3C7))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JnvStatChip("Students", jnvStudents.size.toString(), Modifier.weight(1f))
                    JnvStatChip("Today Present", todayJnvPresent.toString(), Modifier.weight(1f))
                    JnvStatChip("Mock Tests", jnvTests.size.toString(), Modifier.weight(1f))
                    JnvStatChip("Avg Score", "${jnvAvgPct.toInt()}%", Modifier.weight(1f))
                }
                if (canEdit) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAddStudent = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF92400E)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add JNV Student", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Student")
                        }
                        Button(
                            onClick = { showAddTest = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF92400E)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = "Add Mock Test", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Mock Test")
                        }
                        OutlinedButton(
                            onClick = onOpenJnvAttendance,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Icon(Icons.Default.Checklist, contentDescription = "JNV Attendance", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.White, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, label ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    if (jnvStudents.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.EmojiEvents,
                                title = "No JNV Students Yet",
                                subtitle = "Enroll JNV aspirants to track attendance, subject tests, and mock test rankings.",
                                actionLabel = if (canEdit) "Enroll JNV Student" else null,
                                onAction = if (canEdit) { { showAddStudent = true } } else null
                            )
                        }
                    } else {
                        items(jnvStudents, key = { it.id }) { stu ->
                            val stuTests = jnvTests.filter { it.studentId == stu.id }
                            val stuAvg = if (stuTests.isNotEmpty()) stuTests.map { it.percentage }.average() else 0.0
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.fillMaxWidth().clickable { onSelectStudent(stu) }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StudentAvatar(name = stu.name, photoBase64 = stu.photoBase64, accentColor = Color(0xFFD97706), size = 50.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stu.name, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        Text("Roll #${stu.rollNumber} • ${stu.village}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${stuTests.size} Tests", style = MaterialTheme.typography.labelSmall)
                                        Text("Avg ${String.format(Locale.US, "%.1f", stuAvg)}%", fontWeight = FontWeight.Bold, color = WeeklyGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("JNV Entrance Exam Preparation Sections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                SchoolConstants.JNV_SUBJECTS.forEach { subj ->
                                    val subjTests = jnvTests.filter { it.subject == subj }
                                    val avg = if (subjTests.isNotEmpty()) subjTests.map { it.percentage }.average().toFloat() else 0f
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(subj, fontWeight = FontWeight.SemiBold)
                                            Text("${subjTests.size} tests • ${avg.toInt()}%", color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (avg / 100f).coerceIn(0f, 1f) },
                                            color = AmberGold,
                                            trackColor = Color(0xFFFEF3C7),
                                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (jnvTests.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Quiz,
                                title = "No JNV Mock Tests Recorded",
                                subtitle = "Record JNV Subject Tests and Full Mock Exams to track student pass/fail readiness."
                            )
                        }
                    } else {
                        items(jnvTests, key = { it.id }) { t ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(t.studentName, fontWeight = FontWeight.Bold)
                                        Text("${t.subject} • ${t.testTitle}", style = MaterialTheme.typography.bodyMedium)
                                        Text("${t.testDate} • ${t.testType}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${t.obtainedMarks.toInt()}/${t.totalMarks.toInt()} (${t.percentage.toInt()}%)", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Grade ${t.grade} • ${if (t.passed) "PASS" else "FAIL"}",
                                            color = if (t.passed) WeeklyGreen else AbsentCrimson,
                                            style = MaterialTheme.typography.labelSmall,
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
                    val wAtt = jnvAtt.filter { DateUtils.isDateInRange(it.date, wStart, wEnd) }
                    val wTests = jnvTests.filter { DateUtils.isDateInRange(it.testDate, wStart, wEnd) }
                    val m = DateUtils.currentMonth()
                    val y = DateUtils.currentYear()
                    val mAtt = jnvAtt.filter { DateUtils.isDateInMonth(it.date, m, y) }
                    val mTests = jnvTests.filter { DateUtils.isDateInMonth(it.testDate, m, y) }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7EE)),
                            border = BorderStroke(1.dp, WeeklyGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("JNV Weekly Performance ($wStart to $wEnd)", fontWeight = FontWeight.Bold, color = WeeklyGreen)
                                Text("Present: ${wAtt.count { it.status == "PRESENT" }} | Absent: ${wAtt.count { it.status == "ABSENT" }}")
                                Text("Tests Conducted: ${wTests.size} | Pass Count: ${wTests.count { it.passed }}")
                            }
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EBFE)),
                            border = BorderStroke(1.dp, MonthlyPurple.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("JNV Monthly Performance (${DateUtils.monthName(m)} $y)", fontWeight = FontWeight.Bold, color = MonthlyPurple)
                                Text("Present: ${mAtt.count { it.status == "PRESENT" }} | Absent: ${mAtt.count { it.status == "ABSENT" }}")
                                Text("Monthly Tests: ${mTests.size} | Pass Rate: ${if (mTests.isNotEmpty()) "${(mTests.count { it.passed } * 100 / mTests.size)}%" else "0%"}")
                            }
                        }
                    }
                    if (jnvTests.isNotEmpty()) {
                        item {
                            val entries = SchoolConstants.JNV_SUBJECTS.map { s ->
                                val list = jnvTests.filter { it.subject == s }
                                s.take(8) to (if (list.isNotEmpty()) list.map { it.percentage }.average().toFloat() else 0f)
                            }
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("JNV Subject Comparison", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    Spacer(Modifier.height(8.dp))
                                    BarChartCanvas(entries = entries, barColors = ClassCardColors, maxVal = 100f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStudent) {
        StudentFormDialog(
            initial = null,
            defaultClass = "JNV",
            onDismiss = { showAddStudent = false },
            onSave = {
                onSaveStudent(it.copy(className = "JNV"))
                showAddStudent = false
            }
        )
    }

    if (showAddTest) {
        TestFormDialog(
            initial = null,
            students = state.students,
            defaultClass = "JNV",
            onDismiss = { showAddTest = false },
            onSave = {
                onSaveTest(it)
                showAddTest = false
            }
        )
    }
}

@Composable
private fun JnvStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.2f), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFEF3C7), maxLines = 1)
        }
    }
}
