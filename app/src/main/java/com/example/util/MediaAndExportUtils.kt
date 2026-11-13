package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayIso(): String = isoFormat.format(Date())

    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1..12

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun currentWeekRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = isoFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val end = isoFormat.format(cal.time)
        return start to end
    }

    fun shiftWeek(startIso: String, deltaWeeks: Int): Pair<String, String> {
        return try {
            val date = isoFormat.parse(startIso) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, deltaWeeks * 7)
            }
            val newStart = isoFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 6)
            val newEnd = isoFormat.format(cal.time)
            newStart to newEnd
        } catch (_: Exception) {
            currentWeekRange()
        }
    }

    fun isDateInRange(dateIso: String, startIso: String, endIso: String): Boolean {
        if (dateIso.isBlank()) return false
        return dateIso >= startIso && dateIso <= endIso
    }

    fun isDateInMonth(dateIso: String, month: Int, year: Int): Boolean {
        if (dateIso.length < 7) return false
        val prefix = String.format(Locale.US, "%04d-%02d", year, month)
        return dateIso.startsWith(prefix)
    }

    fun monthName(month: Int): String {
        val names = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return names.getOrElse((month - 1).coerceIn(0, 11)) { "January" }
    }
}

object MediaCodecUtils {
    fun uriToCompressedBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val original = BitmapFactory.decodeStream(input) ?: return null
                val maxDim = 240
                val scale = minOf(
                    maxDim.toFloat() / original.width.coerceAtLeast(1),
                    maxDim.toFloat() / original.height.coerceAtLeast(1),
                    1f
                )
                val targetW = (original.width * scale).toInt().coerceAtLeast(1)
                val targetH = (original.height * scale).toInt().coerceAtLeast(1)
                val resized = Bitmap.createScaledBitmap(original, targetW, targetH, true)
                val baos = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 72, baos)
                if (resized !== original) original.recycle()
                Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            bmp.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

object ReportExportUtils {
    fun buildCsvContent(
        title: String,
        subtitle: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM for Microsoft Excel Hindi/Unicode compatibility
        sb.appendLine("\"${escapeCsv(title)}\"")
        sb.appendLine("\"${escapeCsv(subtitle)}\"")
        summaryLines.forEach { sb.appendLine("\"${escapeCsv(it)}\"") }
        sb.appendLine()
        sb.appendLine(headers.joinToString(",") { "\"${escapeCsv(it)}\"" })
        rows.forEach { row ->
            sb.appendLine(row.joinToString(",") { "\"${escapeCsv(it)}\"" })
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String = value.replace("\"", "\"\"")

    fun writePdfReport(
        outputStream: OutputStream,
        title: String,
        subtitle: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>,
        accentColor: Int = Color.rgb(13, 43, 82)
    ) {
        val pdf = createPdfDocument(title, subtitle, summaryLines, headers, rows, accentColor)
        try {
            pdf.writeTo(outputStream)
        } finally {
            pdf.close()
        }
    }

    fun printReport(
        context: Context,
        jobName: String,
        title: String,
        subtitle: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>,
        accentColor: Int = Color.rgb(13, 43, 82)
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdf = createPdfDocument(title, subtitle, summaryLines, headers, rows, accentColor)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdf.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun createPdfDocument(
        title: String,
        subtitle: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>,
        accentColor: Int
    ): PdfDocument {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val pdf = PdfDocument()

        val headerPaint = Paint().apply { color = accentColor }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            isAntiAlias = true
        }
        val tableHeaderBg = Paint().apply { color = Color.rgb(226, 232, 240) }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas: Canvas = page.canvas

        fun drawHeaderBar(c: Canvas) {
            c.drawRoundRect(
                RectF(margin, 28f, pageWidth - margin, 94f),
                12f,
                12f,
                headerPaint
            )
            c.drawText("PARIVAR EDUCATION SOCIETY • CHIKALPAT", margin + 16f, 52f, subtitlePaint)
            c.drawText(title, margin + 16f, 76f, titlePaint)
        }

        drawHeaderBar(canvas)
        var y = 116f
        canvas.drawText(subtitle, margin, y, sectionPaint)
        y += 18f

        summaryLines.forEach { line ->
            canvas.drawText("• $line", margin + 4f, y, bodyPaint)
            y += 15f
        }

        y += 12f
        val usableWidth = pageWidth - margin * 2
        val colCount = headers.size.coerceAtLeast(1)
        val colWidth = usableWidth / colCount

        fun drawTableHeader(c: Canvas, topY: Float): Float {
            c.drawRect(margin, topY, pageWidth - margin, topY + 22f, tableHeaderBg)
            headers.forEachIndexed { idx, h ->
                c.drawText(h.take(18), margin + idx * colWidth + 4f, topY + 15f, sectionPaint)
            }
            return topY + 24f
        }

        y = drawTableHeader(canvas, y)
        rows.forEach { row ->
            if (y > pageHeight - 50f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                drawHeaderBar(canvas)
                y = drawTableHeader(canvas, 114f)
            }
            row.forEachIndexed { idx, cell ->
                canvas.drawText(cell.take(20), margin + idx * colWidth + 4f, y + 13f, bodyPaint)
            }
            canvas.drawLine(margin, y + 18f, pageWidth - margin, y + 18f, borderPaint)
            y += 20f
        }

        pdf.finishPage(page)
        return pdf
    }
}
