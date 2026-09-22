package za.co.kinplus.app.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.LocalPhotoDao
import za.co.kinplus.app.data.local.LocalPhotoEntity
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profile pictures — for the signed-in user and for a circle's group photo.
 * Neither the users API nor the circles API has an upload endpoint, so a
 * picked photo is copied into this app's own files directory and its path
 * kept on-device only; a fresh install or a different device falls back to
 * the default initials avatar, same as before a photo was ever set.
 */
@Singleton
class LocalPhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LocalPhotoDao,
    private val authManager: AuthManager
) {
    fun userPhotoPath(): Flow<String?> = dao.observe(userKey()).map { it?.path }

    fun circlePhotoPath(circleId: String): Flow<String?> = dao.observe(circleKey(circleId)).map { it?.path }

    suspend fun setUserPhoto(uri: Uri) = save(userKey(), uri)

    suspend fun setCirclePhoto(circleId: String, uri: Uri) = save(circleKey(circleId), uri)

    private suspend fun save(key: String, uri: Uri) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        dao.upsert(LocalPhotoEntity(id = key, path = file.absolutePath))
    }

    private fun userKey() = "user:${authManager.currentUid}"
    private fun circleKey(circleId: String) = "circle:$circleId"
}
