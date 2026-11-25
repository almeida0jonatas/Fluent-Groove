package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ArtDesign
import com.example.model.Playlist
import com.example.model.RecentPlayEntry
import com.example.model.Track
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val trackNumber: Int,
    val duration: Int,
    val genre: String,
    val year: Int,
    val addedAt: Long,
    val fileName: String,
    val contentUri: String,
    val coverUri: String?,
    val artColorStart: Long,
    val artColorEnd: Long,
    val artAccent: Long,
    val artPattern: Int,
    val playCount: Int = 0,
) {
    fun toModel(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        trackNumber = trackNumber,
        duration = duration,
        genre = genre,
        year = year,
        addedAt = addedAt,
        fileName = fileName,
        contentUri = contentUri,
        coverUri = coverUri,
        artDesign = ArtDesign(artColorStart, artColorEnd, artAccent, artPattern),
        playCount = playCount,
    )

    companion object {
        fun fromModel(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            artistId = track.artistId,
            album = track.album,
            albumId = track.albumId,
            trackNumber = track.trackNumber,
            duration = track.duration,
            genre = track.genre,
            year = track.year,
            addedAt = track.addedAt,
            fileName = track.fileName,
            contentUri = track.contentUri,
            coverUri = track.coverUri,
            artColorStart = track.artDesign.colorStart,
            artColorEnd = track.artDesign.colorEnd,
            artAccent = track.artDesign.accent,
            artPattern = track.artDesign.pattern,
            playCount = track.playCount,
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val trackIdsCsv: String,
    val createdAt: Long,
) {
    fun toModel(): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        trackIds = if (trackIdsCsv.isBlank()) emptyList() else trackIdsCsv.split("|||"),
        createdAt = createdAt,
    )

    companion object {
        fun fromModel(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            trackIdsCsv = playlist.trackIds.joinToString("|||"),
            createdAt = playlist.createdAt,
        )
    }
}

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey val id: String,
    val trackId: String,
    val playedAt: Long,
) {
    fun toModel(): RecentPlayEntry = RecentPlayEntry(id = id, trackId = trackId, playedAt = playedAt)
}

@Entity(tableName = "artist_image_cache")
data class ArtistCacheEntity(
    @PrimaryKey val artistKey: String,
    val imageUrl: String?,
    val fetchedAt: Long,
)

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val trackId: String,
    val syncedLrc: String?,
    val plainLyrics: String?,
    val isInstrumental: Boolean,
    val source: String,
    val fetchedAt: Long,
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: String)

    @Query("DELETE FROM tracks")
    suspend fun clearTracks()

    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT 40")
    fun observeRecentPlays(): Flow<List<RecentPlayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(entry: RecentPlayEntity)

    @Query("DELETE FROM recent_plays WHERE id NOT IN (SELECT id FROM recent_plays ORDER BY playedAt DESC LIMIT 40)")
    suspend fun trimRecentPlays()

    @Query("DELETE FROM recent_plays")
    suspend fun clearRecentPlays()

    @Query("SELECT * FROM artist_image_cache")
    fun observeArtistCache(): Flow<List<ArtistCacheEntity>>

    @Query("SELECT * FROM artist_image_cache WHERE artistKey = :key LIMIT 1")
    suspend fun getArtistCache(key: String): ArtistCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistCache(cache: ArtistCacheEntity)

    @Query("SELECT * FROM lyrics_cache WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyricsCache(trackId: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyricsCache(cache: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE trackId = :trackId")
    suspend fun deleteLyricsCache(trackId: String)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearLyricsCache()
}

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        RecentPlayEntity::class,
        ArtistCacheEntity::class,
        LyricsCacheEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lyrics_cache` (
                        `trackId` TEXT NOT NULL,
                        `syncedLrc` TEXT,
                        `plainLyrics` TEXT,
                        `isInstrumental` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`trackId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "fluent_groove_db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
