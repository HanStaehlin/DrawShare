package com.example.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver

class AppDatabase private constructor(sqlDb: DrawShareDb) {
    private val dao = DrawingDao(sqlDb)

    fun drawingDao(): DrawingDao = dao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val driver = NativeSqliteDriver(DrawShareDb.Schema, "drawshare.db")
                AppDatabase(DrawShareDb(driver)).also { INSTANCE = it }
            }
        }
    }
}
