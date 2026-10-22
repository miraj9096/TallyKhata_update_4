package com.example.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility and business logic for exporting Customer Ledger data into CSV files.
 * Supports single-party account statements, all-parties summaries, and transaction journals.
 */
object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    /**
     * Escapes a value to comply with standard RFC 4180 CSV specifications.
     * Encloses values with quotes if they contain commas, newlines, or quotes.
     */
    fun escapeCsv(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        val containsSpecialChars = str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")
        return if (containsSpecialChars) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    /**
     * Generates a CSV ledger statement for a single customer.
     * Includes statement header, sorted transactions with running balance, and grand totals.
     */
    fun generateCustomerLedgerCsv(
        customer: Customer,
        transactions: List<Transaction>,
        profile: BusinessProfile? = null
    ): String {
        val sb = StringBuilder()

        // 1. Business & Statement Header
        val businessName = profile?.businessName?.takeIf { it.isNotBlank() } ?: "Tally Khata Store"
        val currency = profile?.currency?.takeIf { it.isNotBlank() } ?: "৳"

        sb.append(escapeCsv("LEDGER ACCOUNT STATEMENT")).append("\n")
        sb.append(escapeCsv("Business Name:")).append(",").append(escapeCsv(businessName)).append("\n")
        if (!profile?.ownerName.isNullOrBlank() || !profile?.phone.isNullOrBlank()) {
            val contact = listOfNotNull(profile?.ownerName?.takeIf { it.isNotBlank() }, profile?.phone?.takeIf { it.isNotBlank() }).joinToString(" - ")
            sb.append(escapeCsv("Merchant Contact:")).append(",").append(escapeCsv(contact)).append("\n")
        }
        sb.append(escapeCsv("Generated On:")).append(",").append(escapeCsv(displayDateFormat.format(Date()))).append("\n")
        sb.append("\n")

        // 2. Customer Details
        sb.append(escapeCsv("PARTY DETAILS")).append("\n")
        sb.append(escapeCsv("Customer / Party Name:")).append(",").append(escapeCsv(customer.name)).append("\n")
        sb.append(escapeCsv("Phone Number:")).append(",").append(escapeCsv(customer.phone)).append("\n")
        sb.append(escapeCsv("Address:")).append(",").append(escapeCsv(customer.address)).append("\n")
        sb.append(escapeCsv("Party Type:")).append(",").append(escapeCsv(customer.type)).append("\n")

        val statusDesc = when {
            customer.netBalance > 0 -> "YOU WILL GET (Debtor / Outstanding Due)"
            customer.netBalance < 0 -> "YOU WILL GIVE (Creditor / Advance Received)"
            else -> "SETTLED (Zero Balance)"
        }
        sb.append(escapeCsv("Balance Status:")).append(",").append(escapeCsv(statusDesc)).append("\n")
        sb.append(escapeCsv("Total Credit Given (Gave):")).append(",").append(escapeCsv(String.format(Locale.US, "%.2f", customer.totalGave))).append("\n")
        sb.append(escapeCsv("Total Payment Received (Got):")).append(",").append(escapeCsv(String.format(Locale.US, "%.2f", customer.totalGot))).append("\n")
        sb.append(escapeCsv("Net Current Balance:")).append(",").append(escapeCsv(String.format(Locale.US, "%.2f", customer.netBalance))).append("\n")
        sb.append("\n")

        // 3. Transactions Table Header
        sb.append(
            listOf(
                "Date & Time",
                "Transaction ID",
                "Bill / Invoice #",
                "Entry Type",
                "Credit (You Gave)",
                "Debit (You Got)",
                "Running Balance ($currency)",
                "Note / Remarks"
            ).joinToString(",") { escapeCsv(it) }
        ).append("\n")

        // 4. Sort transactions chronologically (oldest to newest) to calculate running balance
        val sortedTransactions = transactions.sortedBy { it.transactionDate.toDate().time }
        var runningBalance = 0.0
        var sumGave = 0.0
        var sumGot = 0.0

        for (tx in sortedTransactions) {
            val dateStr = try {
                dateFormat.format(tx.transactionDate.toDate())
            } catch (e: Exception) {
                tx.transactionDate.toDate().toString()
            }

            val isGave = tx.type == "GAVE"
            val gaveAmountStr: String
            val gotAmountStr: String

            if (isGave) {
                runningBalance += tx.amount
                sumGave += tx.amount
                gaveAmountStr = String.format(Locale.US, "%.2f", tx.amount)
                gotAmountStr = "0.00"
            } else {
                runningBalance -= tx.amount
                sumGot += tx.amount
                gaveAmountStr = "0.00"
                gotAmountStr = String.format(Locale.US, "%.2f", tx.amount)
            }

            val row = listOf(
                dateStr,
                tx.id,
                tx.billNumber.ifBlank { "-" },
                if (isGave) "YOU GAVE (Credit)" else "YOU GOT (Payment)",
                gaveAmountStr,
                gotAmountStr,
                String.format(Locale.US, "%.2f", runningBalance),
                tx.note
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        // 5. Totals Footer
        sb.append("\n")
        val totalsRow = listOf(
            "TOTALS / NET BALANCE",
            "",
            "",
            "${sortedTransactions.size} entries",
            String.format(Locale.US, "%.2f", sumGave),
            String.format(Locale.US, "%.2f", sumGot),
            String.format(Locale.US, "%.2f", runningBalance),
            statusDesc
        )
        sb.append(totalsRow.joinToString(",") { escapeCsv(it) }).append("\n")

        return sb.toString()
    }

    /**
     * Generates a CSV summary for all customers in the ledger.
     */
    fun generateAllCustomersSummaryCsv(
        customers: List<Customer>,
        profile: BusinessProfile? = null
    ): String {
        val sb = StringBuilder()
        val businessName = profile?.businessName?.takeIf { it.isNotBlank() } ?: "Tally Khata Store"

        sb.append(escapeCsv("CUSTOMER / PARTY DIRECTORY & SUMMARY")).append("\n")
        sb.append(escapeCsv("Business Name:")).append(",").append(escapeCsv(businessName)).append("\n")
        sb.append(escapeCsv("Export Date:")).append(",").append(escapeCsv(displayDateFormat.format(Date()))).append("\n")
        sb.append(escapeCsv("Total Parties Count:")).append(",").append(escapeCsv(customers.size)).append("\n")
        sb.append("\n")

        // Table Header
        val headers = listOf(
            "Party ID",
            "Name",
            "Phone",
            "Address",
            "Type",
            "Total Gave (Credit)",
            "Total Got (Payment)",
            "Net Balance",
            "Status",
            "Last Updated"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        var totalGaveAll = 0.0
        var totalGotAll = 0.0
        var totalNetAll = 0.0

        for (c in customers) {
            totalGaveAll += c.totalGave
            totalGotAll += c.totalGot
            totalNetAll += c.netBalance

            val status = when {
                c.netBalance > 0 -> "You'll Get"
                c.netBalance < 0 -> "You'll Give"
                else -> "Settled"
            }

            val updatedDate = c.updatedAt?.toDate()?.let { dateFormat.format(it) } ?: "-"

            val row = listOf(
                c.id,
                c.name,
                c.phone,
                c.address,
                c.type,
                String.format(Locale.US, "%.2f", c.totalGave),
                String.format(Locale.US, "%.2f", c.totalGot),
                String.format(Locale.US, "%.2f", c.netBalance),
                status,
                updatedDate
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        // Totals row
        sb.append("\n")
        val grandTotalRow = listOf(
            "GRAND TOTALS",
            "${customers.size} Parties",
            "",
            "",
            "",
            String.format(Locale.US, "%.2f", totalGaveAll),
            String.format(Locale.US, "%.2f", totalGotAll),
            String.format(Locale.US, "%.2f", totalNetAll),
            if (totalNetAll > 0) "Net Receivable" else if (totalNetAll < 0) "Net Payable" else "Balanced",
            ""
        )
        sb.append(grandTotalRow.joinToString(",") { escapeCsv(it) }).append("\n")

        return sb.toString()
    }

    /**
     * Generates a complete transaction journal CSV across all customers.
     */
    fun generateAllTransactionsJournalCsv(
        transactions: List<Transaction>,
        customers: List<Customer> = emptyList(),
        profile: BusinessProfile? = null
    ): String {
        val sb = StringBuilder()
        val businessName = profile?.businessName?.takeIf { it.isNotBlank() } ?: "Tally Khata Store"
        val customerMap = customers.associateBy { it.id }

        sb.append(escapeCsv("TRANSACTION JOURNAL & LEDGER BACKUP")).append("\n")
        sb.append(escapeCsv("Business Name:")).append(",").append(escapeCsv(businessName)).append("\n")
        sb.append(escapeCsv("Export Date:")).append(",").append(escapeCsv(displayDateFormat.format(Date()))).append("\n")
        sb.append(escapeCsv("Total Transactions:")).append(",").append(escapeCsv(transactions.size)).append("\n")
        sb.append("\n")

        val headers = listOf(
            "Date & Time",
            "Transaction ID",
            "Customer ID",
            "Customer / Party Name",
            "Customer Phone",
            "Type",
            "Credit Amount (Gave)",
            "Debit Amount (Got)",
            "Bill / Invoice #",
            "Note / Remarks"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        var sumCredit = 0.0
        var sumDebit = 0.0

        val sorted = transactions.sortedByDescending { it.transactionDate.toDate().time }
        for (tx in sorted) {
            val dateStr = try {
                dateFormat.format(tx.transactionDate.toDate())
            } catch (e: Exception) {
                tx.transactionDate.toDate().toString()
            }

            val cust = customerMap[tx.customerId]
            val partyName = tx.customerName.ifBlank { cust?.name ?: "Unknown Party" }
            val partyPhone = cust?.phone ?: ""

            val isGave = tx.type == "GAVE"
            if (isGave) sumCredit += tx.amount else sumDebit += tx.amount

            val row = listOf(
                dateStr,
                tx.id,
                tx.customerId,
                partyName,
                partyPhone,
                if (isGave) "GAVE" else "GOT",
                if (isGave) String.format(Locale.US, "%.2f", tx.amount) else "0.00",
                if (!isGave) String.format(Locale.US, "%.2f", tx.amount) else "0.00",
                tx.billNumber.ifBlank { "-" },
                tx.note
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        sb.append("\n")
        val totalsRow = listOf(
            "TOTAL JOURNAL VOLUMES",
            "${transactions.size} records",
            "",
            "",
            "",
            "",
            String.format(Locale.US, "%.2f", sumCredit),
            String.format(Locale.US, "%.2f", sumDebit),
            "",
            "Net: ${String.format(Locale.US, "%.2f", sumCredit - sumDebit)}"
        )
        sb.append(totalsRow.joinToString(",") { escapeCsv(it) }).append("\n")

        return sb.toString()
    }

    /**
     * Writes CSV string content to a file in the app's backup directory.
     */
    suspend fun saveCsvToFile(
        context: Context,
        fileName: String,
        csvContent: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "tally_backups")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a customer ledger statement directly to a .csv file.
     */
    suspend fun exportCustomerLedgerToFile(
        context: Context,
        customer: Customer,
        transactions: List<Transaction>,
        profile: BusinessProfile? = null
    ): Result<File> {
        val sanitizedName = customer.name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(25)
        val timestamp = fileTimestampFormat.format(Date())
        val fileName = "ledger_${sanitizedName}_$timestamp.csv"
        val csv = generateCustomerLedgerCsv(customer, transactions, profile)
        return saveCsvToFile(context, fileName, csv)
    }

    /**
     * Exports the summary of all customers to a .csv file.
     */
    suspend fun exportAllCustomersToFile(
        context: Context,
        customers: List<Customer>,
        profile: BusinessProfile? = null
    ): Result<File> {
        val timestamp = fileTimestampFormat.format(Date())
        val fileName = "parties_summary_$timestamp.csv"
        val csv = generateAllCustomersSummaryCsv(customers, profile)
        return saveCsvToFile(context, fileName, csv)
    }

    /**
     * Exports the full transaction journal to a .csv file.
     */
    suspend fun exportAllTransactionsToFile(
        context: Context,
        transactions: List<Transaction>,
        customers: List<Customer> = emptyList(),
        profile: BusinessProfile? = null
    ): Result<File> {
        val timestamp = fileTimestampFormat.format(Date())
        val fileName = "all_transactions_journal_$timestamp.csv"
        val csv = generateAllTransactionsJournalCsv(transactions, customers, profile)
        return saveCsvToFile(context, fileName, csv)
    }

    /**
     * Shares a CSV file via Android system share sheet using FileProvider.
     * Falls back to sharing plain text if URI resolution or FileProvider fails.
     */
    fun shareCsvFile(
        context: Context,
        file: File,
        subject: String = "Customer Ledger Statement (CSV)"
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the customer ledger statement exported from TallyKhata (${file.name})."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Ledger CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text sharing
            try {
                val content = if (file.exists()) file.readText() else ""
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, content)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(textIntent, "Share Ledger CSV").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {}
        }
    }
}
