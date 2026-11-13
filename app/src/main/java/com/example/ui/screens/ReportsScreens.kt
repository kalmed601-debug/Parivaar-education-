package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SchoolConstants
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.LineProgressCanvas
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.ClassCardColors
import com.example.ui.theme.MonthlyPurple
import com.example.ui.theme.MonthlyPurpleDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.theme.WeeklyGreenDark
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils
import com.example.util.ReportExportUtils
import java.util.Locale

@Composable
fun ReportsOverviewScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onOpenWeekly: () -> Unit,
    onOpenMonthly: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(strings.overallStatistics, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenWeekly,
                    colors = ButtonDefaults.buttonColors(containerColor = WeeklyGreen),
                    modifier = Modifier.weight(1f)
                ) { Text(strings.weeklyReport) }
                Button(
                    onClick = onOpenMonthly,
                    colors = ButtonDefaults.buttonColors(containerColor = MonthlyPurple),
                    modifier = Modifier.weight(1f)
                ) { Text(strings.monthlyReport) }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gender & Exam Summary", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile("Boys", state.summary.totalBoys.toString(), Color(0xFF0284C7))
                        StatTile("Girls", state.summary.totalGirls.toString(), Color(0xFFEC4899))
                        StatTile("Pass", state.summary.totalPassTests.toString(), WeeklyGreen)
                        StatTile("Fail", state.summary.totalFailTests.toString(), AbsentCrimson)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Class-wise Student Count", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(Modifier.height(8.dp))
                    val classData = SchoolConstants.ALL_CLASSES.map { cls ->
                        (if (cls == "Anganwadi") "Ang" else cls.replace("Class ", "C")) to (state.summary.classStudentCounts[cls] ?: 0).toFloat()
                    }
                    BarChartCanvas(entries = classData, barColors = ClassCardColors)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Attendance Graph (% by Class)", fontWeight = FontWeight.Bold, color = WeeklyGreenDark)
                    Spacer(Modifier.height(8.dp))
                    val attData = SchoolConstants.ALL_CLASSES.map { cls ->
                        val recs = state.attendance.filter { it.className == cls }
                        val p = recs.count { it.status == "PRESENT" }
                        val a = recs.count { it.status == "ABSENT" }
                        val pct = if (p + a > 0) (p * 100f / (p + a)) else 0f
                        (if (cls == "Anganwadi") "Ang" else cls.replace("Class ", "C")) to pct
                    }
                    BarChartCanvas(entries = attData, barColors = listOf(WeeklyGreen), maxVal = 100f)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Examination Performance (% by Class)", fontWeight = FontWeight.Bold, color = MonthlyPurpleDark)
                    Spacer(Modifier.height(8.dp))
                    val examData = SchoolConstants.ALL_CLASSES.map { cls ->
                        val tList = state.tests.filter { it.className == cls }
                        val avg = if (tList.isNotEmpty()) tList.map { it.percentage }.average().toFloat() else 0f
                        (if (cls == "Anganwadi") "Ang" else cls.replace("Class ", "C")) to avg
                    }
                    BarChartCanvas(entries = examData, barColors = listOf(MonthlyPurple), maxVal = 100f)
                }
            }
        }

        item {
            val curYear = DateUtils.currentYear()
            val curMonth = DateUtils.currentMonth()
            val monthPoints = (maxOf(1, curMonth - 5)..curMonth).map { m ->
                val list = state.tests.filter { DateUtils.isDateInMonth(it.testDate, m, curYear) }
                val avg = if (list.isNotEmpty()) list.map { it.percentage }.average().toFloat() else 0f
                DateUtils.monthName(m).take(3) to avg
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Academic Progress ($curYear)", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(Modifier.height(8.dp))
                    LineProgressCanvas(points = monthPoints, lineColor = MonthlyPurple)
                }
            }
        }
    }
}

