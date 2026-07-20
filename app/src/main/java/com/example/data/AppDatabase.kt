package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfile::class,
        WorkoutSession::class,
        LeaderboardEntry::class,
        Badge::class,
        ProgramExercise::class,
        WorkoutClass::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun badgeDao(): BadgeDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun workoutClassDao(): WorkoutClassDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS program_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, orderIndex INTEGER NOT NULL, name TEXT NOT NULL, targetSets INTEGER NOT NULL DEFAULT 3, targetReps INTEGER NOT NULL DEFAULT 12, restSeconds INTEGER NOT NULL DEFAULT 30, notes TEXT NOT NULL DEFAULT '')"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_classes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startedAt INTEGER NOT NULL, completedAt INTEGER NOT NULL, exerciseCount INTEGER NOT NULL, totalReps INTEGER NOT NULL, avgFormScore REAL NOT NULL, totalPoints INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN classId INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN sets INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_classes ADD COLUMN sets INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_coach_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
