package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AdvanceDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.CompOffDao
import com.example.data.local.dao.DepartmentDao
import com.example.data.local.dao.EmployeeDao
import com.example.data.local.dao.HolidayDao
import com.example.data.local.dao.LeaveDao
import com.example.data.local.dao.OvertimeDao
import com.example.data.local.dao.SalarySlipDao
import com.example.data.local.entity.AdvanceEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CompOffEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.HolidayEntity
import com.example.data.local.entity.LeaveEntity
import com.example.data.local.entity.OvertimeEntity
import com.example.data.local.entity.SalarySlipEntity
import com.example.data.local.entity.SectionEntity

@Database(
    entities = [
        DepartmentEntity::class,
        SectionEntity::class,
        EmployeeEntity::class,
        AttendanceEntity::class,
        OvertimeEntity::class,
        AdvanceEntity::class,
        LeaveEntity::class,
        HolidayEntity::class,
        CompOffEntity::class,
        SalarySlipEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun overtimeDao(): OvertimeDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun leaveDao(): LeaveDao
    abstract fun holidayDao(): HolidayDao
    abstract fun compOffDao(): CompOffDao
    abstract fun salarySlipDao(): SalarySlipDao
    abstract fun departmentDao(): DepartmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example forward-compatible migration adding notes index or column
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_advances_type` ON `advances` (`type`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendx_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun buildInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
