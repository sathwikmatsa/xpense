package com.money.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.money.tracker.data.dao.BudgetDao
import com.money.tracker.data.dao.BudgetPreallocationDao
import com.money.tracker.data.dao.CategoryBudgetDao
import com.money.tracker.data.dao.CategoryDao
import com.money.tracker.data.dao.SharingAppDao
import com.money.tracker.data.dao.TransactionDao
import com.money.tracker.data.dao.UpiReminderDao
import com.money.tracker.data.entity.Budget
import com.money.tracker.data.entity.BudgetPreallocation
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.CategoryBudget
import com.money.tracker.data.entity.DefaultCategories
import com.money.tracker.data.entity.SharingApp
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.UpiReminder
import androidx.room.migration.Migration

@Database(
    entities = [Transaction::class, Category::class, Budget::class, UpiReminder::class, SharingApp::class, BudgetPreallocation::class, CategoryBudget::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun upiReminderDao(): UpiReminderDao
    abstract fun sharingAppDao(): SharingAppDao
    abstract fun budgetPreallocationDao(): BudgetPreallocationDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 6 to 7: Add split transaction fields and sharing_apps table
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add split transaction columns to transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN isSplit INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN splitNumerator INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transactions ADD COLUMN splitDenominator INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transactions ADD COLUMN totalAmount REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN splitSynced INTEGER NOT NULL DEFAULT 0")

                // Create sharing_apps table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sharing_apps (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Insert default sharing apps
                db.execSQL("INSERT INTO sharing_apps (name, packageName, isEnabled, sortOrder) VALUES ('Splitwise', 'com.Splitwise.SplitwiseMobile', 1, 0)")
                db.execSQL("INSERT INTO sharing_apps (name, packageName, isEnabled, sortOrder) VALUES ('PhonePe', 'com.phonepe.app', 1, 1)")
            }
        }

        // Migration from version 7 to 8: Was preallocatedBudget on categories (reverted)
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // This migration was for adding preallocatedBudget to categories
                // but we reverted that approach. Adding column anyway for compatibility.
                db.execSQL("ALTER TABLE categories ADD COLUMN preallocatedBudget REAL NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 8 to 9: Add budget_preallocations table (monthly preallocations)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_preallocations (
                        yearMonth TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        PRIMARY KEY(yearMonth, categoryId),
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_budget_preallocations_categoryId ON budget_preallocations(categoryId)")
            }
        }

        // Migration from version 9 to 10: Add category_budgets table (per-category budget limits)
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS category_budgets (
                        yearMonth TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        PRIMARY KEY(yearMonth, categoryId),
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_category_budgets_categoryId ON category_budgets(categoryId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            insertDefaultCategories(db)
                            insertDefaultSharingApps(db)
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            insertDefaultCategories(db)
                            insertDefaultSharingApps(db)
                        }

                        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
                            DefaultCategories.list.forEach { category ->
                                db.execSQL(
                                    "INSERT INTO categories (name, emoji, parentId, isDefault) VALUES (?, ?, ?, ?)",
                                    arrayOf(category.name, category.emoji, category.parentId, if (category.isDefault) 1 else 0)
                                )
                            }
                        }

                        private fun insertDefaultSharingApps(db: SupportSQLiteDatabase) {
                            val defaultApps = listOf(
                                Triple("Splitwise", "com.Splitwise.SplitwiseMobile", 0),
                                Triple("PhonePe", "com.phonepe.app", 1)
                            )
                            defaultApps.forEach { (name, packageName, order) ->
                                db.execSQL(
                                    "INSERT INTO sharing_apps (name, packageName, isEnabled, sortOrder) VALUES (?, ?, 1, ?)",
                                    arrayOf(name, packageName, order)
                                )
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
