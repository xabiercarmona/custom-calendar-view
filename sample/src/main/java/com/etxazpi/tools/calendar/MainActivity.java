package com.etxazpi.tools.calendar;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.etxazip.tools.calendar.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CalendarListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        calendarView = findViewById(R.id.calendarView);

        List<CalendarEvent> eventList = new ArrayList<>();
        Calendar event1 = Calendar.getInstance();
        event1.set(Calendar.DAY_OF_MONTH, 18);
        event1.set(Calendar.MONTH, 10);
        eventList.add(new CalendarEvent(event1));

        calendarView.updateViews();
        calendarView.setCalendarListener(this);
        calendarView.setEvents(eventList);


        Log.d(TAG, "onCreate: " + calendarView.getDate().getTime());

    }

    @Override
    public void onDaySeledted(Calendar date) {
        Log.d(TAG, "onDaySeledted: " + date.getTime());
        calendarView.updateViews();
    }

    @Override
    public void onPreviousMonth(Calendar date) {
        Log.d(TAG, "onPreviousMonth: " + date.getTime());
        calendarView.animatedUpdateViews();
    }

    @Override
    public void onNextMonth(Calendar date) {
        Log.d(TAG, "onNextMonth: " + date.getTime());
        calendarView.animatedUpdateViews();
    }
}
