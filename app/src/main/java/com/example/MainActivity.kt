package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.SchoolRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AppRoute
import com.example.ui.components.DarkBlueHeaderBar
import com.example.ui.components.SidebarDrawerContent
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.BooksScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HelpAndAboutScreen
import com.example.ui.screens.JnvDashboardScreen
import com.example.ui.screens.MonthlyReportScreen
import com.example.ui.screens.ReportsOverviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StationeryScreen
import com.example.ui.screens.StudentManagementScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.TestExamScreen
import com.example.ui.screens.WeeklyReportScreen
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.ParivarEmsTheme
import com.example.ui.theme.SurfaceLight
import com.example.ui.viewmodel.DataLoadState
import com.example.ui.viewmodel.SchoolViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParivarEmsTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = auth.currentUser })
    } else {
        key(user.uid) {
            val viewModel: SchoolViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        SchoolViewModel(
                            repository = SchoolRepository(db, auth),
                            currentUserId = user.uid,
                            initialDisplayName = user.displayName.orEmpty(),
                            initialEmail = user.email.orEmpty()
                        )
                    }
                }
            )
            ParivarMainShell(
                viewModel = viewModel,
                userName = user.displayName ?: "Administrator",
                userEmail = user.email.orEmpty()
            )
        }
    }
}

