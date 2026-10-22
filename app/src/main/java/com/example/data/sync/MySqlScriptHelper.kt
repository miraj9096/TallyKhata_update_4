package com.example.data.sync

import com.example.data.model.BusinessProfile
import com.example.data.model.CashEntry
import com.example.data.model.Customer
import com.example.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to generate MySQL table schemas, SQL data dumps, and
 * the companion PHP backend script for website cPanel/MySQL hosting.
 */
object MySqlScriptHelper {

    fun generateSqlDump(
        storeId: String,
        customers: List<Customer>,
        transactions: List<Transaction>,
        cashEntries: List<CashEntry>,
        profile: BusinessProfile
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val sb = StringBuilder()

        sb.append("-- ==========================================================\n")
        sb.append("-- Tally Khata MySQL Database Export Dump\n")
        sb.append("-- Store ID: $storeId\n")
        sb.append("-- Export Date: $dateStr\n")
        sb.append("-- ==========================================================\n\n")

        sb.append("SET FOREIGN_KEY_CHECKS=0;\n")
        sb.append("SET SQL_MODE = \"NO_AUTO_VALUE_ON_ZERO\";\n")
        sb.append("START TRANSACTION;\n")
        sb.append("SET time_zone = \"+00:00\";\n\n")

        // 1. Table: tally_profile
        sb.append("-- --------------------------------------------------------\n")
        sb.append("-- Table structure for `tally_profile`\n")
        sb.append("-- --------------------------------------------------------\n")
        sb.append("""
            CREATE TABLE IF NOT EXISTS `tally_profile` (
              `store_id` VARCHAR(64) NOT NULL,
              `business_name` VARCHAR(255) NOT NULL DEFAULT '',
              `owner_name` VARCHAR(255) NOT NULL DEFAULT '',
              `phone` VARCHAR(64) NOT NULL DEFAULT '',
              `email` VARCHAR(255) NOT NULL DEFAULT '',
              `currency` VARCHAR(16) NOT NULL DEFAULT '$',
              `updated_at` BIGINT NOT NULL DEFAULT 0,
              PRIMARY KEY (`store_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """.trimIndent()).append("\n\n")

        val pBusiness = escapeSql(profile.businessName)
        val pOwner = escapeSql(profile.ownerName)
        val pPhone = escapeSql(profile.phone)
        val pEmail = escapeSql(profile.email)
        val pCurrency = escapeSql(profile.currency)
        val pUpdated = System.currentTimeMillis()

        sb.append("""
            INSERT INTO `tally_profile` (`store_id`, `business_name`, `owner_name`, `phone`, `email`, `currency`, `updated_at`)
            VALUES ('${escapeSql(storeId)}', '$pBusiness', '$pOwner', '$pPhone', '$pEmail', '$pCurrency', $pUpdated)
            ON DUPLICATE KEY UPDATE
              `business_name` = VALUES(`business_name`),
              `owner_name` = VALUES(`owner_name`),
              `phone` = VALUES(`phone`),
              `email` = VALUES(`email`),
              `currency` = VALUES(`currency`),
              `updated_at` = VALUES(`updated_at`);
        """.trimIndent()).append("\n\n")

        // 2. Table: tally_customers
        sb.append("-- --------------------------------------------------------\n")
        sb.append("-- Table structure for `tally_customers`\n")
        sb.append("-- --------------------------------------------------------\n")
        sb.append("""
            CREATE TABLE IF NOT EXISTS `tally_customers` (
              `id` VARCHAR(64) NOT NULL,
              `store_id` VARCHAR(64) NOT NULL,
              `name` VARCHAR(255) NOT NULL,
              `phone` VARCHAR(64) NOT NULL DEFAULT '',
              `address` TEXT,
              `type` VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
              `total_gave` DOUBLE NOT NULL DEFAULT 0,
              `total_got` DOUBLE NOT NULL DEFAULT 0,
              `net_balance` DOUBLE NOT NULL DEFAULT 0,
              `created_at` BIGINT NOT NULL DEFAULT 0,
              `updated_at` BIGINT NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`, `store_id`),
              INDEX `idx_store_customer` (`store_id`, `phone`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """.trimIndent()).append("\n\n")

        if (customers.isNotEmpty()) {
            sb.append("-- Dumping data for `tally_customers`\n")
            for (c in customers) {
                val cId = escapeSql(c.id)
                val cName = escapeSql(c.name)
                val cPhone = escapeSql(c.phone)
                val cAddr = escapeSql(c.address)
                val cType = escapeSql(c.type)
                val cCreated = c.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                val cUpdated = c.updatedAt?.toDate()?.time ?: System.currentTimeMillis()

                sb.append("""
                    INSERT INTO `tally_customers` (`id`, `store_id`, `name`, `phone`, `address`, `type`, `total_gave`, `total_got`, `net_balance`, `created_at`, `updated_at`)
                    VALUES ('$cId', '${escapeSql(storeId)}', '$cName', '$cPhone', '$cAddr', '$cType', ${c.totalGave}, ${c.totalGot}, ${c.netBalance}, $cCreated, $cUpdated)
                    ON DUPLICATE KEY UPDATE
                      `name` = VALUES(`name`),
                      `phone` = VALUES(`phone`),
                      `address` = VALUES(`address`),
                      `type` = VALUES(`type`),
                      `total_gave` = VALUES(`total_gave`),
                      `total_got` = VALUES(`total_got`),
                      `net_balance` = VALUES(`net_balance`),
                      `updated_at` = VALUES(`updated_at`);
                """.trimIndent()).append("\n")
            }
            sb.append("\n")
        }

        // 3. Table: tally_transactions
        sb.append("-- --------------------------------------------------------\n")
        sb.append("-- Table structure for `tally_transactions`\n")
        sb.append("-- --------------------------------------------------------\n")
        sb.append("""
            CREATE TABLE IF NOT EXISTS `tally_transactions` (
              `id` VARCHAR(64) NOT NULL,
              `store_id` VARCHAR(64) NOT NULL,
              `customer_id` VARCHAR(64) NOT NULL,
              `customer_name` VARCHAR(255) NOT NULL DEFAULT '',
              `type` VARCHAR(32) NOT NULL,
              `amount` DOUBLE NOT NULL,
              `note` TEXT,
              `bill_number` VARCHAR(64) DEFAULT '',
              `transaction_date` BIGINT NOT NULL,
              `created_at` BIGINT NOT NULL DEFAULT 0,
              `updated_at` BIGINT NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`, `store_id`),
              INDEX `idx_trans_customer` (`store_id`, `customer_id`),
              INDEX `idx_trans_date` (`transaction_date`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """.trimIndent()).append("\n\n")

        if (transactions.isNotEmpty()) {
            sb.append("-- Dumping data for `tally_transactions`\n")
            for (t in transactions) {
                val tId = escapeSql(t.id)
                val cId = escapeSql(t.customerId)
                val cName = escapeSql(t.customerName)
                val tType = escapeSql(t.type)
                val tNote = escapeSql(t.note)
                val tBill = escapeSql(t.billNumber)
                val tDate = t.transactionDate.toDate().time
                val tCreated = t.createdAt?.toDate()?.time ?: tDate
                val tUpdated = t.updatedAt?.toDate()?.time ?: tDate

                sb.append("""
                    INSERT INTO `tally_transactions` (`id`, `store_id`, `customer_id`, `customer_name`, `type`, `amount`, `note`, `bill_number`, `transaction_date`, `created_at`, `updated_at`)
                    VALUES ('$tId', '${escapeSql(storeId)}', '$cId', '$cName', '$tType', ${t.amount}, '$tNote', '$tBill', $tDate, $tCreated, $tUpdated)
                    ON DUPLICATE KEY UPDATE
                      `customer_name` = VALUES(`customer_name`),
                      `type` = VALUES(`type`),
                      `amount` = VALUES(`amount`),
                      `note` = VALUES(`note`),
                      `bill_number` = VALUES(`bill_number`),
                      `transaction_date` = VALUES(`transaction_date`),
                      `updated_at` = VALUES(`updated_at`);
                """.trimIndent()).append("\n")
            }
            sb.append("\n")
        }

        // 4. Table: tally_cashbook
        sb.append("-- --------------------------------------------------------\n")
        sb.append("-- Table structure for `tally_cashbook`\n")
        sb.append("-- --------------------------------------------------------\n")
        sb.append("""
            CREATE TABLE IF NOT EXISTS `tally_cashbook` (
              `id` VARCHAR(64) NOT NULL,
              `store_id` VARCHAR(64) NOT NULL,
              `type` VARCHAR(32) NOT NULL,
              `amount` DOUBLE NOT NULL,
              `category` VARCHAR(128) NOT NULL DEFAULT 'General',
              `note` TEXT,
              `entry_date` BIGINT NOT NULL,
              PRIMARY KEY (`id`, `store_id`),
              INDEX `idx_cashbook_date` (`entry_date`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """.trimIndent()).append("\n\n")

        if (cashEntries.isNotEmpty()) {
            sb.append("-- Dumping data for `tally_cashbook`\n")
            for (ce in cashEntries) {
                val ceId = escapeSql(ce.id)
                val ceType = escapeSql(ce.type)
                val ceCat = escapeSql(ce.category)
                val ceNote = escapeSql(ce.note)
                val ceDate = ce.entryDate.toDate().time

                sb.append("""
                    INSERT INTO `tally_cashbook` (`id`, `store_id`, `type`, `amount`, `category`, `note`, `entry_date`)
                    VALUES ('$ceId', '${escapeSql(storeId)}', '$ceType', ${ce.amount}, '$ceCat', '$ceNote', $ceDate)
                    ON DUPLICATE KEY UPDATE
                      `type` = VALUES(`type`),
                      `amount` = VALUES(`amount`),
                      `category` = VALUES(`category`),
                      `note` = VALUES(`note`),
                      `entry_date` = VALUES(`entry_date`);
                """.trimIndent()).append("\n")
            }
            sb.append("\n")
        }

        sb.append("COMMIT;\n")
        sb.append("SET FOREIGN_KEY_CHECKS=1;\n")
        return sb.toString()
    }

