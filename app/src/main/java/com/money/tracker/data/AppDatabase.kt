package com.money.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.money.tracker.data.dao.BudgetDao
import com.money.tracker.data.dao.CategoryDao
import com.money.tracker.data.dao.TransactionDao
import com.money.tracker.data.entity.Budget
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.DefaultCategories
import com.money.tracker.data.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Transaction::class, Category::class, Budget::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            insertDefaultCategories(db)
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            insertDefaultCategories(db)
                        }

                        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
                            DefaultCategories.list.forEach { category ->
                                db.execSQL(
                                    "INSERT INTO categories (name, emoji, parentId, isDefault) VALUES (?, ?, ?, ?)",
                                    arrayOf(category.name, category.emoji, category.parentId, if (category.isDefault) 1 else 0)
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