@Composable
private fun ParivarMainShell(
    viewModel: SchoolViewModel,
    userName: String,
    userEmail: String
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = stringsFor(state.language)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var currentRoute by remember { mutableStateOf(AppRoute.DASHBOARD) }
    var previousRoute by remember { mutableStateOf(AppRoute.DASHBOARD) }
    var selectedClassFilter by remember { mutableStateOf("All") }
    var openAddStudentPrompt by remember { mutableStateOf(false) }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    fun navigateTo(route: AppRoute, classFilter: String? = null, openAddStudent: Boolean = false) {
        if (currentRoute != route) {
            previousRoute = currentRoute
        }
        if (classFilter != null) {
            selectedClassFilter = classFilter
        }
        openAddStudentPrompt = openAddStudent
        currentRoute = route
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ParivarEmsTheme(isHindi = state.language == AppLanguage.HINDI) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(SurfaceLight)) {
            val isWideScreen = maxWidth >= 900.dp

            val drawerContent: @Composable () -> Unit = {
                SidebarDrawerContent(
                    currentRoute = currentRoute,
                    strings = strings,
                    userName = userName,
                    userEmail = userEmail,
                    activeRole = state.activeRole,
                    onSelectRoute = { route ->
                        val cls = if (route == AppRoute.STUDENTS) "All" else null
                        navigateTo(route, classFilter = cls)
                        if (!isWideScreen) {
                            scope.launch { drawerState.close() }
                        }
                    },
                    onLogout = {
                        if (!isWideScreen) {
                            scope.launch { drawerState.close() }
                        }
                        showLogoutConfirm = true
                    }
                )
            }

            val mainBody: @Composable () -> Unit = {
                Scaffold(
                    containerColor = SurfaceLight,
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.navigationBarsPadding()
                        )
                    },
                    topBar = {
                        DarkBlueHeaderBar(
                            strings = strings,
                            language = state.language,
                            activeRole = state.activeRole,
                            notificationCount = state.notices.size,
                            showMenuButton = !isWideScreen,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onHomeClick = { navigateTo(AppRoute.DASHBOARD) },
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onNotificationsClick = { showNotificationsDialog = true },
                            onProfileClick = { showProfileDialog = true },
                            onSettingsClick = { navigateTo(AppRoute.SETTINGS) },
                            onLogoutClick = { showLogoutConfirm = true }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .navigationBarsPadding()
                    ) {
                        when (val load = state.loadState) {
                            DataLoadState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = NavyPrimary)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Syncing Parivar School Database...", color = NavyPrimary)
                                    }
                                }
                            }
                            is DataLoadState.Error -> {
                                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Error: ${load.message}", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            DataLoadState.Ready -> {
                                when (currentRoute) {
                                    AppRoute.DASHBOARD -> DashboardScreen(
                                        state = state,
                                        strings = strings,
                                        onOpenClass = { cls -> navigateTo(AppRoute.STUDENTS, classFilter = cls) },
                                        onOpenJnv = { navigateTo(AppRoute.JNV) },
                                        onOpenBooks = { navigateTo(AppRoute.BOOKS) },
                                        onOpenStationery = { navigateTo(AppRoute.STATIONERY) },
                                        onQuickAddStudent = { navigateTo(AppRoute.STUDENTS, classFilter = "All", openAddStudent = true) },
                                        onNavigate = { route -> navigateTo(route) },
                                        onSaveNotice = { viewModel.saveNotice(it) },
                                        onDeleteNotice = { viewModel.deleteNotice(it) }
                                    )

                                    AppRoute.STUDENTS -> StudentManagementScreen(
                                        state = state,
                                        strings = strings,
                                        selectedClassFilter = selectedClassFilter,
                                        openAddImmediately = openAddStudentPrompt,
                                        onClassFilterChange = { selectedClassFilter = it },
                                        onSelectStudent = { stu ->
                                            selectedStudentId = stu.id
                                            navigateTo(AppRoute.STUDENT_PROFILE)
                                        },
                                        onSaveStudent = {
                                            openAddStudentPrompt = false
                                            viewModel.saveStudent(it)
                                        },
                                        onDeleteStudent = { viewModel.deleteStudent(it) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.STUDENT_PROFILE -> {
                                        val stu = state.students.firstOrNull { it.id == selectedStudentId }
                                        if (stu == null) {
                                            navigateTo(AppRoute.STUDENTS)
                                        } else {
                                            StudentProfileScreen(
                                                student = stu,
                                                state = state,
                                                onEditStudent = { viewModel.saveStudent(it) },
                                                onDeleteStudent = { viewModel.deleteStudent(it) },
                                                onSaveTest = { viewModel.saveTestRecord(it) },
                                                onBack = {
                                                    val dest = if (previousRoute == AppRoute.JNV) AppRoute.JNV else AppRoute.STUDENTS
                                                    navigateTo(dest)
                                                }
                                            )
                                        }
                                    }

                                    AppRoute.ATTENDANCE -> AttendanceScreen(
                                        state = state,
                                        strings = strings,
                                        initialClass = if (selectedClassFilter == "All") "Class 1" else selectedClassFilter,
                                        onSaveAttendance = { date, cls, recs -> viewModel.saveAttendance(date, cls, recs) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.TESTS -> TestExamScreen(
                                        state = state,
                                        strings = strings,
                                        initialClass = selectedClassFilter,
                                        onSaveTest = { viewModel.saveTestRecord(it) },
                                        onDeleteTest = { viewModel.deleteTestRecord(it) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.JNV -> JnvDashboardScreen(
                                        state = state,
                                        onSelectStudent = { stu ->
                                            selectedStudentId = stu.id
                                            previousRoute = AppRoute.JNV
                                            currentRoute = AppRoute.STUDENT_PROFILE
                                        },
                                        onSaveStudent = { viewModel.saveStudent(it) },
                                        onSaveTest = { viewModel.saveTestRecord(it) },
                                        onOpenJnvAttendance = {
                                            selectedClassFilter = "JNV"
                                            navigateTo(AppRoute.ATTENDANCE, classFilter = "JNV")
                                        },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.REPORTS_OVERVIEW -> ReportsOverviewScreen(
                                        state = state,
                                        strings = strings,
                                        onOpenWeekly = { navigateTo(AppRoute.WEEKLY_REPORT) },
                                        onOpenMonthly = { navigateTo(AppRoute.MONTHLY_REPORT) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.WEEKLY_REPORT -> WeeklyReportScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.MONTHLY_REPORT -> MonthlyReportScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.BOOKS -> BooksScreen(
                                        state = state,
                                        strings = strings,
                                        onSaveBook = { viewModel.saveBook(it) },
                                        onDeleteBook = { viewModel.deleteBook(it) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.STATIONERY -> StationeryScreen(
                                        state = state,
                                        strings = strings,
                                        onSaveStationery = { viewModel.saveStationery(it) },
                                        onDeleteStationery = { viewModel.deleteStationery(it) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.SETTINGS -> SettingsScreen(
                                        state = state,
                                        strings = strings,
                                        onSetLanguage = { viewModel.setLanguage(it) },
                                        onSetRole = { viewModel.setActiveRole(it) },
                                        onUpdateSchool = { name, loc, yr -> viewModel.updateSchoolSettings(name, loc, yr) },
                                        onSaveMember = { viewModel.saveMember(it) },
                                        onDeleteMember = { viewModel.deleteMember(it) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.BACKUP_RESTORE -> BackupRestoreScreen(
                                        state = state,
                                        strings = strings,
                                        buildBackupJson = { viewModel.buildBackupJson() },
                                        onRestoreJson = { viewModel.restoreFromJson(it) },
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.HELP -> HelpAndAboutScreen(
                                        isAbout = false,
                                        strings = strings,
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )

                                    AppRoute.ABOUT -> HelpAndAboutScreen(
                                        isAbout = true,
                                        strings = strings,
                                        onBack = { navigateTo(AppRoute.DASHBOARD) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isWideScreen) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(drawerContainerColor = Color.White) {
                            drawerContent()
                        }
                    },
                    content = mainBody
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(drawerContainerColor = Color.White) {
                            drawerContent()
                        }
                    },
                    content = mainBody
                )
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("${strings.notifications} & Notices") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.notices.isEmpty() && state.activities.isEmpty()) {
                        Text("No notifications or recent notices yet.")
                    } else {
                        state.notices.take(4).forEach { n ->
                            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                    Text("${n.priority}: ${n.title}", fontWeight = FontWeight.Bold)
                                    Text(n.message, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        state.activities.take(3).forEach { a ->
                            Text("• ${a.title} (${a.dateTimeLabel})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Profile & Role Access") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Signed in as: $userName", fontWeight = FontWeight.Bold)
                    if (userEmail.isNotBlank()) Text("Email: $userEmail", style = MaterialTheme.typography.bodyMedium)
                    Text("Society: ${state.profile.schoolName}, ${state.profile.branchLocation}")
                    Spacer(Modifier.height(4.dp))
                    Text("Switch Role View:", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Admin", "Staff", "Parent").forEach { role ->
                            TextButton(onClick = { viewModel.setActiveRole(role) }) {
                                Text(
                                    text = if (state.activeRole == role) "✓ $role" else role,
                                    fontWeight = if (state.activeRole == role) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Done") }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(strings.logout) },
            text = { Text("Sign out of Parivar Education Management System?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    signOutUser(credentialManager, scope)
                }) {
                    Text(strings.logout, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