@Composable
fun WeeklyReportScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var weekRange by remember { mutableStateOf(DateUtils.currentWeekRange()) }
    val (weekStart, weekEnd) = weekRange

    val weekAtt = remember(state.attendance, weekStart, weekEnd) {
        state.attendance.filter { DateUtils.isDateInRange(it.date, weekStart, weekEnd) }
    }
    val weekTests = remember(state.tests, weekStart, weekEnd) {
        state.tests.filter { DateUtils.isDateInRange(it.testDate, weekStart, weekEnd) }
    }

    val classRows = remember(state.students, weekAtt, weekTests) {
        SchoolConstants.ALL_CLASSES.map { cls ->
            val totalStudents = state.students.count { it.className == cls }
            val cAtt = weekAtt.filter { it.className == cls }
            val p = cAtt.count { it.status == "PRESENT" }
            val a = cAtt.count { it.status == "ABSENT" }
            val attPct = if (p + a > 0) (p * 100f / (p + a)) else 0f
            val cTests = weekTests.filter { it.className == cls }
            val totMarks = cTests.sumOf { it.totalMarks }
            val obtMarks = cTests.sumOf { it.obtainedMarks }
            val perfPct = if (totMarks > 0) (obtMarks * 100.0 / totMarks).toFloat() else 0f
            WeeklyClassRow(cls, totalStudents, p, a, attPct, cTests.size, totMarks, obtMarks, perfPct)
        }
    }

    val totalP = classRows.sumOf { it.present }
    val totalA = classRows.sumOf { it.absent }
    val overallAttPct = if (totalP + totalA > 0) (totalP * 100f / (totalP + totalA)) else 0f
    val totalMarksAll = classRows.sumOf { it.totalMarks }
    val obtMarksAll = classRows.sumOf { it.obtainedMarks }
    val overallPerfPct = if (totalMarksAll > 0) (obtMarksAll * 100.0 / totalMarksAll) else 0.0

    val headers = listOf("Class", "Students", "Present", "Absent", "Att %", "Tests", "Marks", "Perf %")
    val exportRows = classRows.map { r ->
        listOf(
            r.className,
            r.totalStudents.toString(),
            r.present.toString(),
            r.absent.toString(),
            "${String.format(Locale.US, "%.1f", r.attendancePct)}%",
            r.testsCount.toString(),
            "${r.obtainedMarks.toInt()}/${r.totalMarks.toInt()}",
            "${String.format(Locale.US, "%.1f", r.performancePct)}%"
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ReportExportUtils.writePdfReport(
                out,
                "School-Wide Weekly Report",
                "Week: $weekStart to $weekEnd | Parivar Education Society, Chikalpat",
                listOf(
                    "Total Students: ${state.summary.totalStudents}",
                    "Present Entries: $totalP | Absent Entries: $totalA | Attendance: ${String.format(Locale.US, "%.1f", overallAttPct)}%",
                    "Tests Conducted: ${weekTests.size} | Marks: ${obtMarksAll.toInt()}/${totalMarksAll.toInt()} (${String.format(Locale.US, "%.1f", overallPerfPct)}%)"
                ),
                headers,
                exportRows,
                AndroidColor.rgb(15, 157, 88)
            )
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val csv = ReportExportUtils.buildCsvContent(
                "Parivar Education Society - School-Wide Weekly Report",
                "Week: $weekStart to $weekEnd",
                listOf("Attendance: ${String.format(Locale.US, "%.1f", overallAttPct)}% | Tests: ${weekTests.size}"),
                headers,
                exportRows
            )
            out.write(csv.toByteArray(Charsets.UTF_8))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(WeeklyGreen, WeeklyGreenDark)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.weeklyReport, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Anganwadi, Classes 1–8 & JNV", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD1FAE5))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { weekRange = DateUtils.shiftWeek(weekStart, -1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Week", tint = Color.White)
                        }
                        Text("$weekStart  to  $weekEnd", color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { weekRange = DateUtils.shiftWeek(weekStart, 1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Week", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                ReportExportUtils.printReport(
                                    context, "Parivar_Weekly_Report",
                                    "School-Wide Weekly Report ($weekStart to $weekEnd)",
                                    "Parivar Education Society, Chikalpat",
                                    listOf("Attendance: ${String.format(Locale.US, "%.1f", overallAttPct)}% | Tests: ${weekTests.size}"),
                                    headers, exportRows, AndroidColor.rgb(15, 157, 88)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WeeklyGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = strings.printReport, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Print")
                        }
                        Button(
                            onClick = { pdfLauncher.launch("Parivar_Weekly_Report_$weekStart.pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WeeklyGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = strings.downloadPdf, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        Button(
                            onClick = { csvLauncher.launch("Parivar_Weekly_Report_$weekStart.csv") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WeeklyGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = strings.exportExcel, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Excel")
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatTile("Present", totalP.toString(), WeeklyGreen)
                    StatTile("Absent", totalA.toString(), AbsentCrimson)
                    StatTile("Att %", "${overallAttPct.toInt()}%", NavyPrimary)
                    StatTile("Tests", weekTests.size.toString(), MonthlyPurple)
                    StatTile("Perf %", "${overallPerfPct.toInt()}%", Color(0xFFD97706))
                }
            }
        }

        items(classRows, key = { it.className }) { row ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.className, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Text("Students: ${row.totalStudents}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Present: ${row.present} | Absent: ${row.absent} (${row.attendancePct.toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = WeeklyGreenDark)
                        Text("Tests: ${row.testsCount} (${row.performancePct.toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MonthlyPurpleDark)
                    }
                    if (row.testsCount > 0) {
                        Text("Marks: ${row.obtainedMarks.toInt()} / ${row.totalMarks.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var selectedMonth by remember { mutableIntStateOf(DateUtils.currentMonth()) }
    var selectedYear by remember { mutableIntStateOf(DateUtils.currentYear()) }
    var monthMenu by remember { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }

    val monthAtt = remember(state.attendance, selectedMonth, selectedYear) {
        state.attendance.filter { DateUtils.isDateInMonth(it.date, selectedMonth, selectedYear) }
    }
    val monthTests = remember(state.tests, selectedMonth, selectedYear) {
        state.tests.filter { DateUtils.isDateInMonth(it.testDate, selectedMonth, selectedYear) }
    }

    val workingDays = monthAtt.filter { it.status != "HOLIDAY" }.map { it.date }.distinct().size
    val presentDays = monthAtt.count { it.status == "PRESENT" }
    val absentDays = monthAtt.count { it.status == "ABSENT" }
    val attPct = if (presentDays + absentDays > 0) (presentDays * 100f / (presentDays + absentDays)) else 0f
    val totalMarks = monthTests.sumOf { it.totalMarks }
    val obtainedMarks = monthTests.sumOf { it.obtainedMarks }
    val overallPerf = if (totalMarks > 0) (obtainedMarks * 100.0 / totalMarks) else 0.0

    val classRows = remember(state.students, monthAtt, monthTests) {
        SchoolConstants.ALL_CLASSES.map { cls ->
            val stuCount = state.students.count { it.className == cls }
            val cAtt = monthAtt.filter { it.className == cls }
            val p = cAtt.count { it.status == "PRESENT" }
            val a = cAtt.count { it.status == "ABSENT" }
            val cAttPct = if (p + a > 0) (p * 100f / (p + a)) else 0f
            val cTests = monthTests.filter { it.className == cls }
            val tM = cTests.sumOf { it.totalMarks }
            val oM = cTests.sumOf { it.obtainedMarks }
            val perf = if (tM > 0) (oM * 100.0 / tM).toFloat() else 0f
            WeeklyClassRow(cls, stuCount, p, a, cAttPct, cTests.size, tM, oM, perf)
        }
    }

    val headers = listOf("Class", "Students", "Present", "Absent", "Att %", "Tests", "Marks", "Perf %")
    val exportRows = classRows.map { r ->
        listOf(
            r.className, r.totalStudents.toString(), r.present.toString(), r.absent.toString(),
            "${r.attendancePct.toInt()}%", r.testsCount.toString(),
            "${r.obtainedMarks.toInt()}/${r.totalMarks.toInt()}", "${r.performancePct.toInt()}%"
        )
    }

    val monthLabel = DateUtils.monthName(selectedMonth)
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ReportExportUtils.writePdfReport(
                out,
                "School-Wide Monthly Report",
                "$monthLabel $selectedYear | Parivar Education Society, Chikalpat",
                listOf(
                    "Total Students: ${state.summary.totalStudents} | Working Days: $workingDays",
                    "Present: $presentDays | Absent: $absentDays | Attendance: ${String.format(Locale.US, "%.1f", attPct)}%",
                    "Total Tests: ${monthTests.size} | Marks: ${obtainedMarks.toInt()}/${totalMarks.toInt()} (${String.format(Locale.US, "%.1f", overallPerf)}%)"
                ),
                headers, exportRows, AndroidColor.rgb(109, 40, 217)
            )
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val csv = ReportExportUtils.buildCsvContent(
                "Parivar Education Society - Monthly Report ($monthLabel $selectedYear)",
                "Working Days: $workingDays | Attendance: ${String.format(Locale.US, "%.1f", attPct)}%",
                listOf("Total Tests: ${monthTests.size} | Performance: ${String.format(Locale.US, "%.1f", overallPerf)}%"),
                headers, exportRows
            )
            out.write(csv.toByteArray(Charsets.UTF_8))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(MonthlyPurple, MonthlyPurpleDark)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.monthlyReport, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("$monthLabel $selectedYear • All Classes & JNV", style = MaterialTheme.typography.labelMedium, color = Color(0xFFEDE9FE))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                ReportExportUtils.printReport(
                                    context, "Parivar_Monthly_Report",
                                    "Monthly Report - $monthLabel $selectedYear",
                                    "Parivar Education Society, Chikalpat",
                                    listOf("Working Days: $workingDays | Attendance: ${String.format(Locale.US, "%.1f", attPct)}%"),
                                    headers, exportRows, AndroidColor.rgb(109, 40, 217)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MonthlyPurpleDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = strings.printReport, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Print")
                        }
                        Button(
                            onClick = { pdfLauncher.launch("Parivar_Monthly_${monthLabel}_$selectedYear.pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MonthlyPurpleDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = strings.downloadPdf, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        Button(
                            onClick = { csvLauncher.launch("Parivar_Monthly_${monthLabel}_$selectedYear.csv") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MonthlyPurpleDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = strings.exportExcel, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Excel")
                        }
                    }
                }
            }
        }

        // Month / Year selectors
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = monthMenu, onExpandedChange = { monthMenu = !monthMenu }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                        (1..12).forEach { m ->
                            DropdownMenuItem(text = { Text(DateUtils.monthName(m)) }, onClick = {
                                selectedMonth = m
                                monthMenu = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = yearMenu, onExpandedChange = { yearMenu = !yearMenu }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                        val baseY = DateUtils.currentYear()
                        ((baseY - 2)..(baseY + 1)).forEach { y ->
                            DropdownMenuItem(text = { Text(y.toString()) }, onClick = {
                                selectedYear = y
                                yearMenu = false
                            })
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile("Students", state.summary.totalStudents.toString(), NavyPrimary)
                        StatTile("Work Days", workingDays.toString(), MonthlyPurple)
                        StatTile("Present", presentDays.toString(), WeeklyGreen)
                        StatTile("Absent", absentDays.toString(), AbsentCrimson)
                    }
                    HorizontalDivider(color = SlateBorder)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile("Att %", "${attPct.toInt()}%", WeeklyGreenDark)
                        StatTile("Total Tests", monthTests.size.toString(), MonthlyPurple)
                        StatTile("Marks", "${obtainedMarks.toInt()}/${totalMarks.toInt()}", NavyPrimary)
                        StatTile("Perf %", "${overallPerf.toInt()}%", Color(0xFFD97706))
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Class-wise Monthly Performance Comparison", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(Modifier.height(8.dp))
                    val entries = classRows.map { r ->
                        (if (r.className == "Anganwadi") "Ang" else r.className.replace("Class ", "C")) to r.performancePct
                    }
                    BarChartCanvas(entries = entries, barColors = ClassCardColors, maxVal = 100f)
                }
            }
        }

        items(classRows, key = { it.className }) { row ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(row.className, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Text("Students: ${row.totalStudents} • Att: ${row.present}P/${row.absent}A (${row.attendancePct.toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${row.testsCount} Tests", style = MaterialTheme.typography.labelSmall, color = MonthlyPurple)
                        Text("${row.performancePct.toInt()}% Avg", fontWeight = FontWeight.Bold, color = WeeklyGreenDark)
                    }
                }
            }
        }
    }
}

private data class WeeklyClassRow(
    val className: String,
    val totalStudents: Int,
    val present: Int,
    val absent: Int,
    val attendancePct: Float,
    val testsCount: Int,
    val totalMarks: Double,
    val obtainedMarks: Double,
    val performancePct: Float
)

@Composable
private fun StatTile(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
    }
}
