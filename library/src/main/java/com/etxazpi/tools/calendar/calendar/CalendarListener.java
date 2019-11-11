package com.etxazpi.tools.calendar.calendar;

import java.util.Calendar;

public interface CalendarListener {

    void onDaySeledted(Calendar date);

    void onPreviousMonth(Calendar date);

    void onNextMonth(Calendar date);
}
