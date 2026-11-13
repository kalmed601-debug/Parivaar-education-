package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Toys
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BookItem
import com.example.data.model.SchoolConstants
import com.example.data.model.StationeryItem
import com.example.ui.components.DateFieldWithPicker
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.AbsentCrimson
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MonthlyPurple
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WeeklyGreen
import com.example.ui.viewmodel.SchoolUiState
import com.example.util.DateUtils
import com.example.util.MediaCodecUtils

@Composable
fun BooksScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onSaveBook: (BookItem) -> Unit,
    onDeleteBook: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("All") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BookItem?>(null) }

    val canEdit = state.activeRole != "Parent"
    val canDelete = state.activeRole == "Admin"

    val filtered = remember(state.books, searchQuery, classFilter) {
        state.books.filter { b ->
            val clsOk = classFilter == "All" || b.className == classFilter
            val q = searchQuery.trim().lowercase()
            val qOk = q.isEmpty() || b.bookName.lowercase().contains(q) ||
                b.subject.lowercase().contains(q) || b.author.lowercase().contains(q)
            clsOk && qOk
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = { editing = null; showForm = true },
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_book_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Book")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.allBooksTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(
                        text = "Total Qty: ${state.summary.totalBooksStock} • Remaining: ${state.summary.totalBooksAvailable}",
                        style = MaterialTheme.typography.labelMedium,
                        color = WeeklyGreen
                    )
                }
                if (canEdit) {
                    Button(onClick = { editing = null; showForm = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Book", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Book")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search book, subject, author...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Books") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All").plus(SchoolConstants.ALL_CLASSES).forEach { cls ->
                    FilterChip(selected = classFilter == cls, onClick = { classFilter = cls }, label = { Text(cls) })
                }
            }
            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "No Books in Inventory",
                    subtitle = "Add class textbooks and library books to track stock and distribution.",
                    actionLabel = if (canEdit) "Add Book" else null,
                    onAction = if (canEdit) { { editing = null; showForm = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 84.dp)) {
                    items(filtered, key = { it.id }) { book ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ItemThumbnail(book.photoBase64, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF0284C7))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.bookName, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    Text("${book.className} • ${book.subject}", style = MaterialTheme.typography.bodyMedium, color = IndigoAccent)
                                    if (book.author.isNotBlank() || book.publisher.isNotBlank()) {
                                        Text("${book.author} ${book.publisher}".trim(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StockBadge("Qty: ${book.quantity}", NavyPrimary)
                                        StockBadge("Dist: ${book.distributed}", Color(0xFFD97706))
                                        StockBadge("Rem: ${book.availableStock}", WeeklyGreen)
                                    }
                                }
                                if (canEdit) {
                                    Column {
                                        IconButton(onClick = { editing = book; showForm = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Book", tint = IndigoAccent)
                                        }
                                        if (book.availableStock > 0) {
                                            TextButton(onClick = {
                                                onSaveBook(book.copy(distributed = book.distributed + 1))
                                            }) {
                                                Text("+1 Dist", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteBook(book.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Book", tint = AbsentCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        BookFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { onSaveBook(it); showForm = false }
        )
    }
}

@Composable
fun StationeryScreen(
    state: SchoolUiState,
    strings: LocalizedStrings,
    onSaveStationery: (StationeryItem) -> Unit,
    onDeleteStationery: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var catFilter by remember { mutableStateOf("All") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StationeryItem?>(null) }
    var historyItem by remember { mutableStateOf<StationeryItem?>(null) }

    val canEdit = state.activeRole != "Parent"
    val canDelete = state.activeRole == "Admin"

    val filtered = remember(state.stationery, searchQuery, catFilter) {
        state.stationery.filter { item ->
            val catOk = catFilter == "All" || item.category == catFilter
            val q = searchQuery.trim().lowercase()
            val qOk = q.isEmpty() || item.itemName.lowercase().contains(q) ||
                item.category.lowercase().contains(q) || item.details.lowercase().contains(q)
            catOk && qOk
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = { editing = null; showForm = true },
                    containerColor = MonthlyPurple,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_stationery_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Stationery")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.allStationeryTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(
                        text = "Total Qty: ${state.summary.totalStationeryStock} • Available: ${state.summary.totalStationeryAvailable}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MonthlyPurple
                    )
                }
                if (canEdit) {
                    Button(onClick = { editing = null; showForm = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search stationery, toys, teaching aids...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Stationery") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All").plus(SchoolConstants.STATIONERY_CATEGORIES).forEach { cat ->
                    FilterChip(selected = catFilter == cat, onClick = { catFilter = cat }, label = { Text(cat) })
                }
            }
            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Toys,
                    title = "No Stationery or Toys Yet",
                    subtitle = "Track notebooks, pens, geometry sets, educational toys, and stock history.",
                    actionLabel = if (canEdit) "Add Stationery Item" else null,
                    onAction = if (canEdit) { { editing = null; showForm = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 84.dp)) {
                    items(filtered, key = { it.id }) { item ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, SlateBorder)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ItemThumbnail(item.photoBase64, Icons.Default.Toys, MonthlyPurple)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemName, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    Text("${item.category} • ₹${item.purchasePrice.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MonthlyPurple)
                                    if (item.details.isNotBlank()) {
                                        Text(item.details, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StockBadge("Qty: ${item.quantity}", NavyPrimary)
                                        StockBadge("Dist: ${item.distributed}", Color(0xFFD97706))
                                        StockBadge("Avail: ${item.availableStock}", WeeklyGreen)
                                    }
                                    if (item.stockHistorySummary.isNotBlank()) {
                                        TextButton(
                                            onClick = { historyItem = item },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("View Stock History", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                if (canEdit) {
                                    Column {
                                        IconButton(onClick = { editing = item; showForm = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = IndigoAccent)
                                        }
                                        if (item.availableStock > 0) {
                                            TextButton(onClick = {
                                                onSaveStationery(item.copy(distributed = item.distributed + 1))
                                            }) {
                                                Text("+1 Dist", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteStationery(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = AbsentCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        StationeryFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { onSaveStationery(it); showForm = false }
        )
    }

    if (historyItem != null) {
        AlertDialog(
            onDismissRequest = { historyItem = null },
            title = { Text("${historyItem!!.itemName} • Stock History") },
            text = { Text(historyItem!!.stockHistorySummary.ifBlank { "No history logged yet." }) },
            confirmButton = {
                TextButton(onClick = { historyItem = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ItemThumbnail(photoBase64: String, fallbackIcon: ImageVector, accent: Color) {
    val bmp = remember(photoBase64) { MediaCodecUtils.decodeBase64ToImageBitmap(photoBase64) }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = "Item photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
        )
    } else {
        Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.12f), modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(fallbackIcon, contentDescription = "Item icon", tint = accent, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun StockBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookFormDialog(
    initial: BookItem?,
    onDismiss: () -> Unit,
    onSave: (BookItem) -> Unit
) {
    val context = LocalContext.current
    var bookName by remember { mutableStateOf(initial?.bookName ?: "") }
    var className by remember { mutableStateOf(initial?.className ?: "Class 1") }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var author by remember { mutableStateOf(initial?.author ?: "") }
    var publisher by remember { mutableStateOf(initial?.publisher ?: "") }
    var quantityStr by remember { mutableStateOf(initial?.quantity?.toString() ?: "10") }
    var distributedStr by remember { mutableStateOf(initial?.distributed?.toString() ?: "0") }
    var purchaseDate by remember { mutableStateOf(initial?.purchaseDate ?: DateUtils.todayIso()) }
    var photoBase64 by remember { mutableStateOf(initial?.photoBase64 ?: "") }
    var classExpanded by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) MediaCodecUtils.uriToCompressedBase64(context, uri)?.let { photoBase64 = it }
    }

    val qty = quantityStr.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    val dist = distributedStr.toLongOrNull()?.coerceIn(0L, qty) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Book" else "Edit Book") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ItemThumbnail(photoBase64, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF0284C7))
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Book Image", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Book Image")
                    }
                }
                OutlinedTextField(value = bookName, onValueChange = { bookName = it }, label = { Text("Book Name*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = classExpanded, onExpandedChange = { classExpanded = !classExpanded }) {
                    OutlinedTextField(
                        value = className, onValueChange = {}, readOnly = true, label = { Text("Class*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                        SchoolConstants.ALL_CLASSES.forEach { cls ->
                            DropdownMenuItem(text = { Text(cls) }, onClick = { className = cls; classExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("Publisher") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantityStr, onValueChange = { quantityStr = it.filter(Char::isDigit) }, label = { Text("Quantity*") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = distributedStr, onValueChange = { distributedStr = it.filter(Char::isDigit) }, label = { Text("Distributed") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Available / Remaining Stock: ${qty - dist}", fontWeight = FontWeight.Bold, color = WeeklyGreen)
                DateFieldWithPicker("Purchase Date", purchaseDate, { purchaseDate = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave((initial ?: BookItem()).copy(
                        bookName = bookName, className = className, subject = subject, author = author,
                        publisher = publisher, quantity = qty, distributed = dist, purchaseDate = purchaseDate, photoBase64 = photoBase64
                    ))
                },
                enabled = bookName.isNotBlank() && subject.isNotBlank()
            ) { Text("Save Book") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationeryFormDialog(
    initial: StationeryItem?,
    onDismiss: () -> Unit,
    onSave: (StationeryItem) -> Unit
) {
    val context = LocalContext.current
    var itemName by remember { mutableStateOf(initial?.itemName ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Notebook") }
    var details by remember { mutableStateOf(initial?.details ?: "") }
    var quantityStr by remember { mutableStateOf(initial?.quantity?.toString() ?: "10") }
    var distributedStr by remember { mutableStateOf(initial?.distributed?.toString() ?: "0") }
    var priceStr by remember { mutableStateOf(initial?.purchasePrice?.toInt()?.toString() ?: "0") }
    var purchaseDate by remember { mutableStateOf(initial?.purchaseDate ?: DateUtils.todayIso()) }
    var photoBase64 by remember { mutableStateOf(initial?.photoBase64 ?: "") }
    var catExpanded by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) MediaCodecUtils.uriToCompressedBase64(context, uri)?.let { photoBase64 = it }
    }

    val qty = quantityStr.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    val dist = distributedStr.toLongOrNull()?.coerceIn(0L, qty) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Stationery / Toy" else "Edit Stationery Item") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ItemThumbnail(photoBase64, Icons.Default.Toys, MonthlyPurple)
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Item Photo", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Item Photo")
                    }
                }
                OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true, label = { Text("Category*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        SchoolConstants.STATIONERY_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Item Details") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantityStr, onValueChange = { quantityStr = it.filter(Char::isDigit) }, label = { Text("Quantity*") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = distributedStr, onValueChange = { distributedStr = it.filter(Char::isDigit) }, label = { Text("Distributed") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Available Stock: ${qty - dist}", fontWeight = FontWeight.Bold, color = WeeklyGreen)
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Purchase Price (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DateFieldWithPicker("Purchase Date", purchaseDate, { purchaseDate = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave((initial ?: StationeryItem()).copy(
                        itemName = itemName, category = category, details = details, quantity = qty, distributed = dist,
                        purchasePrice = priceStr.toDoubleOrNull() ?: 0.0, purchaseDate = purchaseDate, photoBase64 = photoBase64
                    ))
                },
                enabled = itemName.isNotBlank()
            ) { Text("Save Item") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
