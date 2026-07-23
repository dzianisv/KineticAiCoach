package com.example.data

interface CloudSync {
    suspend fun pushProfile(uid: String, profile: UserProfile)
    suspend fun pushSession(uid: String, session: WorkoutSession)
    suspend fun pushProgram(uid: String, exercises: List<ProgramExercise>)
    suspend fun pushClass(uid: String, c: WorkoutClass)
    suspend fun pushMessage(uid: String, message: ChatMessageEntity)
    suspend fun pullMessages(uid: String): List<ChatMessageEntity>
    suspend fun pullProfile(uid: String): UserProfile?
}
