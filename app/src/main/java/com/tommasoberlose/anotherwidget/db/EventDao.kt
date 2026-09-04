package com.tommasoberlose.anotherwidget.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tommasoberlose.anotherwidget.models.Event

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE eventID = :eventId LIMIT 1")
    fun findByEventId(eventId: Long): Event?

    @Query("SELECT * FROM events WHERE endDate > :from")
    fun find(from: Long): List<Event>

    @Query("SELECT * FROM events WHERE endDate > :from AND startDate <= :to")
    fun find(from: Long, to: Long): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(events: List<Event>)

    @Query("DELETE FROM events")
    fun deleteAll()
}
