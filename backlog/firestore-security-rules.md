# Firestore Security Rules Hardening

**Stable slug:** `firestore-security-rules`

**Rationale:** The existing firestore.rules allowed any authenticated user read/write access to their own uid subtree but performed zero validation on written data fields — no type checks, no required-field enforcement, no delete protection. A malicious or compromised client could write arbitrary-shaped data (string where number expected, null for required fields, oversized documents), corrupting downstream reads in FirestoreSync.kt and TrialManager.kt. With monetization live (subscription paywall) and user data (profile, streak, XP, messages) being the core product value, schema enforcement is a security and data-integrity prerequisite before scaling beyond 0 users.

**Status:** Done

**Acceptance criteria:**
- [x] `users/{uid}` create validates types: name/email/goals/workoutProgram (string), height/weight/weeklyGoalDays/experiencePoints/streakDays/trialStartedAt (number), lastWorkoutDate (number or null)
- [x] `users/{uid}` update validates individual fields with `"field" in request.resource.data` guard (additive — never blocks adding new fields)
- [x] `users/{uid}/workoutSessions/{sessionId}` create validates: exerciseName/feedback (string), durationSeconds/reps/formScore/pointsEarned/timestamp (number)
- [x] `users/{uid}/program/{docId}` create validates: exercises is list
- [x] `users/{uid}/classes/{classId}` create validates: startedAt/completedAt/exerciseCount/totalReps/avgFormScore/totalPoints (number)
- [x] `users/{uid}/messages/{messageId}` create validates: role/content (string), timestamp (number)
- [x] All subcollections allow update without field validation (merge-safe)
- [x] `delete` denied on all document paths
- [x] Rules syntax follows Firestore Security Rules language v2

**Evidence:**
- File: `firestore.rules`
- Manual syntax review confirms all `is`, `in`, `!=`, `==` operators used correctly
- No unsupported function calls or parameterized type operators
- Rules follow exact data shape from FirestoreSync.kt (profileToMap, sessionToMap, classToMap, messageToMap) and TrialManager.kt (trialStartedAt)
