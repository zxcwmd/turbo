package com.omnisound.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "listening_events")
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val listenedRatio: Float,
    val skipped: Boolean,
    val liked: Boolean,
    val timestamp: Long
)

@Dao interface ListeningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(event: ListeningEventEntity)
    @Query("SELECT * FROM listening_events ORDER BY timestamp DESC LIMIT 500") fun observeRecent(): Flow<List<ListeningEventEntity>>
    @Query("DELETE FROM listening_events WHERE timestamp < :before") suspend fun prune(before: Long)
}

@Database(entities = [ListeningEventEntity::class], version = 1, exportSchema = false)
abstract class ListeningDatabase : RoomDatabase() { abstract fun listeningDao(): ListeningDao }
