package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    RegularVariableEntity::class,
    HistorySessionEntity::class,
    FunctionPresetEntity::class,
    CompareVariableEntity::class,
    WorkspaceStateEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun calculatorDao(): CalculatorDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "ALTER TABLE workspace_state ADD COLUMN keypressSound INTEGER NOT NULL DEFAULT 1"
        )
      }
    }

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "varcalc_database.db",
        )
          .addMigrations(MIGRATION_1_2)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
