package com.example.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AppDatabase private constructor(sqlDb: DrawShareDb) {
    private val dao = DrawingDao(sqlDb)

    fun drawingDao(): DrawingDao = dao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val driver = AndroidSqliteDriver(
                    schema = DrawShareDb.Schema,
                    context = context.applicationContext,
                    name = "drawshare.db",
                )
                // No column adapters needed — Boolean columns stored as INTEGER 0/1
                // and converted at the DAO boundary.
                AppDatabase(DrawShareDb(driver)).also { INSTANCE = it }
            }
        }
    }
}
