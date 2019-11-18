package com.etxazpi.tools.calendar;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarView extends LinearLayout implements View.OnTouchListener, AnimatorListener {

    public static final String TAG_LAYOUT_DAY = "layoutDay";
    public static final String TAG_TEXT_DAY = "textDay";
    public static final String TAG_LAYOUT_DAYOFTHEMONTH = "layoutDayOfTheMonth";
    public static final String TAG_BACKGROUND_DAYOFTHEMONTH = "backgroundDayOfTheMonth";
    public static final String TAG_TEXT_DAYOFTHEMONTH = "textDayOfTheMonth";
    public static final String TAG_EVENT_DAYOFTHEMONTH = "eventDayOfTheMonth";
    private static final String TAG = CalendarView.class.getSimpleName();
    private static final int DIRECTION_NEXT = 1;
    private static final int DIRECTION_PREVIOUS = -1;
    private static final int MIN_SWIPE_DISTANCE = 100;
    private static final int ANIMATION_DURATION = 50;

    //Parameters
    private boolean useShortWeekDays;
    private boolean onlyEventsClickable;
    private boolean autoUpdate;
    private boolean displaySelectLastEvent;
    private boolean displayOnlyPastEvents;
    private int firstDayOfTheWeek;
    private int eventColor;
    private int selectedColor;
    private int todayColor;


    private View rootView;
    private Calendar currentCalendar = Calendar.getInstance(Locale.getDefault());
    private Calendar calendarInUse = (Calendar)currentCalendar.clone();
    private Calendar selectedDay;
    private Calendar previousCalendar = (Calendar)currentCalendar.clone();
    private boolean isMonthChanged;
    private float downX, upX;
    //Listener
    private CalendarListener calendarListener;

    private List<CalendarEvent> eventList = new ArrayList<>();

    public CalendarView(Context context) {
        super(context);
        init(null);
    }

    public CalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet set) {
        //Check if view is in edit mode
        if (isInEditMode()) {
            return;
        }

        LayoutInflater inflate = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflate.inflate(R.layout.calendar_view, this, true);
        rootView.findViewById(R.id.leftButton).setOnTouchListener(this);
        rootView.findViewById(R.id.rightButton).setOnTouchListener(this);

        TypedArray attrs = getContext().obtainStyledAttributes(set, R.styleable.CalendarView);
        useShortWeekDays = attrs.getBoolean(R.styleable.CalendarView_useShortWeeks, false);
        onlyEventsClickable = attrs.getBoolean(R.styleable.CalendarView_onlyEventsClickable, true);
        autoUpdate = attrs.getBoolean(R.styleable.CalendarView_autoUpdateViews, true);
        displaySelectLastEvent = attrs.getBoolean(R.styleable.CalendarView_displaySelectLastEvent, false);
        displayOnlyPastEvents = attrs.getBoolean(R.styleable.CalendarView_displayOnlyPastEvents, false);
        firstDayOfTheWeek = attrs.getInt(R.styleable.CalendarView_firstDayOfTheWeek, Calendar.SUNDAY);
        eventColor = attrs.getColor(R.styleable.CalendarView_eventColor, ContextCompat.getColor(getContext(), R.color.event_color));
        selectedColor = attrs.getColor(R.styleable.CalendarView_selectedColor, ContextCompat.getColor(getContext(), R.color.selected_day_background));
        todayColor = attrs.getColor(R.styleable.CalendarView_todayColor, getResources().getColor(R.color.current_day_ring));


        if (autoUpdate) {
            updateViews();
        }
    }

    public void animatedUpdateViews() {
        if ((!isSameYearAndMonth(calendarInUse, previousCalendar)) && !isSameDayAndYear(calendarInUse, selectedDay)) {
            startAnimation(getDirection());
        } else {
            updateViews();
        }
    }

    public void updateViews() {
        setupWeekHeader();
        setupHeader();
        setupDays();
        markToday();
        markEvents();
        markSelected();
    }

    private void gotoLastEvent() {
        if (eventList != null) {
            CalendarEvent lastEvent = getLastEvent(displayOnlyPastEvents);
            if(lastEvent!=null) {
                selectDay(lastEvent.getEventDate());
                gotToMonth(lastEvent.getEventDate());
            }
        }
    }

    private CalendarEvent getLastEvent(boolean onlyPastEvents) {
        CalendarEvent winner = null;
        for (CalendarEvent event : eventList) {
            Calendar eventDate = event.getEventDate();
            if (eventDate != null) {
                eventDate.set(Calendar.HOUR_OF_DAY, 0);
                eventDate.set(Calendar.MINUTE, 0);
                eventDate.set(Calendar.SECOND, 0);
                if (winner == null) {
                    if (!onlyPastEvents || (eventDate.compareTo(currentCalendar) <= 0)) {
                        winner = event;
                    }
                } else {
                    if ((winner.getEventDate().compareTo(eventDate) < 0) && (!onlyPastEvents || (eventDate.compareTo(currentCalendar) < 0))) {
                        winner = event;
                    }
                }
            }
        }
        return winner;
    }

    private void gotToMonth(Calendar targetCalendar) {
        previousCalendar.set(Calendar.YEAR, calendarInUse.get(Calendar.YEAR));
        previousCalendar.set(Calendar.MONTH, calendarInUse.get(Calendar.MONTH));

        if(!isSameYearAndMonth(calendarInUse, targetCalendar)){
            isMonthChanged = true;
            calendarInUse.set(Calendar.YEAR, targetCalendar.get(Calendar.YEAR));
            calendarInUse.set(Calendar.MONTH, targetCalendar.get(Calendar.MONTH));
        }

        calendarInUse.set(Calendar.DAY_OF_MONTH, targetCalendar.get(Calendar.DAY_OF_MONTH));

        if (autoUpdate) {
            updateViews();
        }
    }

    private int getDirection() {
        int direction = calendarInUse.compareTo(previousCalendar);
        if (direction > 0) {
            direction = DIRECTION_PREVIOUS;
        } else {
            direction = DIRECTION_NEXT;
        }
        return direction;
    }

    private void startAnimation(int direction) {

        Point size = new Point();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(size);

        AnimatorSet animatorSet = new AnimatorSet();

        final Animator fadeOut = ObjectAnimator
                .ofFloat(rootView, View.ALPHA, 1f, 0f)
                .setDuration(10);

        final Animator fadeIn = ObjectAnimator
                .ofFloat(rootView, View.ALPHA, 0f, 1f)
                .setDuration(10);

        final Animator translate1 = ObjectAnimator
                .ofFloat(rootView, View.TRANSLATION_X, 0, size.x*direction)
                .setDuration(ANIMATION_DURATION);

        final Animator translate2 = ObjectAnimator
                .ofFloat(rootView, View.TRANSLATION_X, size.x*direction, size.x*direction*-1)
                .setDuration(ANIMATION_DURATION);

        final Animator translate3 = ObjectAnimator.
                ofFloat(rootView, View.TRANSLATION_X, size.x*direction*-1, 0f)
                .setDuration(ANIMATION_DURATION);

        translate2.addListener(this);

        animatorSet.playSequentially(
                translate1,
                fadeOut,
                translate2,
                fadeIn,
                translate3
        );


        animatorSet.start();
    }

    private void setupHeader() {
        String dateText = new DateFormatSymbols(Locale.getDefault()).getMonths()[calendarInUse.get(Calendar.MONTH)];
        dateText = dateText.substring(0, 1).toUpperCase() + dateText.subSequence(1, dateText.length());
        TextView dateTitle = rootView.findViewById(R.id.monthText);
        dateTitle.setText(String.format("%s %s", dateText, calendarInUse.get(Calendar.YEAR)));
    }

    private void setupWeekHeader() {
        String dayOfTheWeekString;
        String[] weekDaysArray = new DateFormatSymbols(Locale.getDefault()).getWeekdays();
        int length = weekDaysArray.length;
        for (int i = 1; i < length; i++) {
            TextView dayOfWeek = rootView.findViewWithTag(TAG_TEXT_DAY + getWeekIndex(i));
            dayOfTheWeekString = weekDaysArray[i];
            if (useShortWeekDays) {
                dayOfTheWeekString = dayOfTheWeekString.substring(0, 1).toUpperCase();
            } else {
                dayOfTheWeekString = dayOfTheWeekString.substring(0, 1).toUpperCase() + dayOfTheWeekString.substring(1, 3).toLowerCase();
            }

            dayOfWeek.setText(dayOfTheWeekString);
        }
    }

    private void setupDays() {

        calendarInUse.setTime(calendarInUse.getTime());
        calendarInUse.set(Calendar.DAY_OF_MONTH, 1);
        calendarInUse.setFirstDayOfWeek(firstDayOfTheWeek);

        int firstDay = getWeekIndex(calendarInUse.get(Calendar.DAY_OF_WEEK));
        int numberOfWeeks = calendarInUse.getActualMaximum(Calendar.WEEK_OF_MONTH);
        int numberOfDays = calendarInUse.getActualMaximum(Calendar.DAY_OF_MONTH);

        int topEmptyRows = numberOfWeeks < 5 ? 1 : 0;

        ViewGroup monthContainer = rootView.findViewById(R.id.weeksContainer);


        clearPreviousDays(firstDay, topEmptyRows);
        clearNextDays(firstDay, numberOfDays, numberOfWeeks);

        for (int i = 1; i <= numberOfDays; i++) {
            int rowNum = (i + firstDay - 2) / 7 + topEmptyRows;
            int weekDay = ((i + firstDay - 2) % 7) + 1;
            View weekView = monthContainer.getChildAt(rowNum);

            ViewGroup dayViewGroup = weekView.findViewWithTag(TAG_LAYOUT_DAY + weekDay);
            setupDayLayout(dayViewGroup, i);
        }

        monthContainer.setOnTouchListener(this);

    }

    private void clearPreviousDays(int firstDay, int topEmptyRows) {
        if (isMonthChanged) {
            int maxRowNum = (firstDay - 1) / 7 + topEmptyRows;
            int maxWeekDay = (firstDay - 1) % 7 + 1;
            clearRows(maxRowNum, maxWeekDay, false);
        }
    }

    private void clearNextDays(int firstDay, int lastDay, int totalRows) {
        if (isMonthChanged) {
            int minWeekDay = ((firstDay + lastDay - 2) % 7) + 1;
            clearRows(totalRows, minWeekDay, true);
        }
    }

    private void clearRows(int rowPos, int dayPos, boolean upwards) {
        int startNum = 1;
        int endNum = 42;
        if (!upwards) {
            endNum = (rowPos * 7) + dayPos;
        } else {
            startNum = (rowPos * 7) - (7 - dayPos) + 1;
        }

        ViewGroup monthContainer = rootView.findViewById(R.id.weeksContainer);

        for (int i = startNum; i < endNum; i++) {
            int rowNum = (i - 1) / 7;
            int weekDay = ((i - 1) % 7) + 1;
            View weekView = monthContainer.getChildAt(rowNum);
            ViewGroup dayViewGroup = weekView.findViewWithTag(TAG_LAYOUT_DAY + weekDay);
            dayViewGroup.removeAllViews();
        }
    }

    private void setupDayLayout(ViewGroup viewGroup, int day) {

        viewGroup.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View dayOfTheMonthLayout = inflater.inflate(R.layout.calendar_day_view, null);
        View layoutDayOfTheMonth = dayOfTheMonthLayout.findViewWithTag(TAG_LAYOUT_DAYOFTHEMONTH);
        View backgroundDayOfTheMonth = dayOfTheMonthLayout.findViewWithTag(TAG_BACKGROUND_DAYOFTHEMONTH);
        TextView textDayOfTheMonth = dayOfTheMonthLayout.findViewWithTag(TAG_TEXT_DAYOFTHEMONTH);
        View eventDayOfTheMonth = dayOfTheMonthLayout.findViewWithTag(TAG_EVENT_DAYOFTHEMONTH);

        layoutDayOfTheMonth.setTag(TAG_LAYOUT_DAYOFTHEMONTH + day);
        backgroundDayOfTheMonth.setTag(TAG_BACKGROUND_DAYOFTHEMONTH + day);
        textDayOfTheMonth.setTag(TAG_TEXT_DAYOFTHEMONTH + day);
        textDayOfTheMonth.setText(String.valueOf(day));
        eventDayOfTheMonth.setTag(TAG_EVENT_DAYOFTHEMONTH + day);

        if (!onlyEventsClickable) {
            layoutDayOfTheMonth.setOnTouchListener(this);
        }

        viewGroup.addView(dayOfTheMonthLayout);
    }


    private void markToday() {
        if (isSameYearAndMonth(currentCalendar, calendarInUse)) {
            int todayNumber = currentCalendar.get(Calendar.DAY_OF_MONTH);
            View backgroundDayOfTheMonth = rootView.findViewWithTag(TAG_BACKGROUND_DAYOFTHEMONTH + todayNumber);
            Drawable ring = ContextCompat.getDrawable(getContext(), R.drawable.ring);
            DrawableCompat.setTint(ring, todayColor);
            backgroundDayOfTheMonth.setBackgroundDrawable(ring);
        }

    }

    private void markEvents() {
        for (CalendarEvent event : eventList) {
            Calendar eventDate = event.getEventDate();
            if (isSameYearAndMonth(calendarInUse, eventDate)) {
                int dayNumber = eventDate.get(Calendar.DAY_OF_MONTH);
                View layoutDayOfTheMonth = rootView.findViewWithTag(TAG_LAYOUT_DAYOFTHEMONTH + dayNumber);
                layoutDayOfTheMonth.setOnTouchListener(this);
                ImageView eventDayOfTheMonth = rootView.findViewWithTag(TAG_EVENT_DAYOFTHEMONTH + dayNumber);
                if (eventDayOfTheMonth.getVisibility() != VISIBLE) {
                    eventDayOfTheMonth.setVisibility(VISIBLE);
                    Drawable dot = eventDayOfTheMonth.getDrawable();
                    DrawableCompat.setTint(dot, eventColor);
                    eventDayOfTheMonth.setImageDrawable(dot);
                }
            }
        }
    }

    private void markSelected() {
        if (isSameYearAndMonth(selectedDay, calendarInUse)) {
            addSelection(selectedDay);
        }
    }

    private boolean isSameYearAndMonth(Calendar calendar1, Calendar calendar2) {
        if (calendar1 == null || calendar2 == null) {
            return false;
        }

        return ((calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) && (calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)));
    }

    private boolean isSameDay(Calendar calendar1, Calendar calendar2) {
        if (calendar1 == null || calendar2 == null) {
            return false;
        }

        return (calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR));
    }

    private boolean isSameDayAndYear(Calendar calendar1, Calendar calendar2) {
        return isSameYearAndMonth(calendar1, calendar2) && isSameDay(calendar1, calendar2);
    }

    private int getWeekIndex(int day) {
        day -= (firstDayOfTheWeek - Calendar.SUNDAY);
        if (day < 1) {
            day += 7;
        }
        return day;
    }


    private void onClick(View v) {
        if (v.getId() == R.id.leftButton) {
            showPreviousMonth();
        } else if (v.getId() == R.id.rightButton) {
            showNextMonth();
        } else {
            ViewGroup layoutDayOfTheMonth = (ViewGroup) v;
            if (layoutDayOfTheMonth.getTag() != null) {

                String dayStr = ((String) layoutDayOfTheMonth.getTag()).substring(TAG_LAYOUT_DAYOFTHEMONTH.length());

                Calendar selectedDayCalendar = (Calendar) calendarInUse.clone();
                selectedDayCalendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dayStr));

                selectDay(selectedDayCalendar);
            }
        }
    }

    private void selectDay(Calendar selectedDayCalendar) {
        removeSelection(selectedDay);
        addSelection(selectedDayCalendar);

        selectedDay = selectedDayCalendar;
        calendarInUse.set(Calendar.YEAR, selectedDay.get(Calendar.YEAR));
        calendarInUse.set(Calendar.MONTH, selectedDay.get(Calendar.MONTH));
        calendarInUse.set(Calendar.DAY_OF_MONTH, selectedDay.get(Calendar.DAY_OF_MONTH));

        if (calendarListener != null) {
            calendarListener.onDaySeledted(calendarInUse);
        }
    }

    private void removeSelection(Calendar day) {
        if (isSameYearAndMonth(calendarInUse, day)) {
            int todayNumber = day.get(Calendar.DAY_OF_MONTH);
            View backgroundDayOfTheMonth = rootView.findViewWithTag(TAG_BACKGROUND_DAYOFTHEMONTH + todayNumber);
            if (isSameDayAndYear(currentCalendar, day)) {
                Drawable ring = ContextCompat.getDrawable(getContext(), R.drawable.ring);
                DrawableCompat.setTint(ring, todayColor);
                backgroundDayOfTheMonth.setBackgroundDrawable(ring);
            } else {
                backgroundDayOfTheMonth.setBackgroundResource(android.R.color.transparent);
            }
        }
    }

    private void addSelection(Calendar day) {
        if (isSameYearAndMonth(calendarInUse, day)) {
            int todayNumber = day.get(Calendar.DAY_OF_MONTH);
            View backgroundDayOfTheMonth = rootView.findViewWithTag(TAG_BACKGROUND_DAYOFTHEMONTH + todayNumber);
            Drawable ring = ContextCompat.getDrawable(getContext(), R.drawable.circle);
            DrawableCompat.setTint(ring, selectedColor);
            backgroundDayOfTheMonth.setBackgroundDrawable(ring);
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                float deltaX = downX - upX;

                if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE) {
                    if (deltaX < 0) {
                        showPreviousMonth();
                        return true;
                    }
                    if (deltaX > 0) {
                        showNextMonth();
                        return true;
                    }
                } else {
                    onClick(v);
                }
            }
        }
        return false;
    }

    private void showPreviousMonth() {
        isMonthChanged = true;

        previousCalendar.set(Calendar.YEAR, calendarInUse.get(Calendar.YEAR));
        previousCalendar.set(Calendar.MONTH, calendarInUse.get(Calendar.MONTH));

        calendarInUse.add(Calendar.MONTH, -1);
        calendarInUse.set(Calendar.DAY_OF_MONTH, 1);

        if (calendarListener != null) {
            calendarListener.onNextMonth(calendarInUse);
        }

        if (autoUpdate) {
            animatedUpdateViews();
        }
    }

    private void showNextMonth() {
        isMonthChanged = true;

        previousCalendar.set(Calendar.YEAR, calendarInUse.get(Calendar.YEAR));
        previousCalendar.set(Calendar.MONTH, calendarInUse.get(Calendar.MONTH));

        calendarInUse.add(Calendar.MONTH, +1);
        calendarInUse.set(Calendar.DAY_OF_MONTH, 1);

        if (calendarListener != null) {
            calendarListener.onPreviousMonth(calendarInUse);
        }

        if (autoUpdate) {
            animatedUpdateViews();
        }
    }


    //PUBLIC METHODS FOR CONFIGURATION

    public Calendar getDate() {
        return calendarInUse;
    }

    public void setDate(Calendar calendar) {
        if (calendar != null)
            calendarInUse = calendar;
        if (autoUpdate) {
            updateViews();
        }
    }

    public void setEvents(List<CalendarEvent> eventList) {
        this.eventList = eventList;
        if (autoUpdate) {
            markEvents();
        }
        if (displaySelectLastEvent) {
            gotoLastEvent();
        }
    }

    public void setCalendarListener(CalendarListener calendarListener) {
        this.calendarListener = calendarListener;
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        updateViews();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
