package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudioProjectRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProjects(userId: String = requireUserId()): Flow<List<StudioProjectDoc>> = flow {
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioProjectDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserProjects(userId: String = requireUserId()): Result<List<StudioProjectDoc>> {
        val path = "users/$userId/projects"
        return try {
            val snapshot = db.collection("users").document(userId).collection("projects")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val docs = snapshot.toObjects(
                StudioProjectDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(docs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getProjectById(
        projectId: String,
        targetUserId: String = requireUserId()
    ): Result<StudioProjectDoc?> {
        val path = "users/$targetUserId/projects/$projectId"
        return try {
            val doc = db.collection("users").document(targetUserId)
                .collection("projects").document(projectId)
                .get()
                .await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                Result.success(
                    doc.toObject(
                        StudioProjectDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveProject(project: StudioProject): Result<String> {
        val uid = requireUserId()
        val rawId = project.id.ifBlank { UUID.randomUUID().toString() }
        val sanitizedId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "proj_1" }
        val docRef = db.collection("users").document(uid).collection("projects").document(sanitizedId)

        val primaryTextLayer = project.layers.filterIsInstance<TextLayer>().firstOrNull()
        val layerNames = project.layers.map { it.name.take(120) }.take(50)

        return try {
            val existingSnap = try {
                docRef.get().await()
            } catch (e: Exception) {
                null
            }

            if (existingSnap != null && existingSnap.exists()) {
                val updatePayload = mapOf(
                    "title" to project.title.take(120).ifBlank { "پروژه بدون نام" },
                    "canvasWidth" to project.canvasWidth.coerceIn(64, 4096),
                    "canvasHeight" to project.canvasHeight.coerceIn(64, 4096),
                    "backgroundType" to project.background.type.name,
                    "backgroundColorHex" to project.background.primaryColorHex.take(20),
                    "backgroundSecondaryHex" to project.background.secondaryColorHex.take(20),
                    "layerCount" to project.layers.size.coerceIn(0, 100),
                    "layerTitles" to layerNames,
                    "primaryText" to (primaryTextLayer?.text?.take(500) ?: ""),
                    "fontId" to (primaryTextLayer?.fontId?.take(60) ?: StudioFont.LALEZAR.id),
                    "accentColorHex" to (primaryTextLayer?.colorHex?.take(20) ?: "#38BDF8"),
                    "is3dEnabled" to (primaryTextLayer?.is3dEnabled ?: false),
                    "depth3d" to (primaryTextLayer?.depth3d?.coerceIn(0, 100) ?: 0),
                    "curveAngle" to (primaryTextLayer?.curveAngle?.toInt()?.coerceIn(-360, 360) ?: 0),
                    "plpFileName" to project.plpFileName.take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "title" to project.title.take(120).ifBlank { "پروژه بدون نام" },
                    "canvasWidth" to project.canvasWidth.coerceIn(64, 4096),
                    "canvasHeight" to project.canvasHeight.coerceIn(64, 4096),
                    "backgroundType" to project.background.type.name,
                    "backgroundColorHex" to project.background.primaryColorHex.take(20),
                    "backgroundSecondaryHex" to project.background.secondaryColorHex.take(20),
                    "layerCount" to project.layers.size.coerceIn(0, 100),
                    "layerTitles" to layerNames,
                    "primaryText" to (primaryTextLayer?.text?.take(500) ?: ""),
                    "fontId" to (primaryTextLayer?.fontId?.take(60) ?: StudioFont.LALEZAR.id),
                    "accentColorHex" to (primaryTextLayer?.colorHex?.take(20) ?: "#38BDF8"),
                    "is3dEnabled" to (primaryTextLayer?.is3dEnabled ?: false),
                    "depth3d" to (primaryTextLayer?.depth3d?.coerceIn(0, 100) ?: 0),
                    "curveAngle" to (primaryTextLayer?.curveAngle?.toInt()?.coerceIn(-360, 360) ?: 0),
                    "plpFileName" to project.plpFileName.take(160),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(sanitizedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
