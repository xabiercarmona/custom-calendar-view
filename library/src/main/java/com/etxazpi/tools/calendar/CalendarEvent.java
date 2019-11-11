package com.etxazpi.tools.calendar;

import java.util.Calendar;

public class CalendarEvent {

    private Calendar eventDate;

    public CalendarEvent() {

    }

    public CalendarEvent(Calendar eventDate) {
        this.eventDate = eventDate;
    }

    public Calendar getEventDate() {
        return eventDate;
    }

    public void setEventDate(Calendar eventDate) {
        this.eventDate = eventDate;
    }
}
