package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FitRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var sync: NoopSync
    private var fakeClock: Long = 0L

    private fun epochDay(): Long = fakeClock / 86_400_000L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
        sync = NoopSync()
        fakeClock = 1_000_000_000_000L
        repo()
    }

    private fun repo(): FitRepository {
        return FitRepository(db, sync, clockMillis = { fakeClock })
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `checkAndSeedDatabase creates seeded data on first run`() = runTest {
        repo().checkAndSeedDatabase()

        val profile = db.userProfileDao().getProfileDirect()
        assertNotNull(profile)
        assertEquals(false, profile?.isLoggedIn)
        assertEquals("", profile?.name)

        val badges = db.badgeDao().getAllBadges().firstOrNull() ?: emptyList()
        assertEquals(4, badges.size)
        assertTrue(badges.any { it.id == "first_step" })
        assertTrue(badges.any { it.id == "form_master" })
        assertTrue(badges.any { it.id == "iron_will" })
        assertTrue(badges.any { it.id == "xp_milestone" })
        badges.forEach { assertNull(it.unlockedAt) }

        val leaderboard = db.leaderboardDao().getLeaderboard().firstOrNull() ?: emptyList()
        assertEquals(6, leaderboard.size)
        assertTrue(leaderboard.any { it.name == "User (You)" && it.isCurrentUser })
    }

    @Test
    fun `checkAndSeedDatabase is idempotent`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        r.checkAndSeedDatabase()

        val badges = db.badgeDao().getAllBadges().firstOrNull() ?: emptyList()
        assertEquals(4, badges.size)

        val leaderboard = db.leaderboardDao().getLeaderboard().firstOrNull() ?: emptyList()
        assertEquals(6, leaderboard.size)
    }

    @Test
    fun `addWorkoutSession calculates points correctly`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Squats", 120, 10, 80.0, "Great form!")

        val profile = db.userProfileDao().getProfileDirect()
        val expectedPoints = ((10 * (80.0 / 100.0) * 15).toInt() + 10)
        assertEquals(expectedPoints, profile?.experiencePoints)
    }

    @Test
    fun `points floor for zero form score`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Squats", 120, 1, 0.0, "")

        val profile = db.userProfileDao().getProfileDirect()
        val expectedPoints = 10
        assertEquals(expectedPoints, profile?.experiencePoints)
    }

    @Test
    fun `streak starts at 1 for first session`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Pushups", 60, 5, 70.0, "")

        val profile = db.userProfileDao().getProfileDirect()
        assertEquals(1, profile?.streakDays)
        assertNotNull(profile?.lastWorkoutDate)
    }

    @Test
    fun `streak preserves on same-day session`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Pushups", 60, 5, 70.0, "")
        r.addWorkoutSession("Squats", 120, 10, 90.0, "")

        val profile = db.userProfileDao().getProfileDirect()
        assertEquals(1, profile?.streakDays)
    }

    @Test
    fun `streak increments on consecutive day`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val epoch = epochDay()

        fakeClock = epoch * 86_400_000L
        r.addWorkoutSession("Pushups", 60, 5, 70.0, "")

        fakeClock = (epoch + 1L) * 86_400_000L
        r.addWorkoutSession("Squats", 120, 10, 90.0, "")

        val profile = db.userProfileDao().getProfileDirect()
        assertEquals(2, profile?.streakDays)
    }

    @Test
    fun `streak resets to 1 after gap`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val epoch = epochDay()

        fakeClock = epoch * 86_400_000L
        r.addWorkoutSession("Pushups", 60, 5, 70.0, "")

        fakeClock = (epoch + 2L) * 86_400_000L
        r.addWorkoutSession("Squats", 120, 10, 90.0, "")

        val profile = db.userProfileDao().getProfileDirect()
        assertEquals(1, profile?.streakDays)
    }

    @Test
    fun `streak reaches 3 across 3 consecutive days`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val epoch = epochDay()

        fakeClock = epoch * 86_400_000L
        r.addWorkoutSession("D1", 60, 5, 70.0, "")
        fakeClock = (epoch + 1L) * 86_400_000L
        r.addWorkoutSession("D2", 60, 5, 80.0, "")
        fakeClock = (epoch + 2L) * 86_400_000L
        r.addWorkoutSession("D3", 60, 5, 90.0, "")

        val profile = db.userProfileDao().getProfileDirect()
        assertEquals(3, profile?.streakDays)
    }

    @Test
    fun `first_step badge unlocks on first session only`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Pushups", 60, 5, 70.0, "")
        val badges1 = db.badgeDao().getAllBadges().firstOrNull() ?: emptyList()
        assertNotNull(badges1.find { it.id == "first_step" }?.unlockedAt)

        r.addWorkoutSession("Squats", 120, 10, 90.0, "")
        val badges2 = db.badgeDao().getAllBadges().firstOrNull() ?: emptyList()
        assertNotNull(badges2.find { it.id == "first_step" }?.unlockedAt)
    }

    @Test
    fun `form_master badge unlocks on score 90 or above`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Squats", 120, 10, 95.0, "")
        val formMaster = db.badgeDao().getAllBadges().firstOrNull()?.first { it.id == "form_master" }
        assertNotNull(formMaster?.unlockedAt)
    }

    @Test
    fun `form_master stays locked on low score`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Squats", 120, 10, 50.0, "")
        val formMaster = db.badgeDao().getAllBadges().firstOrNull()?.first { it.id == "form_master" }
        assertNull(formMaster?.unlockedAt)
    }

    @Test
    fun `iron_will unlocks after 3 sessions`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val epoch = epochDay()

        fakeClock = epoch * 86_400_000L
        r.addWorkoutSession("S1", 60, 5, 70.0, "")
        fakeClock = (epoch + 1L) * 86_400_000L
        r.addWorkoutSession("S2", 60, 5, 70.0, "")
        fakeClock = (epoch + 2L) * 86_400_000L
        r.addWorkoutSession("S3", 60, 5, 70.0, "")

        val ironWill = db.badgeDao().getAllBadges().firstOrNull()?.first { it.id == "iron_will" }
        assertNotNull(ironWill?.unlockedAt)
    }

    @Test
    fun `iron_will stays locked before 3 sessions`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("S1", 60, 5, 70.0, "")
        r.addWorkoutSession("S2", 60, 5, 70.0, "")

        val badges = db.badgeDao().getAllBadges().firstOrNull() ?: emptyList()
        assertNull(badges.find { it.id == "iron_will" }?.unlockedAt)
    }

    @Test
    fun `xp_milestone unlocks on streak of 2`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val epoch = epochDay()

        fakeClock = epoch * 86_400_000L
        r.addWorkoutSession("S1", 60, 5, 70.0, "")
        fakeClock = (epoch + 1L) * 86_400_000L
        r.addWorkoutSession("S2", 60, 5, 70.0, "")

        val xp = db.badgeDao().getAllBadges().firstOrNull()?.first { it.id == "xp_milestone" }
        assertNotNull(xp?.unlockedAt)
    }

    @Test
    fun `xp_milestone stays locked with streak of 1`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("S1", 60, 5, 70.0, "")

        val xp = db.badgeDao().getAllBadges().firstOrNull()?.first { it.id == "xp_milestone" }
        assertNull(xp?.unlockedAt)
    }

    @Test
    fun `addWorkoutSession updates leaderboard points`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val pts = ((10 * (80.0 / 100.0) * 15).toInt() + 10)

        r.addWorkoutSession("Squats", 120, 10, 80.0, "")

        val lb = db.leaderboardDao().getLeaderboard().firstOrNull() ?: emptyList()
        val user = lb.find { it.isCurrentUser }
        assertEquals(pts, user?.points)
    }

    @Test
    fun `saveProfile persists and retrieves profile`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val profile = UserProfile(
            name = "TestUser",
            goals = "Gain Muscle",
            height = 180.0,
            weight = 75.0,
            weeklyGoalDays = 5
        )

        r.saveProfile(profile)

        val loaded = db.userProfileDao().getProfileDirect()
        assertNotNull(loaded)
        assertEquals("TestUser", loaded?.name)
        assertEquals("Gain Muscle", loaded?.goals)
        assertEquals(180.0, loaded!!.height, 0.01)
        assertEquals(75.0, loaded.weight, 0.01)
        assertEquals(5, loaded.weeklyGoalDays)
    }

    @Test
    fun `onSignedIn merges remote profile when available`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val uid = "test-uid-123"
        val remoteProfile = UserProfile(
            goals = "Lose Weight",
            height = 170.0,
            weight = 80.0,
            weeklyGoalDays = 4,
            experiencePoints = 500,
            streakDays = 7,
            lastWorkoutDate = 1000000L,
            workoutProgram = "Custom program"
        )
        db.userProfileDao().insertOrUpdate(remoteProfile)
        sync.remoteProfile = UserProfile(
            id = 1,
            uid = uid,
            name = "Alice",
            email = "alice@test.com",
            isLoggedIn = true,
            goals = "Lose Weight",
            height = 170.0,
            weight = 80.0,
            weeklyGoalDays = 4,
            experiencePoints = 500,
            streakDays = 7,
            lastWorkoutDate = 1000000L,
            workoutProgram = "Custom program"
        )

        r.onSignedIn(uid, "Alice", "alice@test.com")

        val merged = db.userProfileDao().getProfileDirect()
        assertEquals("test-uid-123", merged?.uid)
        assertEquals(true, merged?.isLoggedIn)
        assertEquals("Alice", merged?.name)
        assertEquals("Lose Weight", merged?.goals)
        assertEquals(500, merged?.experiencePoints)
        assertEquals(7, merged?.streakDays)
        assertEquals(1000000L, merged?.lastWorkoutDate)
    }

    @Test
    fun `onSignedIn keeps local when no remote`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val local = UserProfile(
            name = "Bob",
            email = "bob@local.com"
        )
        db.userProfileDao().insertOrUpdate(local)

        r.onSignedIn("uid-bob", "Bob", "bob@test.com")

        val merged = db.userProfileDao().getProfileDirect()
        assertEquals("uid-bob", merged?.uid)
        assertEquals(true, merged?.isLoggedIn)
        assertEquals("Bob", merged?.name)
        assertEquals("bob@local.com", merged?.email)
    }

    @Test
    fun `addWorkoutSession saves session with all fields`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addWorkoutSession("Burpees", 90, 15, 75.0, "Good pace", classId = 42, sets = 3)

        val sessions = db.workoutSessionDao().getAllSessions().firstOrNull() ?: emptyList()
        assertEquals(1, sessions.size)
        val s = sessions[0]
        assertEquals("Burpees", s.exerciseName)
        assertEquals(90, s.durationSeconds)
        assertEquals(15, s.reps)
        assertEquals(75.0, s.formScore, 0.01)
        assertEquals(42, s.classId)
        assertEquals(3, s.sets)
    }

    @Test
    fun `chat message roundtrip`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addChatMessage("user", "Hello coach")
        r.addChatMessage("coach", "Hello! Ready to train?")
        r.addChatMessage("user", "Let's do squats")

        val messages = r.getChatMessages()
        assertEquals(3, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("Hello coach", messages[0].content)
        assertEquals("coach", messages[1].role)
    }

    @Test
    fun `clearChatMessages empties the table`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        r.addChatMessage("user", "Hello")
        r.addChatMessage("coach", "Hi")
        r.clearChatMessages()

        val messages = r.getChatMessages()
        assertEquals(0, messages.size)
    }

    @Test
    fun `saveProgram replaces previous program`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        val exercises = listOf(
            ProgramExercise(orderIndex = 0, name = "Squats", targetSets = 3, targetReps = 10),
            ProgramExercise(orderIndex = 1, name = "Pushups", targetSets = 3, targetReps = 12)
        )

        r.saveProgram(exercises)
        val saved = r.getProgram()
        assertEquals(2, saved.size)
        assertEquals("Squats", saved[0].name)

        val exercises2 = listOf(
            ProgramExercise(orderIndex = 0, name = "Planks", targetSets = 3, targetReps = 5)
        )
        r.saveProgram(exercises2)
        val saved2 = r.getProgram()
        assertEquals(1, saved2.size)
        assertEquals("Planks", saved2[0].name)
    }

    @Test
    fun `syncChatMessagesFromCloud seeds only when local empty`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        r.addChatMessage("user", "existing message")

        r.syncChatMessagesFromCloud("uid")
        val messages = r.getChatMessages()
        assertEquals(1, messages.size)
        assertEquals("existing message", messages[0].content)
    }

    @Test
    fun `syncChatMessagesFromCloud pulls from cloud when local empty`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()
        sync.cloudMessages = listOf(
            ChatMessageEntity(id = 0, role = "user", content = "cloud msg 1", timestamp = 100L),
            ChatMessageEntity(id = 0, role = "coach", content = "cloud reply", timestamp = 200L)
        )

        r.syncChatMessagesFromCloud("uid")
        val messages = r.getChatMessages()
        assertEquals(2, messages.size)
        assertEquals("cloud msg 1", messages[0].content)
    }

    @Test
    fun `userProfile flow emits after seed`() = runTest {
        val r = repo()
        r.checkAndSeedDatabase()

        val profile = r.userProfile.firstOrNull()
        assertNotNull(profile)
        assertEquals(false, profile?.isLoggedIn)
    }
}
