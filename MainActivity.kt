package com.rpdryfish.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val BORA_COST = 200.0
private const val PREFS = "rp_dry_fish_data"
private const val BILLS_KEY = "bills"

data class Bill(
    val no: Int, val customer: String, val kg: Double, val rate: Double,
    val bora: Int, val labour: Double, val paid: Double, val date: String,
    val period: String
) {
    val fishValue get() = kg / 40.0 * rate
    val total get() = fishValue + bora * BORA_COST + labour
    val due get() = total - paid
}

class VM(private val context: Context) : ViewModel() {
    var bills by mutableStateOf(load(context)); private set

    fun add(customer: String, kg: Double, rate: Double, bora: Int, labour: Double, paid: Double, period: String): Bill {
        val bill = Bill(
            (bills.maxOfOrNull { it.no } ?: 0) + 1, customer, kg, rate, bora, labour, paid,
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()), period
        )
        bills = bills + bill
        save(context, bills)
        return bill
    }

    fun exportJson(): String = billsToJson(bills).toString(2)
    fun importJson(raw: String) { try { bills = jsonToBills(raw); save(context, bills) } catch (_: Exception) {} }

    private fun save(c: Context, list: List<Bill>) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString(BILLS_KEY, billsToJson(list).toString()).apply()

    companion object {
        private fun billsToJson(list: List<Bill>) = JSONArray().apply {
            list.forEach { b -> put(JSONObject().apply {
                put("no", b.no); put("customer", b.customer); put("kg", b.kg); put("rate", b.rate)
                put("bora", b.bora); put("labour", b.labour); put("paid", b.paid); put("date", b.date); put("period", b.period)
            }) }
        }
        private fun jsonToBills(raw: String): List<Bill> {
            val arr = JSONArray(raw)
            return (0 until arr.length()).map { i -> arr.getJSONObject(i).let { o ->
                Bill(o.getInt("no"), o.getString("customer"), o.getDouble("kg"), o.getDouble("rate"),
                    o.getInt("bora"), o.getDouble("labour"), o.getDouble("paid"), o.getString("date"), o.optString("period"))
            } }
        }
        fun load(c: Context): List<Bill> = try {
            val raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(BILLS_KEY, "[]") ?: "[]"
            jsonToBills(raw)
        } catch (_: Exception) { emptyList() }
    }
}

val Red = Color(0xFF8B1E1E)
val Cream = Color(0xFFFFF8F0)
val Gold = Color(0xFFC9972B)

@Composable
fun App() {
    val context = LocalContext.current
    val vm: VM = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = VM(context) as T
    })
    var page by remember { mutableStateOf(0) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("RP DRY FISH", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Red, contentColor = Color.White))
    }, bottomBar = {
        NavigationBar { listOf("Home", "Bill", "Customer", "Stock", "Reports").forEachIndexed { i, s ->
            NavigationBarItem(selected = page == i, onClick = { page = i }, icon = { Text(s.first().toString()) }, label = { Text(s) })
        } }
    }) { padding -> Box(Modifier.padding(padding).fillMaxSize().background(Cream)) {
        when (page) {
            0 -> Home(vm) { page = 1 }
            1 -> BillPage(vm) { page = 0 }
            2 -> Customers(vm)
            3 -> Stock(vm)
            4 -> Reports(vm)
        }
    } }
}

@Composable fun Home(vm: VM, newBill: () -> Unit) {
    val context = LocalContext.current
    var backupText by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Red)) { Column(Modifier.padding(20.dp)) {
            Text("RP", color = Gold, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("DRY FISH", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("ASSAM HERITAGE", color = Color.White)
        } }
        Button(newBill, Modifier.fillMaxWidth()) { Text("＋ NEW DRY FISH BILL") }
        Text("Bills: ${vm.bills.size}", fontWeight = FontWeight.Bold)
        Text("Total Sales: ₹${money(vm.bills.sumOf { it.total })}")
        Text("Received: ₹${money(vm.bills.sumOf { it.paid })}")
        Text("Remaining: ₹${money(vm.bills.sumOf { it.due })}")
        OutlinedButton(onClick = {
            val file = File(context.cacheDir, "RP_Dry_Fish_Backup_${System.currentTimeMillis()}.json")
            file.writeText(vm.exportJson())
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Backup share करें"))
        }, Modifier.fillMaxWidth()) { Text("BACKUP / SHARE DATA") }
        backupText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable fun BillPage(vm: VM, done: () -> Unit) {
    val context = LocalContext.current
    var c by remember { mutableStateOf("") }; var k by remember { mutableStateOf("") }
    var r by remember { mutableStateOf("") }; var b by remember { mutableStateOf("0") }
    var l by remember { mutableStateOf("0") }; var p by remember { mutableStateOf("0") }
    var period by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<Bill?>(null) }
    val kg = k.toDoubleOrNull() ?: 0.0; val rate = r.toDoubleOrNull() ?: 0.0
    val bora = b.toIntOrNull() ?: 0; val labour = l.toDoubleOrNull() ?: 0.0; val paid = p.toDoubleOrNull() ?: 0.0
    val total = kg / 40.0 * rate + bora * BORA_COST + labour
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("New Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Red)
        field("Customer Name", c) { c = it }; field("Weight (KG)", k) { k = it }; field("Rate per 40 KG (₹)", r) { r = it }
        field("Total Bora", b) { b = it }; field("Labour (₹)", l) { l = it }; field("Paid (₹)", p) { p = it }; field("Purchase Period", period) { period = it }
        Text("Dry Fish", fontWeight = FontWeight.Bold, color = Red)
        Card { Column(Modifier.padding(14.dp)) {
            Text("Total Bora: $bora"); Text("Labour: ₹${money(labour)}"); Text("TOTAL ₹${money(total)}", fontWeight = FontWeight.Bold); Text("REMAINING ₹${money(total - paid)}")
        } }
        Button(enabled = c.isNotBlank() && kg > 0 && rate > 0 && paid >= 0, onClick = {
            val bill = vm.add(c.trim(), kg, rate, bora, labour, paid, period.trim()); saved = bill
        }, modifier = Modifier.fillMaxWidth()) { Text("SAVE BILL") }
        saved?.let { bill ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton({ sharePdf(context, bill) }, Modifier.weight(1f)) { Text("PDF") }
                OutlinedButton({ sharePdf(context, bill, true) }, Modifier.weight(1f)) { Text("WHATSAPP") }
            }
            Text("Bill #${bill.no} saved successfully", color = Red, fontWeight = FontWeight.Bold)
            OutlinedButton(done, Modifier.fillMaxWidth()) { Text("DONE") }
        }
    }
}

