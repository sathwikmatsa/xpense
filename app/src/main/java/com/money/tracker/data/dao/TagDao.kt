package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.money.tracker.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Tag>)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): Tag?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Tag?

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getCount(): Int

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsSync(): List<Tag>

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(tag: Tag)
}