    /**
     * Standalone SQL DDL Script formatted specifically for phpMyAdmin's SQL query runner.
     * Can be pasted directly into phpMyAdmin -> SQL tab to create all tables and initial store setup.
     */
    fun generatePhpMyAdminImportSql(storeId: String = "store_main"): String {
        return """
-- ==========================================================
-- Tally Khata - phpMyAdmin / MySQL Database Initialization
-- Target: phpMyAdmin (MySQL 5.7+ / MariaDB 10.2+)
-- Store ID: $storeId
-- ==========================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- --------------------------------------------------------
-- Table structure for `tally_profile`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tally_profile` (
  `store_id` VARCHAR(64) NOT NULL,
  `business_name` VARCHAR(255) NOT NULL DEFAULT 'My Store',
  `owner_name` VARCHAR(255) NOT NULL DEFAULT '',
  `phone` VARCHAR(64) NOT NULL DEFAULT '',
  `email` VARCHAR(255) NOT NULL DEFAULT '',
  `currency` VARCHAR(16) NOT NULL DEFAULT '$',
  `updated_at` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for `tally_customers`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tally_customers` (
  `id` VARCHAR(64) NOT NULL,
  `store_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(64) NOT NULL DEFAULT '',
  `address` TEXT,
  `type` VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
  `total_gave` DOUBLE NOT NULL DEFAULT 0,
  `total_got` DOUBLE NOT NULL DEFAULT 0,
  `net_balance` DOUBLE NOT NULL DEFAULT 0,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  `updated_at` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`, `store_id`),
  INDEX `idx_store_customer` (`store_id`, `phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for `tally_transactions`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tally_transactions` (
  `id` VARCHAR(64) NOT NULL,
  `store_id` VARCHAR(64) NOT NULL,
  `customer_id` VARCHAR(64) NOT NULL,
  `customer_name` VARCHAR(255) NOT NULL DEFAULT '',
  `type` VARCHAR(32) NOT NULL,
  `amount` DOUBLE NOT NULL,
  `note` TEXT,
  `bill_number` VARCHAR(64) DEFAULT '',
  `transaction_date` BIGINT NOT NULL,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  `updated_at` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`, `store_id`),
  INDEX `idx_trans_customer` (`store_id`, `customer_id`),
  INDEX `idx_trans_date` (`transaction_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for `tally_cashbook`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tally_cashbook` (
  `id` VARCHAR(64) NOT NULL,
  `store_id` VARCHAR(64) NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `amount` DOUBLE NOT NULL,
  `category` VARCHAR(128) NOT NULL DEFAULT 'General',
  `note` TEXT,
  `entry_date` BIGINT NOT NULL,
  PRIMARY KEY (`id`, `store_id`),
  INDEX `idx_cashbook_date` (`entry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default store profile if not exists
INSERT INTO `tally_profile` (`store_id`, `business_name`, `owner_name`, `phone`, `email`, `currency`, `updated_at`)
VALUES ('$storeId', 'My Store', '', '', '', '$', UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE `updated_at` = VALUES(`updated_at`);

COMMIT;
        """.trimIndent()
    }

