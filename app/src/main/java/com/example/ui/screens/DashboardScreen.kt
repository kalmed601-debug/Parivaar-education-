package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Boy
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Girl
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.NoticeItem
import com.example.data.model.SchoolConstants
import com.example.ui.components.AppRoute
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ClassCardColors
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MonthlyPurple
import com.example.ui.theme.MonthlyPurpleDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryDark
import com.example.ui.theme.PresentEmerald
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.theme.WeeklyGreenDark
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils
import com.example.util.ReportExportUtils
import java.util.Locale

@Composable
fun DashboardScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onOpenClass: (String) -> Unit,
    onOpenJnv: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenStationery: () -> Unit,
    onQuickAddStudent: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSaveNotice: (NoticeItem) -> Unit,
    onDeleteNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val isAdminOrStaff = state.activeRole != "Parent"
    val isAdmin = state.activeRole == "Admin"

    var noticeToEdit by remember { mutableStateOf<NoticeItem?>(null) }
    var showNoticeDialog by remember { mutableStateOf(false) }

    val (weekStart, weekEnd) = remember { DateUtils.currentWeekRange() }
    val weeklyAtt = remember(state.attendance, weekStart, weekEnd) {
        state.attendance.filter { DateUtils.isDateInRange(it.date, weekStart, weekEnd) }
    }
    val weeklyTests = remember(state.tests, weekStart, weekEnd) {
        state.tests.filter { DateUtils.isDateInRange(it.testDate, weekStart, weekEnd) }
    }
    val weekPresent = weeklyAtt.count { it.status == "PRESENT" }
    val weekAbsent = weeklyAtt.count { it.status == "ABSENT" }
    val weekAttPct = if (weekPresent + weekAbsent > 0) (weekPresent * 100f / (weekPresent + weekAbsent)) else 0f
    val weekAvgScore = if (weeklyTests.isNotEmpty()) weeklyTests.map { it.percentage }.average().toFloat() else 0f

    val curMonth = remember { DateUtils.currentMonth() }
    val curYear = remember { DateUtils.currentYear() }
    val monthlyAtt = remember(state.attendance, curMonth, curYear) {
        state.attendance.filter { DateUtils.isDateInMonth(it.date, curMonth, curYear) }
    }
    val monthlyTests = remember(state.tests, curMonth, curYear) {
        state.tests.filter { DateUtils.isDateInMonth(it.testDate, curMonth, curYear) }
    }
    val monthPresent = monthlyAtt.count { it.status == "PRESENT" }
    val monthAbsent = monthlyAtt.count { it.status == "ABSENT" }
    val monthWorkingDays = monthlyAtt.filter { it.status != "HOLIDAY" }.map { it.date }.distinct().size
    val monthAttPct = if (monthPresent + monthAbsent > 0) (monthPresent * 100f / (monthPresent + monthAbsent)) else 0f
    val monthAvgScore = if (monthlyTests.isNotEmpty()) monthlyTests.map { it.percentage }.average().toFloat() else 0f

    // PDF & Excel launchers for Weekly & Monthly dashboard panels
    var pendingPdfType by remember { mutableStateOf("weekly") }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            if (pendingPdfType == "weekly") {
                val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                    val clsTotal = state.students.count { it.className == cls }
                    val clsAtt = weeklyAtt.filter { it.className == cls }
                    val p = clsAtt.count { it.status == "PRESENT" }
                    val a = clsAtt.count { it.status == "ABSENT" }
                    val pct = if (p + a > 0) "${(p * 100 / (p + a))}%" else "0%"
                    val tCount = weeklyTests.count { it.className == cls }
                    listOf(cls, clsTotal.toString(), p.toString(), a.toString(), pct, tCount.toString())
                }
                ReportExportUtils.writePdfReport(
                    outputStream = out,
                    title = "Weekly School Report ($weekStart to $weekEnd)",
                    subtitle = "Parivar Education Society, Chikalpat",
                    summaryLines = listOf(
                        "Total Students: ${state.summary.totalStudents}",
                        "Present Entries: $weekPresent | Absent Entries: $weekAbsent (${String.format(Locale.US, "%.1f", weekAttPct)}%)",
                        "Tests Conducted: ${weeklyTests.size} | Avg Performance: ${String.format(Locale.US, "%.1f", weekAvgScore)}%"
                    ),
                    headers = listOf("Class", "Students", "Present", "Absent", "Att %", "Tests"),
                    rows = rows,
                    accentColor = AndroidColor.rgb(15, 157, 88)
                )
            } else {
                val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                    val clsTotal = state.students.count { it.className == cls }
                    val clsAtt = monthlyAtt.filter { it.className == cls }
                    val p = clsAtt.count { it.status == "PRESENT" }
                    val a = clsAtt.count { it.status == "ABSENT" }
                    val pct = if (p + a > 0) "${(p * 100 / (p + a))}%" else "0%"
                    val tCount = monthlyTests.count { it.className == cls }
                    listOf(cls, clsTotal.toString(), p.toString(), a.toString(), pct, tCount.toString())
                }
                ReportExportUtils.writePdfReport(
                    outputStream = out,
                    title = "Monthly Report - ${DateUtils.monthName(curMonth)} $curYear",
                    subtitle = "Parivar Education Society, Chikalpat",
                    summaryLines = listOf(
                        "Working Days Recorded: $monthWorkingDays | Total Students: ${state.summary.totalStudents}",
                        "Attendance: $monthPresent Present / $monthAbsent Absent (${String.format(Locale.US, "%.1f", monthAttPct)}%)",
                        "Tests: ${monthlyTests.size} | Avg Performance: ${String.format(Locale.US, "%.1f", monthAvgScore)}%"
                    ),
                    headers = listOf("Class", "Students", "Present", "Absent", "Att %", "Tests"),
                    rows = rows,
                    accentColor = AndroidColor.rgb(109, 40, 217)
                )
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val isWeekly = pendingPdfType == "weekly"
            val attSrc = if (isWeekly) weeklyAtt else monthlyAtt
            val testSrc = if (isWeekly) weeklyTests else monthlyTests
            val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                val clsTotal = state.students.count { it.className == cls }
                val clsAtt = attSrc.filter { it.className == cls }
                val p = clsAtt.count { it.status == "PRESENT" }
                val a = clsAtt.count { it.status == "ABSENT" }
                val pct = if (p + a > 0) "${(p * 100 / (p + a))}%" else "0%"
                val tCount = testSrc.count { it.className == cls }
                listOf(cls, clsTotal.toString(), p.toString(), a.toString(), pct, tCount.toString())
            }
            val csv = ReportExportUtils.buildCsvContent(
                title = if (isWeekly) "Weekly Report ($weekStart to $weekEnd)" else "Monthly Report (${DateUtils.monthName(curMonth)} $curYear)",
                subtitle = "Parivar Education Society, Chikalpat",
                summaryLines = listOf("Total Students: ${state.summary.totalStudents}"),
                headers = listOf("Class", "Students", "Present", "Absent", "Attendance %", "Tests Conducted"),
                rows = rows
            )
            out.write(csv.toByteArray(Charsets.UTF_8))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Society Welcome Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(NavyPrimaryDark, NavyPrimary, Color(0xFF1D4ED8))
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AmberGold.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Academic Session ${state.profile.academicYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.profile.schoolName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.profile.branchLocation} • ${strings.appTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDBEAFE)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_society_banner),
                        contentDescription = "Parivar Society Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 108.dp, height = 76.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        }

        // 2. Dashboard Summary Cards (6 Dynamic Metrics)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth >= 820.dp) 6 else if (maxWidth >= 540.dp) 3 else 2
                val cards = listOf(
                    SummaryCardData(strings.totalStudents, state.summary.totalStudents.toString(), Icons.Default.Groups, Color(0xFF2563EB)),
                    SummaryCardData(strings.totalClasses, state.summary.totalClasses.toString(), Icons.Default.Class, Color(0xFF7C3AED)),
                    SummaryCardData(strings.totalBoys, state.summary.totalBoys.toString(), Icons.Default.Boy, Color(0xFF0284C7)),
                    SummaryCardData(strings.totalGirls, state.summary.totalGirls.toString(), Icons.Default.Girl, Color(0xFFEC4899)),
                    SummaryCardData(strings.todayPresent, state.summary.todayPresent.toString(), Icons.Default.CheckCircle, WeeklyGreen),
                    SummaryCardData(strings.todayAbsent, state.summary.todayAbsent.toString(), Icons.Default.WarningAmber, AbsentCrimson)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cards.chunked(cols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                SummaryMetricCard(
                                    data = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(cols - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Access Colorful Action Buttons
        item {
            SectionHeaderTitle(title = strings.quickAccess, icon = Icons.Default.School)
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth >= 760.dp) 6 else if (maxWidth >= 480.dp) 3 else 2
                val actions = listOf(
                    QuickActionData(strings.addStudent, Icons.Default.PersonAdd, Color(0xFF2563EB)) {
                        if (isAdminOrStaff) onQuickAddStudent() else onNavigate(AppRoute.STUDENTS)
                    },
                    QuickActionData(strings.takeTest, Icons.Default.Quiz, Color(0xFF0F9D58)) {
                        onNavigate(AppRoute.TESTS)
                    },
                    QuickActionData(strings.viewResult, Icons.Default.Assessment, Color(0xFF7C3AED)) {
                        onNavigate(AppRoute.REPORTS_OVERVIEW)
                    },
                    QuickActionData(strings.bookDetails, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF0284C7)) {
                        onOpenBooks()
                    },
                    QuickActionData(strings.stationeryStock, Icons.Default.Toys, Color(0xFFD97706)) {
                        onOpenStationery()
                    },
                    QuickActionData(strings.backupData, Icons.Default.Backup, Color(0xFFDC2626)) {
                        onNavigate(AppRoute.BACKUP_RESTORE)
                    }
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    actions.chunked(cols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { act ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = act.color,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { act.onClick() }
                                        .testTag("quick_${act.label.lowercase().replace(" ", "_")}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(act.icon, contentDescription = act.label, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = act.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Main Class-Wise Dashboard (12 Colorful Clickable Cards)
        item {
            SectionHeaderTitle(title = strings.classWiseDashboard, icon = Icons.Default.Class)
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth >= 900.dp) 4 else if (maxWidth >= 560.dp) 3 else 2
                val classCards = listOf(
                    ClassTileData("Anganwadi", "${state.summary.classStudentCounts["Anganwadi"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Anganwadi"] ?: 0}", Icons.Default.School, ClassCardColors[0]) { onOpenClass("Anganwadi") },
                    ClassTileData("Class 1", "${state.summary.classStudentCounts["Class 1"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 1"] ?: 0}", Icons.Default.Class, ClassCardColors[1]) { onOpenClass("Class 1") },
                    ClassTileData("Class 2", "${state.summary.classStudentCounts["Class 2"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 2"] ?: 0}", Icons.Default.Class, ClassCardColors[2]) { onOpenClass("Class 2") },
                    ClassTileData("Class 3", "${state.summary.classStudentCounts["Class 3"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 3"] ?: 0}", Icons.Default.Class, ClassCardColors[3]) { onOpenClass("Class 3") },
                    ClassTileData("Class 4", "${state.summary.classStudentCounts["Class 4"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 4"] ?: 0}", Icons.Default.Class, ClassCardColors[4]) { onOpenClass("Class 4") },
                    ClassTileData("Class 5", "${state.summary.classStudentCounts["Class 5"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 5"] ?: 0}", Icons.Default.Class, ClassCardColors[5]) { onOpenClass("Class 5") },
                    ClassTileData("Class 6", "${state.summary.classStudentCounts["Class 6"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 6"] ?: 0}", Icons.Default.Class, ClassCardColors[6]) { onOpenClass("Class 6") },
                    ClassTileData("Class 7", "${state.summary.classStudentCounts["Class 7"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 7"] ?: 0}", Icons.Default.Class, ClassCardColors[7]) { onOpenClass("Class 7") },
                    ClassTileData("Class 8", "${state.summary.classStudentCounts["Class 8"] ?: 0} Students", "Today P: ${state.summary.classTodayPresentCounts["Class 8"] ?: 0}", Icons.Default.Class, ClassCardColors[8]) { onOpenClass("Class 8") },
                    ClassTileData("JNV", "${state.summary.classStudentCounts["JNV"] ?: 0} Students", "Navodaya Prep", Icons.Default.EmojiEvents, ClassCardColors[9]) { onOpenJnv() },
                    ClassTileData("All Books", "${state.books.size} Titles", "Avail: ${state.summary.totalBooksAvailable}", Icons.AutoMirrored.Filled.MenuBook, ClassCardColors[10]) { onOpenBooks() },
                    ClassTileData("All Stationery", "${state.stationery.size} Items", "Avail: ${state.summary.totalStationeryAvailable}", Icons.Default.Toys, ClassCardColors[11]) { onOpenStationery() }
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    classCards.chunked(cols).forEach { rowTiles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTiles.forEach { tile ->
                                ClassTileCard(tile = tile, modifier = Modifier.weight(1f))
                            }
                            repeat(cols - rowTiles.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 5. Green Weekly Report Panel & Purple Monthly Report Panel (SEPARATE ON MAIN DASHBOARD)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 780.dp
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WeeklyReportDashboardPanel(
                            strings = strings,
                            weekStart = weekStart,
                            weekEnd = weekEnd,
                            totalStudents = state.summary.totalStudents,
                            presentCount = weekPresent,
                            absentCount = weekAbsent,
                            attendancePct = weekAttPct,
                            testsCount = weeklyTests.size,
                            avgPerfPct = weekAvgScore,
                            onViewFull = { onNavigate(AppRoute.WEEKLY_REPORT) },
                            onPrint = {
                                val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                                    val cAtt = weeklyAtt.filter { it.className == cls }
                                    val p = cAtt.count { it.status == "PRESENT" }
                                    val a = cAtt.count { it.status == "ABSENT" }
                                    listOf(cls, state.students.count { it.className == cls }.toString(), p.toString(), a.toString())
                                }
                                ReportExportUtils.printReport(
                                    context,
                                    "Parivar_Weekly_Report",
                                    "Weekly Report ($weekStart to $weekEnd)",
                                    "Parivar Education Society, Chikalpat",
                                    listOf("Present: $weekPresent | Absent: $weekAbsent | Tests: ${weeklyTests.size}"),
                                    listOf("Class", "Students", "Present", "Absent"),
                                    rows,
                                    AndroidColor.rgb(15, 157, 88)
                                )
                            },
                            onDownloadPdf = {
                                pendingPdfType = "weekly"
                                pdfLauncher.launch("Parivar_Weekly_Report_$weekStart.pdf")
                            },
                            onExportExcel = {
                                pendingPdfType = "weekly"
                                csvLauncher.launch("Parivar_Weekly_Report_$weekStart.csv")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyReportDashboardPanel(
                            strings = strings,
                            monthName = DateUtils.monthName(curMonth),
                            year = curYear,
                            totalStudents = state.summary.totalStudents,
                            workingDays = monthWorkingDays,
                            presentDays = monthPresent,
                            absentDays = monthAbsent,
                            attendancePct = monthAttPct,
                            testsCount = monthlyTests.size,
                            avgPerfPct = monthAvgScore,
                            onViewFull = { onNavigate(AppRoute.MONTHLY_REPORT) },
                            onPrint = {
                                val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                                    val cAtt = monthlyAtt.filter { it.className == cls }
                                    val p = cAtt.count { it.status == "PRESENT" }
                                    val a = cAtt.count { it.status == "ABSENT" }
                                    listOf(cls, state.students.count { it.className == cls }.toString(), p.toString(), a.toString())
                                }
                                ReportExportUtils.printReport(
                                    context,
                                    "Parivar_Monthly_Report",
                                    "Monthly Report - ${DateUtils.monthName(curMonth)} $curYear",
                                    "Parivar Education Society, Chikalpat",
                                    listOf("Working Days: $monthWorkingDays | Attendance: ${String.format(Locale.US, "%.1f", monthAttPct)}%"),
                                    listOf("Class", "Students", "Present", "Absent"),
                                    rows,
                                    AndroidColor.rgb(109, 40, 217)
                                )
                            },
                            onDownloadPdf = {
                                pendingPdfType = "monthly"
                                pdfLauncher.launch("Parivar_Monthly_${DateUtils.monthName(curMonth)}_$curYear.pdf")
                            },
                            onExportExcel = {
                                pendingPdfType = "monthly"
                                csvLauncher.launch("Parivar_Monthly_${DateUtils.monthName(curMonth)}_$curYear.csv")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        WeeklyReportDashboardPanel(
                            strings = strings,
                            weekStart = weekStart,
                            weekEnd = weekEnd,
                            totalStudents = state.summary.totalStudents,
                            presentCount = weekPresent,
                            absentCount = weekAbsent,
                            attendancePct = weekAttPct,
                            testsCount = weeklyTests.size,
                            avgPerfPct = weekAvgScore,
                            onViewFull = { onNavigate(AppRoute.WEEKLY_REPORT) },
                            onPrint = {
                                val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                                    val cAtt = weeklyAtt.filter { it.className == cls }
                                    listOf(cls, state.students.count { it.className == cls }.toString(), cAtt.count { it.status == "PRESENT" }.toString(), cAtt.count { it.status == "ABSENT" }.toString())
                                }
                                ReportExportUtils.printReport(
                                    context, "Parivar_Weekly_Report", "Weekly Report ($weekStart to $weekEnd)",
                                    "Parivar Education Society, Chikalpat", listOf("Attendance: ${String.format(Locale.US, "%.1f", weekAttPct)}%"),
                                    listOf("Class", "Students", "Present", "Absent"), rows, AndroidColor.rgb(15, 157, 88)
                                )
                            },
                            onDownloadPdf = {
                                pendingPdfType = "weekly"
                                pdfLauncher.launch("Parivar_Weekly_Report_$weekStart.pdf")
                            },
                            onExportExcel = {
                                pendingPdfType = "weekly"
                                csvLauncher.launch("Parivar_Weekly_Report_$weekStart.csv")
                            }
                        )
                        MonthlyReportDashboardPanel(
                            strings = strings,
                            monthName = DateUtils.monthName(curMonth),
                            year = curYear,
                            totalStudents = state.summary.totalStudents,
                            workingDays = monthWorkingDays,
                            presentDays = monthPresent,
                            absentDays = monthAbsent,
                            attendancePct = monthAttPct,
                            testsCount = monthlyTests.size,
                            avgPerfPct = monthAvgScore,
                            onViewFull = { onNavigate(AppRoute.MONTHLY_REPORT) },
                            onPrint = {
                                val rows = SchoolConstants.ALL_CLASSES.map { cls ->
                                    val cAtt = monthlyAtt.filter { it.className == cls }
                                    listOf(cls, state.students.count { it.className == cls }.toString(), cAtt.count { it.status == "PRESENT" }.toString(), cAtt.count { it.status == "ABSENT" }.toString())
                                }
                                ReportExportUtils.printReport(
                                    context, "Parivar_Monthly_Report", "Monthly Report - ${DateUtils.monthName(curMonth)} $curYear",
                                    "Parivar Education Society, Chikalpat", listOf("Working Days: $monthWorkingDays"),
                                    listOf("Class", "Students", "Present", "Absent"), rows, AndroidColor.rgb(109, 40, 217)
                                )
                            },
                            onDownloadPdf = {
                                pendingPdfType = "monthly"
                                pdfLauncher.launch("Parivar_Monthly_${DateUtils.monthName(curMonth)}_$curYear.pdf")
                            },
                            onExportExcel = {
                                pendingPdfType = "monthly"
                                csvLauncher.launch("Parivar_Monthly_${DateUtils.monthName(curMonth)}_$curYear.csv")
                            }
                        )
                    }
                }
            }
        }

        // 6. Overall Statistics & Class-wise Graph
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderTitle(title = strings.overallStatistics, icon = Icons.Default.Assessment)
                        TextButton(onClick = { onNavigate(AppRoute.REPORTS_OVERVIEW) }) {
                            Text("All Graphs")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniStatPill(strings.totalBoys, state.summary.totalBoys.toString(), Color(0xFF0284C7), Modifier.weight(1f))
                        MiniStatPill(strings.totalGirls, state.summary.totalGirls.toString(), Color(0xFFEC4899), Modifier.weight(1f))
                        MiniStatPill(strings.totalPass, state.summary.totalPassTests.toString(), WeeklyGreen, Modifier.weight(1f))
                        MiniStatPill(strings.totalFail, state.summary.totalFailTests.toString(), AbsentCrimson, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Class-wise Student Distribution", style = MaterialTheme.typography.titleSmall, color = NavyPrimary)
                    Spacer(Modifier.height(8.dp))
                    val chartEntries = SchoolConstants.ALL_CLASSES.map { cls ->
                        val shortLabel = when (cls) {
                            "Anganwadi" -> "Ang"
                            "JNV" -> "JNV"
                            else -> cls.replace("Class ", "C")
                        }
                        shortLabel to (state.summary.classStudentCounts[cls] ?: 0).toFloat()
                    }
                    BarChartCanvas(
                        entries = chartEntries,
                        barColors = ClassCardColors
                    )
                }
            }
        }

        // 7. Important Notice Board (Admin Add / Edit / Delete)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderTitle(title = strings.importantNotice, icon = Icons.Default.Campaign)
                        if (isAdmin) {
                            Button(
                                onClick = {
                                    noticeToEdit = null
                                    showNoticeDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_notice_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = strings.addNotice, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.addNotice)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (state.notices.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.Campaign,
                            title = "No Notices Posted",
                            subtitle = "Administrators can publish school announcements for teachers, parents, and students."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.notices.take(6).forEach { notice ->
                                val urgent = notice.priority == "Urgent"
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (urgent) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (urgent) Color(0xFFFCA5A5) else SlateBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (urgent) AbsentCrimson else IndigoAccent
                                                ) {
                                                    Text(
                                                        text = "${notice.priority} • ${notice.targetAudience}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(notice.dateStr, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(notice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(2.dp))
                                            Text(notice.message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155))
                                        }
                                        if (isAdmin) {
                                            IconButton(onClick = {
                                                noticeToEdit = notice
                                                showNoticeDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Notice", tint = IndigoAccent)
                                            }
                                            IconButton(onClick = { onDeleteNotice(notice.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Notice", tint = AbsentCrimson)
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

        // 8. Recent Activities Feed
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeaderTitle(title = strings.recentActivities, icon = Icons.Default.History)
                    Spacer(Modifier.height(10.dp))
                    if (state.activities.isEmpty()) {
                        Text(
                            text = "Activities will appear automatically as students, attendance, tests, books, and stationery are updated.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.activities.take(8).forEach { act ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = NavyPrimary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.History, contentDescription = act.type, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(act.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (act.description.isNotBlank()) {
                                            Text(act.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                                        }
                                    }
                                    Text(act.dateTimeLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showNoticeDialog) {
        NoticeEditorDialog(
            initial = noticeToEdit,
            onDismiss = { showNoticeDialog = false },
            onSave = {
                onSaveNotice(it)
                showNoticeDialog = false
            }
        )
    }
}

private data class SummaryCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun SummaryMetricCard(data: SummaryCardData, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, data.color.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = data.color.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(data.icon, contentDescription = data.title, tint = data.color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = data.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class QuickActionData(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

private data class ClassTileData(
    val title: String,
    val countText: String,
    val subText: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun ClassTileCard(tile: ClassTileData, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { tile.onClick() }
            .testTag("class_card_${tile.title.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(tile.color, tile.color.copy(alpha = 0.82f))
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(tile.icon, contentDescription = tile.title, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = tile.countText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = tile.subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun WeeklyReportDashboardPanel(
    strings: LocalizedStrings,
    weekStart: String,
    weekEnd: String,
    totalStudents: Int,
    presentCount: Int,
    absentCount: Int,
    attendancePct: Float,
    testsCount: Int,
    avgPerfPct: Float,
    onViewFull: () -> Unit,
    onPrint: () -> Unit,
    onDownloadPdf: () -> Unit,
    onExportExcel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.testTag("dashboard_weekly_report_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(WeeklyGreen, WeeklyGreenDark)))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = strings.weeklyReport, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.weeklyReport, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("$weekStart to $weekEnd • School-wide", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD1FAE5))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelStatBox("Students", totalStudents.toString(), Modifier.weight(1f))
                PanelStatBox("Present / Abs", "$presentCount / $absentCount", Modifier.weight(1f))
                PanelStatBox("Att %", "${String.format(Locale.US, "%.0f", attendancePct)}%", Modifier.weight(1f))
                PanelStatBox("Tests / Avg", "$testsCount (${avgPerfPct.toInt()}%)", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onViewFull,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WeeklyGreenDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.viewFullReport, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Print, contentDescription = strings.printReport, tint = Color.White)
                }
                IconButton(
                    onClick = onDownloadPdf,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Download, contentDescription = strings.downloadPdf, tint = Color.White)
                }
                IconButton(
                    onClick = onExportExcel,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = strings.exportExcel, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MonthlyReportDashboardPanel(
    strings: LocalizedStrings,
    monthName: String,
    year: Int,
    totalStudents: Int,
    workingDays: Int,
    presentDays: Int,
    absentDays: Int,
    attendancePct: Float,
    testsCount: Int,
    avgPerfPct: Float,
    onViewFull: () -> Unit,
    onPrint: () -> Unit,
    onDownloadPdf: () -> Unit,
    onExportExcel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.testTag("dashboard_monthly_report_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MonthlyPurple, MonthlyPurpleDark)))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = strings.monthlyReport, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.monthlyReport, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("$monthName $year • All Classes & JNV", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEDE9FE))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelStatBox("Working Days", workingDays.toString(), Modifier.weight(1f))
                PanelStatBox("Students", totalStudents.toString(), Modifier.weight(1f))
                PanelStatBox("Att %", "${String.format(Locale.US, "%.0f", attendancePct)}%", Modifier.weight(1f))
                PanelStatBox("Tests / Avg", "$testsCount (${avgPerfPct.toInt()}%)", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onViewFull,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MonthlyPurpleDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.viewFullReport, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Print, contentDescription = strings.printReport, tint = Color.White)
                }
                IconButton(
                    onClick = onDownloadPdf,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Download, contentDescription = strings.downloadPdf, tint = Color.White)
                }
                IconButton(
                    onClick = onExportExcel,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = strings.exportExcel, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PanelStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
        }
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionHeaderTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = NavyPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeEditorDialog(
    initial: NoticeItem?,
    onDismiss: () -> Unit,
    onSave: (NoticeItem) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var message by remember { mutableStateOf(initial?.message ?: "") }
    var audience by remember { mutableStateOf(initial?.targetAudience ?: "All") }
    var priority by remember { mutableStateOf(initial?.priority ?: "Normal") }
    var audienceExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Important Notice" else "Edit Notice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notice Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Announcement Message*") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = audienceExpanded,
                    onExpandedChange = { audienceExpanded = !audienceExpanded }
                ) {
                    OutlinedTextField(
                        value = audience,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Audience") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = audienceExpanded,
                        onDismissRequest = { audienceExpanded = false }
                    ) {
                        SchoolConstants.NOTICE_AUDIENCES.forEach { aud ->
                            DropdownMenuItem(
                                text = { Text(aud) },
                                onClick = {
                                    audience = aud
                                    audienceExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Normal", "Urgent").forEach { p ->
                        OutlinedButton(
                            onClick = { priority = p },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (priority == p) (if (p == "Urgent") AbsentCrimson else NavyPrimary) else Color.Transparent,
                                contentColor = if (priority == p) Color.White else NavyPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(p)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        onSave(
                            (initial ?: NoticeItem()).copy(
                                title = title,
                                message = message,
                                targetAudience = audience,
                                priority = priority,
                                dateStr = initial?.dateStr?.ifBlank { DateUtils.todayIso() } ?: DateUtils.todayIso(),
                                authorName = "Admin"
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && message.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
