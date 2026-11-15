package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class StudioProjectRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveProject_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioProjectRepository(firestore)
        val projectId = "proj_${UUID.randomUUID().toString().take(8)}"
        val sampleProject = StudioProject(
            id = projectId,
            title = "پوستر تایپوگرافی جمینای",
            canvasWidth = 1080,
            canvasHeight = 1080,
            layers = listOf(
                TextLayer(name = "تیتر اصلی", text = "PixelLab AI", is3dEnabled = true, depth3d = 24)
            )
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveProject(sampleProject) }
        assertTrue("Save should succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        assertEquals(projectId, saveResult.getOrThrow())

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first { list -> list.any { it.id == projectId } }
        }
        assertTrue(emitted.any { it.id == projectId && it.title == "پوستر تایپوگرافی جمینای" })
    }

    @Test
    fun getProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioProjectRepository(firestore)
        val projectId = "proj_${UUID.randomUUID().toString().take(8)}"
        val sampleProject = StudioProject(
            id = projectId,
            title = "پروژه خصوصی آلیس"
        )
        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveProject(sampleProject) }
        assertTrue(saveResult.isSuccess)

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudioProjectRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProjectById(projectId = projectId, targetUserId = aliceUid)
        }
        assertTrue("Cross-user access must fail", crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = StudioProjectRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProjects(aliceUid).first()
            }
            fail("Expected exception for unauthenticated observeProjects")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@pixellab.test"
        const val BOB_EMAIL = "bob@pixellab.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
