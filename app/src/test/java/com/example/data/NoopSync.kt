package com.example.data

/**
 * In-test [CloudSync] that performs no network I/O. Push operations are no-ops;
 * pull operations return configurable fixtures so tests can exercise the
 * repository's local-vs-remote merge and cloud-seed paths without Firebase.
 */
class NoopSync : CloudSync {
    var remoteProfile: UserProfile? = null
    var cloudMessages: List<ChatMessageEntity> = emptyList()

    override suspend fun pushProfile(uid: String, profile: UserProfile) {}
    override suspend fun pushSession(uid: String, session: WorkoutSession) {}
    override suspend fun pushProgram(uid: String, exercises: List<ProgramExercise>) {}
    override suspend fun pushClass(uid: String, c: WorkoutClass) {}
    override suspend fun pushMessage(uid: String, message: ChatMessageEntity) {}
    override suspend fun pullMessages(uid: String): List<ChatMessageEntity> = cloudMessages
    override suspend fun pullProfile(uid: String): UserProfile? = remoteProfile
}
