/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * A backup of a file's tags, taken before Metadator wrote it. The pictures are files next to the
 * database ([TagBackupFiles]); a row only names them, so the database stays small.
 */
@Entity(tableName = "tag_backups", indices = [Index("uri"), Index("created_at")])
data class TagBackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** The property map as JSON: `{"TITLE": ["…"], …}`. */
    @ColumnInfo(name = "tags_json") val tagsJson: String,
    /** The pictures' descriptions as JSON; their bytes are files. */
    @ColumnInfo(name = "pictures_json") val picturesJson: String,
)

@Dao
interface TagBackupDao {
    @Insert suspend fun insert(backup: TagBackupEntity): Long

    @Query("SELECT * FROM tag_backups WHERE id = :id") suspend fun get(id: Long): TagBackupEntity?

    @Query("SELECT * FROM tag_backups WHERE uri = :uri ORDER BY created_at DESC")
    suspend fun forUri(uri: String): List<TagBackupEntity>

    @Query("DELETE FROM tag_backups WHERE id = :id") suspend fun delete(id: Long)

    @Query("SELECT id FROM tag_backups ORDER BY created_at DESC, id DESC LIMIT -1 OFFSET :keep")
    suspend fun idsBeyond(keep: Int): List<Long>

    @Query("SELECT id FROM tag_backups") suspend fun allIds(): List<Long>

    @Query("DELETE FROM tag_backups") suspend fun clear()
}

@Database(entities = [TagBackupEntity::class], version = 1, exportSchema = true)
abstract class MetadatorDatabase : RoomDatabase() {
    abstract fun tagBackups(): TagBackupDao

    companion object {
        fun build(context: Context): MetadatorDatabase =
            Room.databaseBuilder(context, MetadatorDatabase::class.java, "metadator.db").build()
    }
}