@Composable fun field(label: String, value: String, onValue: (String) -> Unit) = OutlinedTextField(value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

@Composable fun Customers(vm: VM) {
    val customers = vm.bills.groupBy { it.customer }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Customers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Red)
        if (customers.isEmpty()) Text("No customers yet")
        customers.forEach { (name, list) -> Card { Column(Modifier.padding(12.dp)) { Text(name, fontWeight = FontWeight.Bold); Text("Bills: ${list.size}"); Text("Due: ₹${money(list.sumOf { it.due })}") } } }
    }
}

@Composable fun Stock(vm: VM) {
    val kg = vm.bills.sumOf { it.kg }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Red)
        Card { Column(Modifier.padding(16.dp)) { Text("Dry Fish Purchased", fontWeight = FontWeight.Bold); Text("${money(kg)} KG") } }
        Text("Current stock is based on recorded purchase bills.")
    }
}

@Composable fun Reports(vm: VM) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Red)
        Text("Bills: ${vm.bills.size}"); Text("Total Sales ₹${money(vm.bills.sumOf { it.total })}")
        Text("Received ₹${money(vm.bills.sumOf { it.paid })}"); Text("Remaining ₹${money(vm.bills.sumOf { it.due })}")
        Text("Total Bora ${vm.bills.sumOf { it.bora }}"); Text("Total Dry Fish ${money(vm.bills.sumOf { it.kg })} KG")
        vm.bills.maxByOrNull { it.no }?.let { Text("Latest bill #${it.no} — ${it.date}") }
    }
}

private fun money(v: Double) = String.format(Locale.getDefault(), "%.2f", v)

private fun createBillPdf(context: Context, b: Bill): File {
    val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); val c = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(139,30,30) }
    val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
    paint.textSize = 28f; paint.isFakeBoldText = true; c.drawText("RP DRY FISH", 40f, 55f, paint)
    black.textSize = 15f; black.isFakeBoldText = true; c.drawText("DRY FISH", 40f, 90f, black)
    black.isFakeBoldText = false; c.drawText("Bill No: ${b.no}", 400f, 55f, black); c.drawText("Date: ${b.date}", 400f, 78f, black)
    c.drawLine(40f, 105f, 555f, 105f, black)
    black.textSize = 17f; black.isFakeBoldText = true; c.drawText("Customer", 40f, 140f, black); black.isFakeBoldText = false; c.drawText(b.customer, 145f, 140f, black)
    var y = 190f; black.isFakeBoldText = true; c.drawText("Dry Fish", 40f, y, black); c.drawText("Weight", 230f, y, black); c.drawText("Rate / 40 KG", 340f, y, black); c.drawText("Amount", 470f, y, black)
    y += 30f; black.isFakeBoldText = false; c.drawText("Dry Fish", 40f, y, black); c.drawText("${money(b.kg)} KG", 230f, y, black); c.drawText("₹${money(b.rate)}", 340f, y, black); c.drawText("₹${money(b.fishValue)}", 470f, y, black)
    y += 55f; c.drawText("Total Bora: ${b.bora}", 40f, y, black); c.drawText("Bora Cost: ₹${money(b.bora * BORA_COST)}", 300f, y, black)
    y += 30f; c.drawText("Labour: ₹${money(b.labour)}", 40f, y, black)
    y += 45f; black.isFakeBoldText = true; black.textSize = 20f; c.drawText("TOTAL: ₹${money(b.total)}", 40f, y, black); c.drawText("PAID: ₹${money(b.paid)}", 40f, y + 35f, black); c.drawText("DUE: ₹${money(b.due)}", 320f, y + 35f, black)
    if (b.period.isNotBlank()) { black.textSize = 14f; black.isFakeBoldText = false; c.drawText("Purchase Period: ${b.period}", 40f, y + 75f, black) }
    c.drawLine(40f, 760f, 555f, 760f, black); black.textSize = 12f; c.drawText("RP Dry Fish", 40f, 790f, black)
    doc.finishPage(page)
    val file = File(context.cacheDir, "RP_Dry_Fish_Bill_${b.no}.pdf"); file.outputStream().use { doc.writeTo(it) }; doc.close(); return file
}

private fun sharePdf(context: Context, bill: Bill, whatsapp: Boolean = false) {
    val file = createBillPdf(context, bill); val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, "RP Dry Fish Bill #${bill.no}"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    if (whatsapp) intent.setPackage("com.whatsapp")
    try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent.createChooser(intent, "Bill share करें")) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) { super.onCreate(b); setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Red, secondary = Gold, background = Cream)) { App() } } }
}
