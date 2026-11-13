package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryDark
import com.example.ui.theme.SlateBorder
import com.example.util.MediaCodecUtils
import java.util.Calendar
import java.util.Locale

enum class AppRoute {
    DASHBOARD,
    STUDENTS,
    STUDENT_PROFILE,
    ATTENDANCE,
    TESTS,
    JNV,
    REPORTS_OVERVIEW,
    WEEKLY_REPORT,
    MONTHLY_REPORT,
    BOOKS,
    STATIONERY,
    SETTINGS,
    BACKUP_RESTORE,
    HELP,
    ABOUT
}

@Composable
fun DarkBlueHeaderBar(
    strings: LocalizedStrings,
    language: AppLanguage,
    activeRole: String,
    notificationCount: Int,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        color = NavyPrimary,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(NavyPrimaryDark, NavyPrimary, Color(0xFF174580))
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("header_menu_button")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Sidebar Menu", tint = Color.White)
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.School, contentDescription = "Society Logo", tint = AmberGold)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${strings.societyName} • ${strings.locationName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier.testTag("header_home_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = strings.home, tint = Color.White)
                }

                IconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("header_language_button")
                ) {
                    Icon(Icons.Default.Language, contentDescription = "Switch Language", tint = AmberGold)
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("header_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                Badge { Text(notificationCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = strings.notifications, tint = Color.White)
                    }
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.testTag("header_profile_button")
                ) {
                    Icon(Icons.Default.Person, contentDescription = strings.adminProfile, tint = Color.White)
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("header_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = Color.White)
                }

                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.testTag("header_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout, tint = Color(0xFFFCA5A5))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmberGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${strings.roleLabel}: $activeRole",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = if (language == AppLanguage.ENGLISH) "EN / हिन्दी" else "हिन्दी / EN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF93C5FD),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private data class SidebarMenuItem(
    val route: AppRoute?,
    val label: String,
    val icon: ImageVector,
    val isLogout: Boolean = false
)

@Composable
fun SidebarDrawerContent(
    currentRoute: AppRoute,
    strings: LocalizedStrings,
    userName: String,
    userEmail: String,
    activeRole: String,
    onSelectRoute: (AppRoute) -> Unit,
    onLogout: () -> Unit
) {
    val items = listOf(
        SidebarMenuItem(AppRoute.DASHBOARD, strings.dashboard, Icons.Default.Dashboard),
        SidebarMenuItem(AppRoute.STUDENTS, strings.studentManagement, Icons.Default.Groups),
        SidebarMenuItem(AppRoute.ATTENDANCE, strings.attendance, Icons.Default.Checklist),
        SidebarMenuItem(AppRoute.TESTS, strings.testAndExam, Icons.Default.Quiz),
        SidebarMenuItem(AppRoute.JNV, strings.jnvDashboard, Icons.Default.EmojiEvents),
        SidebarMenuItem(AppRoute.REPORTS_OVERVIEW, strings.reports, Icons.Default.Assessment),
        SidebarMenuItem(AppRoute.WEEKLY_REPORT, strings.weeklyReport, Icons.Default.DateRange),
        SidebarMenuItem(AppRoute.MONTHLY_REPORT, strings.monthlyReport, Icons.Default.CalendarMonth),
        SidebarMenuItem(AppRoute.BOOKS, strings.books, Icons.AutoMirrored.Filled.MenuBook),
        SidebarMenuItem(AppRoute.STATIONERY, strings.stationery, Icons.Default.Toys),
        SidebarMenuItem(AppRoute.SETTINGS, strings.settings, Icons.Default.Settings),
        SidebarMenuItem(AppRoute.BACKUP_RESTORE, strings.backupRestore, Icons.Default.Backup),
        SidebarMenuItem(AppRoute.HELP, strings.help, Icons.AutoMirrored.Filled.HelpOutline),
        SidebarMenuItem(AppRoute.ABOUT, strings.aboutUs, Icons.Default.Info),
        SidebarMenuItem(null, strings.logout, Icons.AutoMirrored.Filled.Logout, isLogout = true)
    )

    Column(
        modifier = Modifier
            .width(290.dp)
            .background(Color.White)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(NavyPrimaryDark, NavyPrimary)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AmberGold,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userName.take(1).uppercase().ifBlank { "P" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimaryDark
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.societyName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${strings.locationName} • $activeRole",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
                if (userEmail.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        items.forEach { item ->
            if (item.isLogout) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)
            }
            val selected = item.route != null && currentRoute == item.route
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = selected,
                onClick = {
                    if (item.isLogout) onLogout() else item.route?.let(onSelectRoute)
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (item.isLogout) Color(0xFFDC2626) else if (selected) NavyPrimary else Color(0xFF475569)
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = NavyPrimary.copy(alpha = 0.1f),
                    selectedTextColor = NavyPrimary,
                    unselectedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("sidebar_${item.label.lowercase().replace(" ", "_")}")
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun StudentAvatar(
    name: String,
    photoBase64: String,
    accentColor: Color = NavyPrimary,
    size: Dp = 48.dp
) {
    val bitmap = remember(photoBase64) {
        MediaCodecUtils.decodeBase64ToImageBitmap(photoBase64)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "$name photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f),
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.trim().take(1).uppercase().ifBlank { "S" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun DateFieldWithPicker(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = onDateSelected,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = {
                val cal = Calendar.getInstance()
                val parts = value.split("-")
                val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                val m = (parts.getOrNull(1)?.toIntOrNull()?.minus(1)) ?: cal.get(Calendar.MONTH)
                val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                DatePickerDialog(context, { _, year, month, day ->
                    onDateSelected(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                }, y, m, d).show()
            }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick $label")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = NavyPrimary.copy(alpha = 0.08f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = NavyPrimary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun BarChartCanvas(
    entries: List<Pair<String, Float>>,
    barColors: List<Color>,
    maxVal: Float? = null,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
) {
    if (entries.isEmpty()) return
    val resolvedMax = (maxVal ?: entries.maxOf { it.second }).coerceAtLeast(1f)
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val count = entries.size
            val slotW = size.width / count
            val barW = (slotW * 0.56f).coerceAtMost(42.dp.toPx())

            // Baseline
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )

            entries.forEachIndexed { idx, (_, value) ->
                val ratio = (value / resolvedMax).coerceIn(0f, 1f)
                val barH = ratio * (size.height - 12f)
                val left = idx * slotW + (slotW - barW) / 2f
                val top = size.height - barH
                val color = barColors[idx % barColors.size]
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barW, barH.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            entries.forEach { (label, value) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.0f", value),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun LineProgressCanvas(
    points: List<Pair<String, Float>>,
    lineColor: Color,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
) {
    if (points.isEmpty()) return
    val maxVal = 100f
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width / 2f
            val path = Path()
            val coords = points.mapIndexed { idx, (_, v) ->
                val x = if (points.size == 1) size.width / 2f else idx * stepX
                val y = size.height - (v.coerceIn(0f, maxVal) / maxVal) * size.height
                Offset(x, y)
            }
            coords.forEachIndexed { i, pt ->
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 6f))
            coords.forEach { pt ->
                drawCircle(color = Color.White, radius = 9f, center = pt)
                drawCircle(color = lineColor, radius = 6f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { (label, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${v.toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = lineColor)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                }
            }
        }
    }
}
