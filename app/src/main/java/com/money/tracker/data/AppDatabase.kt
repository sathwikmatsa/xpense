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
import com.money.tracker.data.dao.TagBudgetDao
import com.money.tracker.data.dao.TagDao
import com.money.tracker.data.dao.TransactionDao
import com.money.tracker.data.dao.TransactionTagDao
import com.money.tracker.data.dao.UpiReminderDao
import com.money.tracker.data.entity.Budget
import com.money.tracker.data.entity.BudgetPreallocation
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.CategoryBudget
import com.money.tracker.data.entity.DefaultCategories
import com.money.tracker.data.entity.DefaultTags
import com.money.tracker.data.entity.SharingApp
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.TagBudget
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionTag
import com.money.tracker.data.entity.UpiReminder
import androidx.room.migration.Migration

@Database(
    entities = [Transaction::class, Category::class, Budget::class, UpiReminder::class, SharingApp::class, BudgetPreallocation::class, CategoryBudget::class, Tag::class, TagBudget::class, TransactionTag::class],
    version = 14,
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
    abstract fun tagDao(): TagDao
    abstract fun tagBudgetDao(): TagBudgetDao
    abstract fun transactionTagDao(): TransactionTagDao

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

        // Migration from version 10 to 11: Add tags table, tag_budgets table, and tagId to transactions
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create tags table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        color INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create tag_budgets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tag_budgets (
                        yearMonth TEXT NOT NULL,
                        tagId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        PRIMARY KEY(yearMonth, tagId),
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tag_budgets_tagId ON tag_budgets(tagId)")

                // Insert default tags first (before recreating transactions table with FK)
                db.execSQL("INSERT INTO tags (name, emoji, color) VALUES ('Travel', '✈️', ${0xFF2196F3})")
                db.execSQL("INSERT INTO tags (name, emoji, color) VALUES ('Party', '🎉', ${0xFFFF9800})")
                db.execSQL("INSERT INTO tags (name, emoji, color) VALUES ('Gift', '🎁', ${0xFFE91E63})")
                db.execSQL("INSERT INTO tags (name, emoji, color) VALUES ('Holiday', '🎄', ${0xFF4CAF50})")
                db.execSQL("INSERT INTO tags (name, emoji, color) VALUES ('Work Trip', '💼', ${0xFF607D8B})")

                // Recreate transactions table with tagId foreign key (SQLite doesn't support adding FK via ALTER)
                db.execSQL("""
                    CREATE TABLE transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        merchant TEXT,
                        categoryId INTEGER,
                        source TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        rawMessage TEXT,
                        isManual INTEGER NOT NULL,
                        isPending INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isSplit INTEGER NOT NULL,
                        splitNumerator INTEGER NOT NULL,
                        splitDenominator INTEGER NOT NULL,
                        totalAmount REAL NOT NULL,
                        splitSynced INTEGER NOT NULL,
                        tagId INTEGER,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE SET NULL
                    )
                """.trimIndent())

                // Copy data from old table
                db.execSQL("""
                    INSERT INTO transactions_new (id, amount, type, description, merchant, categoryId, source, date, rawMessage, isManual, isPending, createdAt, isSplit, splitNumerator, splitDenominator, totalAmount, splitSynced, tagId)
                    SELECT id, amount, type, description, merchant, categoryId, source, date, rawMessage, isManual, isPending, createdAt, isSplit, splitNumerator, splitDenominator, totalAmount, splitSynced, NULL
                    FROM transactions
                """.trimIndent())

                // Drop old table and rename new one
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                // Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_tagId ON transactions(tagId)")
            }
        }

        // Migration from version 11 to 12: Add excludeFromExpense to categories and Settlement category
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add excludeFromExpense column to categories
                db.execSQL("ALTER TABLE categories ADD COLUMN excludeFromExpense INTEGER NOT NULL DEFAULT 0")

                // Insert Settlement category
                db.execSQL("INSERT INTO categories (name, emoji, parentId, isDefault, preallocatedBudget, excludeFromExpense) VALUES ('Settlement', '🤝', NULL, 1, 0, 1)")
            }
        }

        // Migration from version 12 to 13: Add expenseDate to transactions
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add expenseDate column (nullable, defaults to null which means use date)
                db.execSQL("ALTER TABLE transactions ADD COLUMN expenseDate INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_expenseDate ON transactions(expenseDate)")
            }
        }

        // Migration from version 13 to 14: Add transaction_tags junction table for multi-tag support
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create transaction_tags junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_tags (
                        transactionId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY(transactionId, tagId),
                        FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE,
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_transactionId ON transaction_tags(transactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_tagId ON transaction_tags(tagId)")

                // Migrate existing tagId data to the junction table
                db.execSQL("""
                    INSERT INTO transaction_tags (transactionId, tagId)
                    SELECT id, tagId FROM transactions WHERE tagId IS NOT NULL
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            insertDefaultCategories(db)
                            insertDefaultSharingApps(db)
                            insertDefaultTags(db)
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            insertDefaultCategories(db)
                            insertDefaultSharingApps(db)
                            insertDefaultTags(db)
                        }

                        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
                            DefaultCategories.list.forEach { category ->
                                db.execSQL(
                                    "INSERT INTO categories (name, emoji, parentId, isDefault, preallocatedBudget, excludeFromExpense) VALUES (?, ?, ?, ?, 0, ?)",
                                    arrayOf(category.name, category.emoji, category.parentId, if (category.isDefault) 1 else 0, if (category.excludeFromExpense) 1 else 0)
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

                        private fun insertDefaultTags(db: SupportSQLiteDatabase) {
                            DefaultTags.list.forEach { tag ->
                                db.execSQL(
                                    "INSERT INTO tags (name, emoji, color) VALUES (?, ?, ?)",
                                    arrayOf(tag.name, tag.emoji, tag.color)
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
