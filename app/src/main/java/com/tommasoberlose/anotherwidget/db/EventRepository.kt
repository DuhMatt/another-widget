package com.tommasoberlose.anotherwidget.db

import android.content.Context
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.applyFilters
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.sortEvents
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.util.Calendar

class EventRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.eventDao()

    fun saveEvents(eventList: List<Event>) {
        database.runInTransaction {
            dao.deleteAll()
            if (eventList.isNotEmpty()) dao.insert(eventList)
        }
    }

    fun clearEvents() {
        dao.deleteAll()
    }

    fun resetNextEventData() {
        Preferences.bulk {
            remove(Preferences::nextEventId)
            remove(Preferences::nextEventName)
            remove(Preferences::nextEventStartDate)
            remove(Preferences::nextEventAllDay)
            remove(Preferences::nextEventLocation)
            remove(Preferences::nextEventEndDate)
            remove(Preferences::nextEventCalendarId)
        }
    }

    fun saveNextEventData(event: Event) {
        Preferences.nextEventId = event.eventID
    }

    fun getNextEvent(): Event? {
        val nextEvent = getEventByEventId(Preferences.nextEventId)
        val now = Calendar.getInstance().timeInMillis
        val limit = getEventWindow(now)
        return if (nextEvent != null && nextEvent.endDate > now && nextEvent.startDate < limit) {
            nextEvent
        } else {
            val events = getEvents()
            if (events.isNotEmpty()) {
                val newNextEvent = events.first()
                saveNextEventData(newNextEvent)
                newNextEvent
            } else {
                resetNextEventData()
                null
            }
        }
    }

    fun getEventByEventId(id: Long): Event? = dao.findByEventId(id)

    fun goToNextEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.eventID == Preferences.nextEventId }
            saveNextEventData(if (index in 0 until eventList.lastIndex) eventList[index + 1] else eventList.first())
        } else {
            resetNextEventData()
        }
        UpdatesReceiver.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun goToPreviousEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.eventID == Preferences.nextEventId }
            saveNextEventData(if (index > 0) eventList[index - 1] else eventList.last())
        } else {
            resetNextEventData()
        }
        UpdatesReceiver.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun getFutureEvents(): List<Event> = dao.find(Calendar.getInstance().timeInMillis).applyFilters().sortEvents()

    private fun getEvents(): List<Event> {
        val now = Calendar.getInstance().timeInMillis
        return dao.find(now, getEventWindow(now)).applyFilters().sortEvents()
    }

    fun getEventsCount(): Int = getEvents().size

    fun close() = Unit

    private fun getEventWindow(now: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            when (Preferences.showUntil) {
                0 -> add(Calendar.HOUR, 3)
                1 -> add(Calendar.HOUR, 6)
                2 -> add(Calendar.HOUR, 12)
                3 -> add(Calendar.DAY_OF_MONTH, 1)
                4 -> add(Calendar.DAY_OF_MONTH, 3)
                5 -> add(Calendar.DAY_OF_MONTH, 7)
                6 -> add(Calendar.MINUTE, 30)
                7 -> add(Calendar.HOUR, 1)
                else -> add(Calendar.HOUR, 6)
            }
        }.timeInMillis
    }
}
