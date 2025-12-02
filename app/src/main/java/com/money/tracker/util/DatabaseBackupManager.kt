package com.money.tracker.util

import android.content.Context
import android.net.Uri
import com.money.tracker.data.AppDatabase
import com.money.tracker.data.entity.Budget
import com.money.tracker.data.entity.BudgetPreallocation
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.CategoryBudget
import com.money.tracker.data.entity.SharingApp
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class DatabaseBackupManager(private val context: Context) {

    companion object {
        const val CURRENT_BACKUP_VERSION = 10
    }

    data class BackupResult(
        val success: Boolean,
        val message: String
    )

    suspend fun exportToJson(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)

            val backup = JSONObject().apply {
                put("version", CURRENT_BACKUP_VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("categories", exportCategories(db))
                put("transactions", exportTransactions(db))
                put("budgets", exportBudgets(db))
                put("budgetPreallocations", exportBudgetPreallocations(db))
                put("categoryBudgets", exportCategoryBudgets(db))
                put("sharingApps", exportSharingApps(db))
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backup.toString(2).toByteArray())
            }

            BackupResult(true, "Backup exported successfully")
        } catch (e: Exception) {
            BackupResult(false, "Export failed: ${e.message}")
        }
    }

    suspend fun importFromJson(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext BackupResult(false, "Could not read file")

            val backup = JSONObject(jsonString)
            val version = backup.optInt("version", 1)

            val db = AppDatabase.getDatabase(context)

            // Import with migration support
            importCategories(db, backup.getJSONArray("categories"), version)
            importTransactions(db, backup.getJSONArray("transactions"), version)
            importBudgets(db, backup.getJSONArray("budgets"), version)

            // These tables were added in later versions
            if (version >= 9 && backup.has("budgetPreallocations")) {
                importBudgetPreallocations(db, backup.getJSONArray("budgetPreallocations"))
            }
            if (version >= 10 && backup.has("categoryBudgets")) {
                importCategoryBudgets(db, backup.getJSONArray("categoryBudgets"))
            }
            if (version >= 7 && backup.has("sharingApps")) {
                importSharingApps(db, backup.getJSONArray("sharingApps"))
            }

            BackupResult(true, "Backup imported successfully")
        } catch (e: Exception) {
            BackupResult(false, "Import failed: ${e.message}")
        }
    }

    // Export functions
    private suspend fun exportCategories(db: AppDatabase): JSONArray {
        val categories = db.categoryDao().getAllCategoriesSync()
        return JSONArray().apply {
            categories.forEach { cat ->
                put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("emoji", cat.emoji)
                    put("parentId", cat.parentId ?: JSONObject.NULL)
                    put("isDefault", cat.isDefault)
                })
            }
        }
    }

    private suspend fun exportTransactions(db: AppDatabase): JSONArray {
        val transactions = db.transactionDao().getAllTransactionsSync()
        return JSONArray().apply {
            transactions.forEach { txn ->
                put(JSONObject().apply {
                    put("id", txn.id)
                    put("amount", txn.amount)
                    put("type", txn.type.name)
                    put("description", txn.description)
                    put("merchant", txn.merchant ?: JSONObject.NULL)
                    put("categoryId", txn.categoryId ?: JSONObject.NULL)
                    put("source", txn.source.name)
                    put("date", txn.date)
                    put("rawMessage", txn.rawMessage ?: JSONObject.NULL)
                    put("isManual", txn.isManual)
                    put("isPending", txn.isPending)
                    put("createdAt", txn.createdAt)
                    put("isSplit", txn.isSplit)
                    put("splitNumerator", txn.splitNumerator)
                    put("splitDenominator", txn.splitDenominator)
                    put("totalAmount", txn.totalAmount)
                    put("splitSynced", txn.splitSynced)
                })
            }
        }
    }

    private suspend fun exportBudgets(db: AppDatabase): JSONArray {
        val budgets = db.budgetDao().getAllBudgetsSync()
        return JSONArray().apply {
            budgets.forEach { budget ->
                put(JSONObject().apply {
                    put("yearMonth", budget.yearMonth)
                    put("amount", budget.amount)
                })
            }
        }
    }

    private suspend fun exportBudgetPreallocations(db: AppDatabase): JSONArray {
        val preallocations = db.budgetPreallocationDao().getAllPreallocationsSync()
        return JSONArray().apply {
            preallocations.forEach { prealloc ->
                put(JSONObject().apply {
                    put("yearMonth", prealloc.yearMonth)
                    put("categoryId", prealloc.categoryId)
                    put("amount", prealloc.amount)
                })
            }
        }
    }

    private suspend fun exportCategoryBudgets(db: AppDatabase): JSONArray {
        val categoryBudgets = db.categoryBudgetDao().getAllCategoryBudgetsSync()
        return JSONArray().apply {
            categoryBudgets.forEach { cb ->
                put(JSONObject().apply {
                    put("yearMonth", cb.yearMonth)
                    put("categoryId", cb.categoryId)
                    put("amount", cb.amount)
                })
            }
        }
    }

    private suspend fun exportSharingApps(db: AppDatabase): JSONArray {
        val apps = db.sharingAppDao().getAllAppsSync()
        return JSONArray().apply {
            apps.forEach { app ->
                put(JSONObject().apply {
                    put("id", app.id)
                    put("name", app.name)
                    put("packageName", app.packageName)
                    put("isEnabled", app.isEnabled)
                    put("sortOrder", app.sortOrder)
                })
            }
        }
    }

    // Import functions with version migration
    private suspend fun importCategories(db: AppDatabase, array: JSONArray, version: Int) {
        val categoryDao = db.categoryDao()
        // Clear existing non-default categories or all if importing
        categoryDao.deleteAllCategories()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val category = Category(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                emoji = obj.getString("emoji"),
                parentId = if (obj.isNull("parentId")) null else obj.getLong("parentId"),
                isDefault = obj.optBoolean("isDefault", false)
            )
            categoryDao.insertWithId(category)
        }
    }

    private suspend fun importTransactions(db: AppDatabase, array: JSONArray, version: Int) {
        val transactionDao = db.transactionDao()
        transactionDao.deleteAllTransactions()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            // Handle version migrations for transactions
            val isSplit = if (version >= 7) obj.optBoolean("isSplit", false) else false
            val splitNumerator = if (version >= 7) obj.optInt("splitNumerator", 1) else 1
            val splitDenominator = if (version >= 7) obj.optInt("splitDenominator", 1) else 1
            val totalAmount = if (version >= 7) obj.optDouble("totalAmount", 0.0) else 0.0
            val splitSynced = if (version >= 7) obj.optBoolean("splitSynced", false) else false

            val transaction = Transaction(
                id = obj.getLong("id"),
                amount = obj.getDouble("amount"),
                type = TransactionType.valueOf(obj.getString("type")),
                description = obj.getString("description"),
                merchant = if (obj.isNull("merchant")) null else obj.getString("merchant"),
                categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                source = try {
                    TransactionSource.valueOf(obj.getString("source"))
                } catch (e: Exception) {
                    TransactionSource.OTHER
                },
                date = obj.getLong("date"),
                rawMessage = if (obj.isNull("rawMessage")) null else obj.optString("rawMessage"),
                isManual = obj.optBoolean("isManual", false),
                isPending = obj.optBoolean("isPending", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                isSplit = isSplit,
                splitNumerator = splitNumerator,
                splitDenominator = splitDenominator,
                totalAmount = totalAmount,
                splitSynced = splitSynced
            )
            transactionDao.insertWithId(transaction)
        }
    }

    private suspend fun importBudgets(db: AppDatabase, array: JSONArray, version: Int) {
        val budgetDao = db.budgetDao()
        budgetDao.deleteAllBudgets()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val budget = Budget(
                yearMonth = obj.getString("yearMonth"),
                amount = obj.getDouble("amount")
            )
            budgetDao.insert(budget)
        }
    }

    private suspend fun importBudgetPreallocations(db: AppDatabase, array: JSONArray) {
        val dao = db.budgetPreallocationDao()
        dao.deleteAllPreallocations()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val prealloc = BudgetPreallocation(
                yearMonth = obj.getString("yearMonth"),
                categoryId = obj.getLong("categoryId"),
                amount = obj.getDouble("amount")
            )
            dao.insert(prealloc)
        }
    }

    private suspend fun importCategoryBudgets(db: AppDatabase, array: JSONArray) {
        val dao = db.categoryBudgetDao()
        dao.deleteAllCategoryBudgets()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val cb = CategoryBudget(
                yearMonth = obj.getString("yearMonth"),
                categoryId = obj.getLong("categoryId"),
                amount = obj.getDouble("amount")
            )
            dao.insert(cb)
        }
    }

    private suspend fun importSharingApps(db: AppDatabase, array: JSONArray) {
        val dao = db.sharingAppDao()
        dao.deleteAllApps()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val app = SharingApp(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                packageName = obj.getString("packageName"),
                isEnabled = obj.optBoolean("isEnabled", true),
                sortOrder = obj.optInt("sortOrder", 0)
            )
            dao.insertWithId(app)
        }
    }
}