    private fun escapeSql(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("'", "''")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
    }

    /**
     * Complete, secure PHP backend script for users to drop onto their web host.
     * Handles individual CRUD operations, batch sync, authentication, and schema verification.
     */
    fun getSamplePhpBackendScript(suggestedApiKey: String = "TALLY_SECRET_KEY_12345"): String {
        return """
<?php
/**
 * Tally Khata - Website phpMyAdmin / MySQL Synchronization API Endpoint
 * 
 * Instructions:
 * 1. Upload this file as `tally_sync.php` or `api.php` to your web server (e.g. public_html/api/tally_sync.php).
 * 2. Configure your MySQL credentials below in db_host, db_name, db_user, db_pass.
 * 3. Set your custom secret API Key in api_key.
 * 4. Enter your endpoint URL in the Tally Khata app: https://yourdomain.com/api/tally_sync.php
 */

header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, X-API-Key, X-Store-Id');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');

if (${'$'}_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// -------------------------------------------------------------
// Database & Security Configuration
// -------------------------------------------------------------
${'$'}db_host = 'localhost';
${'$'}db_name = 'your_mysql_database_name';
${'$'}db_user = 'your_mysql_username';
${'$'}db_pass = 'your_mysql_password';
${'$'}api_key = '$suggestedApiKey'; // Match this key in the Android app

// -------------------------------------------------------------
// Authentication Check
// -------------------------------------------------------------
${'$'}headers = getallheaders();
${'$'}received_key = ${'$'}_SERVER['HTTP_X_API_KEY'] ?? ${'$'}headers['X-API-Key'] ?? ${'$'}headers['x-api-key'] ?? ${'$'}_GET['key'] ?? '';

if (${'$'}api_key !== '' && ${'$'}received_key !== ${'$'}api_key) {
    http_response_code(401);
    echo json_encode([
        'status' => 'error',
        'message' => 'Unauthorized: Invalid or missing API Key'
    ]);
    exit;
}

// -------------------------------------------------------------
// Database Connection
// -------------------------------------------------------------
try {
    ${'$'}pdo = new PDO(
        "mysql:host=${'$'}db_host;dbname=${'$'}db_name;charset=utf8mb4",
        ${'$'}db_user,
        ${'$'}db_pass,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]
    );
} catch (PDOException ${'$'}e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'MySQL connection error: ' . ${'$'}e->getMessage()
    ]);
    exit;
}

// Auto-create tables if they do not exist
init_tables(${'$'}pdo);

// -------------------------------------------------------------
// Request Processing
// -------------------------------------------------------------
${'$'}input = json_decode(file_get_contents('php://input'), true) ?? [];
${'$'}action = ${'$'}input['action'] ?? ${'$'}_GET['action'] ?? 'ping';
${'$'}store_id = ${'$'}input['storeId'] ?? ${'$'}_SERVER['HTTP_X_STORE_ID'] ?? ${'$'}_GET['store_id'] ?? 'store_main';

switch (${'$'}action) {
    case 'ping':
        ${'$'}cust_count = 0;
        ${'$'}trans_count = 0;
        ${'$'}cash_count = 0;
        try {
            ${'$'}cust_count = (int)${'$'}pdo->query("SELECT COUNT(*) FROM `tally_customers` WHERE `store_id` = " . ${'$'}pdo->quote(${'$'}store_id))->fetchColumn();
            ${'$'}trans_count = (int)${'$'}pdo->query("SELECT COUNT(*) FROM `tally_transactions` WHERE `store_id` = " . ${'$'}pdo->quote(${'$'}store_id))->fetchColumn();
            ${'$'}cash_count = (int)${'$'}pdo->query("SELECT COUNT(*) FROM `tally_cashbook` WHERE `store_id` = " . ${'$'}pdo->quote(${'$'}store_id))->fetchColumn();
        } catch (Exception ${'$'}e) {}
        echo json_encode([
            'status' => 'ok',
            'message' => 'Connected to MySQL Database via phpMyAdmin bridge successfully',
            'timestamp' => time() * 1000,
            'database' => ${'$'}db_name,
            'serverVersion' => ${'$'}pdo->getAttribute(PDO::ATTR_SERVER_VERSION),
            'storeId' => ${'$'}store_id,
            'customerCount' => ${'$'}cust_count,
            'transactionCount' => ${'$'}trans_count,
            'cashbookCount' => ${'$'}cash_count,
            'tablesReady' => true
        ]);
        break;

    case 'save_customer':
        try {
            ${'$'}c = ${'$'}input['customer'] ?? ${'$'}input;
            if (empty(${'$'}c['id']) || empty(${'$'}c['name'])) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Customer id and name required']);
                exit;
            }
            ${'$'}stmt = ${'$'}pdo->prepare("
                INSERT INTO `tally_customers` (`id`, `store_id`, `name`, `phone`, `address`, `type`, `total_gave`, `total_got`, `net_balance`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  `name` = VALUES(`name`),
                  `phone` = VALUES(`phone`),
                  `address` = VALUES(`address`),
                  `type` = VALUES(`type`),
                  `total_gave` = VALUES(`total_gave`),
                  `total_got` = VALUES(`total_got`),
                  `net_balance` = VALUES(`net_balance`),
                  `updated_at` = VALUES(`updated_at`)
            ");
            ${'$'}stmt->execute([
                ${'$'}c['id'],
                ${'$'}store_id,
                ${'$'}c['name'],
                ${'$'}c['phone'] ?? '',
                ${'$'}c['address'] ?? '',
                ${'$'}c['type'] ?? 'CUSTOMER',
                (float)(${'$'}c['totalGave'] ?? 0),
                (float)(${'$'}c['totalGot'] ?? 0),
                (float)(${'$'}c['netBalance'] ?? 0),
                (int)(${'$'}c['createdAt'] ?? time() * 1000),
                (int)(${'$'}c['updatedAt'] ?? time() * 1000)
            ]);
            echo json_encode(['status' => 'ok', 'message' => 'Customer saved to MySQL', 'id' => ${'$'}c['id']]);
        } catch (Exception ${'$'}e) {
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to save customer: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'delete_customer':
        try {
            ${'$'}cid = ${'$'}input['id'] ?? ${'$'}_GET['id'] ?? '';
            if (empty(${'$'}cid)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Customer id required']);
                exit;
            }
            ${'$'}pdo->beginTransaction();
            ${'$'}stmt1 = ${'$'}pdo->prepare("DELETE FROM `tally_transactions` WHERE `customer_id` = ? AND `store_id` = ?");
            ${'$'}stmt1->execute([${'$'}cid, ${'$'}store_id]);
            ${'$'}stmt2 = ${'$'}pdo->prepare("DELETE FROM `tally_customers` WHERE `id` = ? AND `store_id` = ?");
            ${'$'}stmt2->execute([${'$'}cid, ${'$'}store_id]);
            ${'$'}pdo->commit();
            echo json_encode(['status' => 'ok', 'message' => 'Customer deleted from MySQL', 'id' => ${'$'}cid]);
        } catch (Exception ${'$'}e) {
            if (${'$'}pdo->inTransaction()) ${'$'}pdo->rollBack();
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to delete customer: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'save_transaction':
        try {
            ${'$'}t = ${'$'}input['transaction'] ?? ${'$'}input;
            if (empty(${'$'}t['id']) || empty(${'$'}t['customerId'])) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Transaction id and customerId required']);
                exit;
            }
            ${'$'}pdo->beginTransaction();
            ${'$'}stmt = ${'$'}pdo->prepare("
                INSERT INTO `tally_transactions` (`id`, `store_id`, `customer_id`, `customer_name`, `type`, `amount`, `note`, `bill_number`, `transaction_date`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  `customer_name` = VALUES(`customer_name`),
                  `type` = VALUES(`type`),
                  `amount` = VALUES(`amount`),
                  `note` = VALUES(`note`),
                  `bill_number` = VALUES(`bill_number`),
                  `transaction_date` = VALUES(`transaction_date`),
                  `updated_at` = VALUES(`updated_at`)
            ");
            ${'$'}stmt->execute([
                ${'$'}t['id'],
                ${'$'}store_id,
                ${'$'}t['customerId'],
                ${'$'}t['customerName'] ?? '',
                ${'$'}t['type'] ?? 'GAVE',
                (float)(${'$'}t['amount'] ?? 0),
                ${'$'}t['note'] ?? '',
                ${'$'}t['billNumber'] ?? '',
                (int)(${'$'}t['transactionDate'] ?? time() * 1000),
                (int)(${'$'}t['createdAt'] ?? time() * 1000),
                (int)(${'$'}t['updatedAt'] ?? time() * 1000)
            ]);

            // Update customer balance if provided
            if (!empty(${'$'}input['customer'])) {
                ${'$'}c = ${'$'}input['customer'];
                ${'$'}stmtCust = ${'$'}pdo->prepare("
                    UPDATE `tally_customers`
                    SET `total_gave` = ?, `total_got` = ?, `net_balance` = ?, `updated_at` = ?
                    WHERE `id` = ? AND `store_id` = ?
                ");
                ${'$'}stmtCust->execute([
                    (float)(${'$'}c['totalGave'] ?? 0),
                    (float)(${'$'}c['totalGot'] ?? 0),
                    (float)(${'$'}c['netBalance'] ?? 0),
                    time() * 1000,
                    ${'$'}t['customerId'],
                    ${'$'}store_id
                ]);
            }
            ${'$'}pdo->commit();
            echo json_encode(['status' => 'ok', 'message' => 'Transaction recorded in MySQL', 'id' => ${'$'}t['id']]);
        } catch (Exception ${'$'}e) {
            if (${'$'}pdo->inTransaction()) ${'$'}pdo->rollBack();
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to save transaction: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'delete_transaction':
        try {
            ${'$'}tid = ${'$'}input['id'] ?? ${'$'}_GET['id'] ?? '';
            ${'$'}cid = ${'$'}input['customerId'] ?? ${'$'}_GET['customerId'] ?? '';
            if (empty(${'$'}tid)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Transaction id required']);
                exit;
            }
            ${'$'}pdo->beginTransaction();
            ${'$'}stmt = ${'$'}pdo->prepare("DELETE FROM `tally_transactions` WHERE `id` = ? AND `store_id` = ?");
            ${'$'}stmt->execute([${'$'}tid, ${'$'}store_id]);

            if (!empty(${'$'}input['customer']) && !empty(${'$'}cid)) {
                ${'$'}c = ${'$'}input['customer'];
                ${'$'}stmtCust = ${'$'}pdo->prepare("
                    UPDATE `tally_customers`
                    SET `total_gave` = ?, `total_got` = ?, `net_balance` = ?, `updated_at` = ?
                    WHERE `id` = ? AND `store_id` = ?
                ");
                ${'$'}stmtCust->execute([
                    (float)(${'$'}c['totalGave'] ?? 0),
                    (float)(${'$'}c['totalGot'] ?? 0),
                    (float)(${'$'}c['netBalance'] ?? 0),
                    time() * 1000,
                    ${'$'}cid,
                    ${'$'}store_id
                ]);
            }
            ${'$'}pdo->commit();
            echo json_encode(['status' => 'ok', 'message' => 'Transaction deleted from MySQL', 'id' => ${'$'}tid]);
        } catch (Exception ${'$'}e) {
            if (${'$'}pdo->inTransaction()) ${'$'}pdo->rollBack();
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to delete transaction: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'save_cash':
        try {
            ${'$'}cb = ${'$'}input['cashEntry'] ?? ${'$'}input;
            if (empty(${'$'}cb['id'])) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Cash entry id required']);
                exit;
            }
            ${'$'}stmt = ${'$'}pdo->prepare("
                INSERT INTO `tally_cashbook` (`id`, `store_id`, `type`, `amount`, `category`, `note`, `entry_date`)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  `type` = VALUES(`type`),
                  `amount` = VALUES(`amount`),
                  `category` = VALUES(`category`),
                  `note` = VALUES(`note`),
                  `entry_date` = VALUES(`entry_date`)
            ");
            ${'$'}stmt->execute([
                ${'$'}cb['id'],
                ${'$'}store_id,
                ${'$'}cb['type'] ?? 'CASH_IN',
                (float)(${'$'}cb['amount'] ?? 0),
                ${'$'}cb['category'] ?? 'General',
                ${'$'}cb['note'] ?? '',
                (int)(${'$'}cb['entryDate'] ?? time() * 1000)
            ]);
            echo json_encode(['status' => 'ok', 'message' => 'Cash entry saved to MySQL', 'id' => ${'$'}cb['id']]);
        } catch (Exception ${'$'}e) {
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to save cash entry: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'delete_cash':
        try {
            ${'$'}id = ${'$'}input['id'] ?? ${'$'}_GET['id'] ?? '';
            if (empty(${'$'}id)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Cash entry id required']);
                exit;
            }
            ${'$'}stmt = ${'$'}pdo->prepare("DELETE FROM `tally_cashbook` WHERE `id` = ? AND `store_id` = ?");
            ${'$'}stmt->execute([${'$'}id, ${'$'}store_id]);
            echo json_encode(['status' => 'ok', 'message' => 'Cash entry deleted from MySQL', 'id' => ${'$'}id]);
        } catch (Exception ${'$'}e) {
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to delete cash entry: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'save_profile':
        try {
            ${'$'}p = ${'$'}input['profile'] ?? ${'$'}input;
            ${'$'}stmt = ${'$'}pdo->prepare("
                INSERT INTO `tally_profile` (`store_id`, `business_name`, `owner_name`, `phone`, `email`, `currency`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  `business_name` = VALUES(`business_name`),
                  `owner_name` = VALUES(`owner_name`),
                  `phone` = VALUES(`phone`),
                  `email` = VALUES(`email`),
                  `currency` = VALUES(`currency`),
                  `updated_at` = VALUES(`updated_at`)
            ");
            ${'$'}stmt->execute([
                ${'$'}store_id,
                ${'$'}p['businessName'] ?? '',
                ${'$'}p['ownerName'] ?? '',
                ${'$'}p['phone'] ?? '',
                ${'$'}p['email'] ?? '',
                ${'$'}p['currency'] ?? '$',
                time() * 1000
            ]);
            echo json_encode(['status' => 'ok', 'message' => 'Store profile updated in MySQL']);
        } catch (Exception ${'$'}e) {
            http_response_code(500);
            echo json_encode(['status' => 'error', 'message' => 'Failed to save profile: ' . ${'$'}e->getMessage()]);
        }
        break;

    case 'push':
        // Synchronize from offline app to MySQL
        try {
            ${'$'}pdo->beginTransaction();

            // 1. Profile
            if (!empty(${'$'}input['profile'])) {
                ${'$'}p = ${'$'}input['profile'];
                ${'$'}stmt = ${'$'}pdo->prepare("
                    INSERT INTO `tally_profile` (`store_id`, `business_name`, `owner_name`, `phone`, `email`, `currency`, `updated_at`)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      `business_name` = VALUES(`business_name`),
                      `owner_name` = VALUES(`owner_name`),
                      `phone` = VALUES(`phone`),
                      `email` = VALUES(`email`),
                      `currency` = VALUES(`currency`),
                      `updated_at` = VALUES(`updated_at`)
                ");
                ${'$'}stmt->execute([
                    ${'$'}store_id,
                    ${'$'}p['businessName'] ?? '',
                    ${'$'}p['ownerName'] ?? '',
                    ${'$'}p['phone'] ?? '',
                    ${'$'}p['email'] ?? '',
                    ${'$'}p['currency'] ?? '$',
                    time() * 1000
                ]);
            }

            // 2. Customers
            ${'$'}cust_count = 0;
            if (!empty(${'$'}input['customers']) && is_array(${'$'}input['customers'])) {
                ${'$'}stmtCust = ${'$'}pdo->prepare("
                    INSERT INTO `tally_customers` (`id`, `store_id`, `name`, `phone`, `address`, `type`, `total_gave`, `total_got`, `net_balance`, `created_at`, `updated_at`)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      `name` = VALUES(`name`),
                      `phone` = VALUES(`phone`),
                      `address` = VALUES(`address`),
                      `type` = VALUES(`type`),
                      `total_gave` = VALUES(`total_gave`),
                      `total_got` = VALUES(`total_got`),
                      `net_balance` = VALUES(`net_balance`),
                      `updated_at` = VALUES(`updated_at`)
                ");
                foreach (${'$'}input['customers'] as ${'$'}c) {
                    ${'$'}stmtCust->execute([
                        ${'$'}c['id'],
                        ${'$'}store_id,
                        ${'$'}c['name'] ?? '',
                        ${'$'}c['phone'] ?? '',
                        ${'$'}c['address'] ?? '',
                        ${'$'}c['type'] ?? 'CUSTOMER',
                        (float)(${'$'}c['totalGave'] ?? 0),
                        (float)(${'$'}c['totalGot'] ?? 0),
                        (float)(${'$'}c['netBalance'] ?? 0),
                        (int)(${'$'}c['createdAt'] ?? time() * 1000),
                        (int)(${'$'}c['updatedAt'] ?? time() * 1000)
                    ]);
                    ${'$'}cust_count++;
                }
            }

            // 3. Transactions
            ${'$'}trans_count = 0;
            if (!empty(${'$'}input['transactions']) && is_array(${'$'}input['transactions'])) {
                ${'$'}stmtTrans = ${'$'}pdo->prepare("
                    INSERT INTO `tally_transactions` (`id`, `store_id`, `customer_id`, `customer_name`, `type`, `amount`, `note`, `bill_number`, `transaction_date`, `created_at`, `updated_at`)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      `customer_name` = VALUES(`customer_name`),
                      `type` = VALUES(`type`),
                      `amount` = VALUES(`amount`),
                      `note` = VALUES(`note`),
                      `bill_number` = VALUES(`bill_number`),
                      `transaction_date` = VALUES(`transaction_date`),
                      `updated_at` = VALUES(`updated_at`)
                ");
                foreach (${'$'}input['transactions'] as ${'$'}t) {
                    ${'$'}stmtTrans->execute([
                        ${'$'}t['id'],
                        ${'$'}store_id,
                        ${'$'}t['customerId'] ?? '',
                        ${'$'}t['customerName'] ?? '',
                        ${'$'}t['type'] ?? 'GAVE',
                        (float)(${'$'}t['amount'] ?? 0),
                        ${'$'}t['note'] ?? '',
                        ${'$'}t['billNumber'] ?? '',
                        (int)(${'$'}t['transactionDate'] ?? time() * 1000),
                        (int)(${'$'}t['createdAt'] ?? time() * 1000),
                        (int)(${'$'}t['updatedAt'] ?? time() * 1000)
                    ]);
                    ${'$'}trans_count++;
                }
            }

            // 4. Cashbook
            ${'$'}cash_count = 0;
            if (!empty(${'$'}input['cashbook']) && is_array(${'$'}input['cashbook'])) {
                ${'$'}stmtCash = ${'$'}pdo->prepare("
                    INSERT INTO `tally_cashbook` (`id`, `store_id`, `type`, `amount`, `category`, `note`, `entry_date`)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      `type` = VALUES(`type`),
                      `amount` = VALUES(`amount`),
                      `category` = VALUES(`category`),
                      `note` = VALUES(`note`),
                      `entry_date` = VALUES(`entry_date`)
                ");
                foreach (${'$'}input['cashbook'] as ${'$'}cb) {
                    ${'$'}stmtCash->execute([
                        ${'$'}cb['id'],
                        ${'$'}store_id,
                        ${'$'}cb['type'] ?? 'CASH_IN',
                        (float)(${'$'}cb['amount'] ?? 0),
                        ${'$'}cb['category'] ?? 'General',
                        ${'$'}cb['note'] ?? '',
                        (int)(${'$'}cb['entryDate'] ?? time() * 1000)
                    ]);
                    ${'$'}cash_count++;
                }
            }

            ${'$'}pdo->commit();

            echo json_encode([
                'status' => 'ok',
                'message' => 'Successfully synchronized offline ledger to website MySQL',
                'syncedCustomers' => ${'$'}cust_count,
                'syncedTransactions' => ${'$'}trans_count,
                'syncedCashbook' => ${'$'}cash_count,
                'timestamp' => time() * 1000
            ]);
        } catch (Exception ${'$'}e) {
            if (${'$'}pdo->inTransaction()) {
                ${'$'}pdo->rollBack();
            }
            http_response_code(500);
            echo json_encode([
                'status' => 'error',
                'message' => 'Sync push failed: ' . ${'$'}e->getMessage()
            ]);
        }
        break;

    case 'pull':
        // Download ledger data from MySQL to restore on mobile
        try {
            // Profile
            ${'$'}stmt = ${'$'}pdo->prepare("SELECT * FROM `tally_profile` WHERE `store_id` = ? LIMIT 1");
            ${'$'}stmt->execute([${'$'}store_id]);
            ${'$'}profile = ${'$'}stmt->fetch() ?: null;

            // Customers
            ${'$'}stmt = ${'$'}pdo->prepare("SELECT * FROM `tally_customers` WHERE `store_id` = ? ORDER BY `updated_at` DESC");
            ${'$'}stmt->execute([${'$'}store_id]);
            ${'$'}customers = ${'$'}stmt->fetchAll();

            // Transactions
            ${'$'}stmt = ${'$'}pdo->prepare("SELECT * FROM `tally_transactions` WHERE `store_id` = ? ORDER BY `transaction_date` DESC");
            ${'$'}stmt->execute([${'$'}store_id]);
            ${'$'}transactions = ${'$'}stmt->fetchAll();

            // Cashbook
            ${'$'}stmt = ${'$'}pdo->prepare("SELECT * FROM `tally_cashbook` WHERE `store_id` = ? ORDER BY `entry_date` DESC");
            ${'$'}stmt->execute([${'$'}store_id]);
            ${'$'}cashbook = ${'$'}stmt->fetchAll();

            echo json_encode([
                'status' => 'ok',
                'storeId' => ${'$'}store_id,
                'profile' => ${'$'}profile,
                'customers' => ${'$'}customers,
                'transactions' => ${'$'}transactions,
                'cashbook' => ${'$'}cashbook,
                'count' => count(${'$'}customers)
            ]);
        } catch (Exception ${'$'}e) {
            http_response_code(500);
            echo json_encode([
                'status' => 'error',
                'message' => 'Sync pull failed: ' . ${'$'}e->getMessage()
            ]);
        }
        break;

    default:
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Invalid action: ' . ${'$'}action
        ]);
        break;
}

function init_tables(${'$'}pdo) {
    ${'$'}pdo->exec("
        CREATE TABLE IF NOT EXISTS `tally_profile` (
          `store_id` VARCHAR(64) NOT NULL,
          `business_name` VARCHAR(255) NOT NULL DEFAULT '',
          `owner_name` VARCHAR(255) NOT NULL DEFAULT '',
          `phone` VARCHAR(64) NOT NULL DEFAULT '',
          `email` VARCHAR(255) NOT NULL DEFAULT '',
          `currency` VARCHAR(16) NOT NULL DEFAULT '$',
          `updated_at` BIGINT NOT NULL DEFAULT 0,
          PRIMARY KEY (`store_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

        CREATE TABLE IF NOT EXISTS `tally_customers` (
          `id` VARCHAR(64) NOT NULL,
          `store_id` VARCHAR(64) NOT NULL,
          `name` VARCHAR(255) NOT NULL,
          `phone` VARCHAR(64) NOT NULL DEFAULT '',
          `address` TEXT,
          `type` VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
          `total_gave` DOUBLE NOT NULL DEFAULT 0,
          `total_got` DOUBLE NOT NULL DEFAULT 0,
          `net_balance` DOUBLE NOT NULL DEFAULT 0,
          `created_at` BIGINT NOT NULL DEFAULT 0,
          `updated_at` BIGINT NOT NULL DEFAULT 0,
          PRIMARY KEY (`id`, `store_id`),
          INDEX `idx_store_customer` (`store_id`, `phone`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

        CREATE TABLE IF NOT EXISTS `tally_transactions` (
          `id` VARCHAR(64) NOT NULL,
          `store_id` VARCHAR(64) NOT NULL,
          `customer_id` VARCHAR(64) NOT NULL,
          `customer_name` VARCHAR(255) NOT NULL DEFAULT '',
          `type` VARCHAR(32) NOT NULL,
          `amount` DOUBLE NOT NULL,
          `note` TEXT,
          `bill_number` VARCHAR(64) DEFAULT '',
          `transaction_date` BIGINT NOT NULL,
          `created_at` BIGINT NOT NULL DEFAULT 0,
          `updated_at` BIGINT NOT NULL DEFAULT 0,
          PRIMARY KEY (`id`, `store_id`),
          INDEX `idx_trans_customer` (`store_id`, `customer_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

        CREATE TABLE IF NOT EXISTS `tally_cashbook` (
          `id` VARCHAR(64) NOT NULL,
          `store_id` VARCHAR(64) NOT NULL,
          `type` VARCHAR(32) NOT NULL,
          `amount` DOUBLE NOT NULL,
          `category` VARCHAR(128) NOT NULL DEFAULT 'General',
          `note` TEXT,
          `entry_date` BIGINT NOT NULL,
          PRIMARY KEY (`id`, `store_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ");
}
?>
        """.trimIndent()
    }
}
