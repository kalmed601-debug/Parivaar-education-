package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SchoolConstants
import com.example.data.model.StaffParentMember
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils

@Composable
fun SettingsScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetRole: (String) -> Unit,
    onUpdateSchool: (String, String, String) -> Unit,
    onSaveMember: (StaffParentMember) -> Unit,
    onDeleteMember: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var schoolName by remember(state.profile.schoolName) { mutableStateOf(state.profile.schoolName) }
    var branchLocation by remember(state.profile.branchLocation) { mutableStateOf(state.profile.branchLocation) }
    var academicYear by remember(state.profile.academicYear) { mutableStateOf(state.profile.academicYear) }
    var showAddMember by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(strings.settings, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        }

        // Language & Role Access Switcher
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Language / भाषा", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = state.language == AppLanguage.ENGLISH,
                            onClick = { onSetLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = state.language == AppLanguage.HINDI,
                            onClick = { onSetLanguage(AppLanguage.HINDI) },
                            label = { Text("हिन्दी (Hindi)") }
                        )
                    }

                    Text("Role-Based Access Mode", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(
                        text = "Admin: Full control • Staff: Attendance & Marks • Parent: Read-only Student & Notice Portal",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SchoolConstants.ROLES.forEach { role ->
                            FilterChip(
                                selected = state.activeRole == role,
                                onClick = { onSetRole(role) },
                                label = { Text(role) }
                            )
                        }
                    }
                }
            }
        }

        // Institution Details
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Society & School Configuration", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("Society / School Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = branchLocation, onValueChange = { branchLocation = it }, label = { Text("Village / Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = academicYear, onValueChange = { academicYear = it }, label = { Text("Academic Year") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onUpdateSchool(schoolName, branchLocation, academicYear) },
                        enabled = state.activeRole == "Admin",
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }

        // Staff & Parent Role Directory
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Staff & Parent Directory", tint = NavyPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Staff & Parent Access Directory", fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                        if (state.activeRole == "Admin") {
                            OutlinedButton(onClick = { showAddMember = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Member", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }

                    if (state.members.isEmpty()) {
                        Text("No staff or parent profiles added yet.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                    } else {
                        state.members.forEach { m ->
                            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, SlateBorder)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${m.name} (${m.role})", fontWeight = FontWeight.Bold)
                                        Text("Class: ${m.assignedClass.ifBlank { "All" }} • Phone: ${m.phone.ifBlank { "-" }}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    if (state.activeRole == "Admin") {
                                        IconButton(onClick = { onDeleteMember(m.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Member", tint = AbsentCrimson)
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

    if (showAddMember) {
        var mName by remember { mutableStateOf("") }
        var mRole by remember { mutableStateOf("Staff") }
        var mPhone by remember { mutableStateOf("") }
        var mClass by remember { mutableStateOf("All") }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Staff / Parent Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = mName, onValueChange = { mName = it }, label = { Text("Full Name*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Staff", "Parent", "Admin").forEach { r ->
                            FilterChip(selected = mRole == r, onClick = { mRole = r }, label = { Text(r) })
                        }
                    }
                    OutlinedTextField(value = mPhone, onValueChange = { mPhone = it }, label = { Text("Mobile Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mClass, onValueChange = { mClass = it }, label = { Text("Assigned Class (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveMember(StaffParentMember(name = mName, role = mRole, phone = mPhone, assignedClass = mClass, active = true))
                        showAddMember = false
                    },
                    enabled = mName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddMember = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun BackupRestoreScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    buildBackupJson: () -> String,
    onRestoreJson: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var statusText by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val json = buildBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            statusText = "Backup exported successfully!"
        } catch (e: Exception) {
            statusText = "Export failed: ${e.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (json.isNotBlank()) {
                onRestoreJson(json)
                statusText = "Restoring records to Cloud Firestore..."
            }
        } catch (e: Exception) {
            statusText = "Restore read failed: ${e.message}"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(strings.backupRestore, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7EE)), border = BorderStroke(1.dp, WeeklyGreen.copy(alpha = 0.35f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Cloud Sync Active", tint = WeeklyGreen, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Automatic Cloud Synchronization Active", fontWeight = FontWeight.Bold, color = WeeklyGreen)
                        Text(
                            text = "All ${state.students.size} students, ${state.attendance.size} attendance entries, ${state.tests.size} tests, ${state.books.size} books & ${state.stationery.size} stationery items are backed up live to Firebase Firestore.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Manual Snapshot Backup & Restore (JSON)", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(
                        text = "Download a complete offline JSON backup or restore records safely without overwriting existing school data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )
                    Button(
                        onClick = { exportLauncher.launch("Parivar_EMS_Backup_${DateUtils.todayIso()}.json") },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Backup")
                        Spacer(Modifier.width(8.dp))
                        Text("Download Complete Backup (.json)")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        enabled = state.activeRole == "Admin",
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore Backup")
                        Spacer(Modifier.width(8.dp))
                        Text("Restore From Backup File (.json)")
                    }
                    if (statusText != null) {
                        Text(statusText!!, color = IndigoAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpAndAboutScreen(
    isAbout: Boolean,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(if (isAbout) strings.aboutUs else strings.help, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        }

        if (isAbout) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = "Society", tint = NavyPrimary, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Parivar Education Society", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text("Chikalpat", style = MaterialTheme.typography.titleSmall, color = IndigoAccent)
                            }
                        }
                        Text(
                            text = "Parivar Education Management System is built to empower teachers, staff, administrators, and parents at Parivar Education Society, Chikalpat — unifying Anganwadi, Classes 1 to 8, Jawahar Navodaya Vidyalaya (JNV) entrance preparation, textbook & stationery inventory, and automated weekly/monthly reporting.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = NavyPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Quick Teacher & Admin Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                        Text("1. Tap any class card (Anganwadi to Class 8) to view, add, or edit students and open their 6-tab Student Profile.")
                        Text("2. Tap JNV to manage Jawahar Navodaya Vidyalaya preparation, mock tests, and entrance tracking.")
                        Text("3. Use Attendance to mark daily Present, Absent, or Holiday for an entire class with one tap.")
                        Text("4. Weekly Report (Green) and Monthly Report (Purple) calculate attendance & exam averages automatically and export real PDF, Print, and Excel CSV files.")
                        Text("5. Switch between English & Hindi (हिन्दी) anytime using the globe button in the top bar.")
                    }
                }
            }
        }
    }
}
