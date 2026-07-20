package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE classId = :classId ORDER BY timestamp ASC")
    suspend fun getSessionsForClass(classId: Int): List<WorkoutSession>
}

@Dao
interface ProgramExerciseDao {
    @Query("SELECT * FROM program_exercises ORDER BY orderIndex ASC")
    fun getProgramFlow(): Flow<List<ProgramExercise>>

    @Query("SELECT * FROM program_exercises ORDER BY orderIndex ASC")
    suspend fun getProgramDirect(): List<ProgramExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProgramExercise>)

    @Query("DELETE FROM program_exercises")
    suspend fun clear()
}

@Dao
interface WorkoutClassDao {
    @Query("SELECT * FROM workout_classes ORDER BY completedAt DESC")
    fun getClassesFlow(): Flow<List<WorkoutClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(c: WorkoutClass): Long
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY points DESC")
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaderboardEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LeaderboardEntry>)

    @Query("UPDATE leaderboard SET points = :points WHERE isCurrentUser = 1")
    suspend fun updateCurrentUserPoints(points: Int)
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<Badge>>

    @Query("UPDATE badges SET unlockedAt = :timestamp WHERE id = :badgeId")
    suspend fun unlockBadge(badgeId: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)
}
