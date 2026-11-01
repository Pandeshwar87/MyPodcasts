package com.example.mypodcasts.data.local

import androidx.room.*
import com.example.mypodcasts.data.model.PlayList
import com.example.mypodcasts.data.model.PlayListWithTracks
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayListDao {
    @Query("SELECT * FROM playlists ORDER BY position ASC, updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlayList>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY position ASC, updatedAt DESC")
    fun getAllPlaylistsWithTracks(): Flow<List<PlayListWithTracks>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithTracks(playlistId: Long): Flow<PlayListWithTracks?>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlayList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlayList): Long

    @Update
    suspend fun updatePlaylist(playlist: PlayList)

    @Update
    suspend fun updatePlaylists(playlists: List<PlayList>)

    @Delete
    suspend fun deletePlaylist(playlist: PlayList)
}
