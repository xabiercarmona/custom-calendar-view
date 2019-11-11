#Custom CalendarView
This custom CalendarView is an alternative to Android's CalendarView.
It provides:
- Event icons
- Customizable first day of the week
- Customizable colors
- Day selection and month change listeners
- Full control of view updates, ideal for seting up monthly event pagination.

## Sample
A sample application is available alongside the library

##Compatibility
The library requires minSdkVersion 15.
## Usage
The custom CalendarView can be configured with the following attributes:
- **firstDayOfTheWeek**: The first day of the week acording to Calendar. Default is Calendar.SUNDAY
- **useShortWeeks**: True to show the calendar weekdays with 1 letter, or false to show with 3. Default shows with 3.
- **onlyEventsClickable**: True to make only the days which have events clickable. Default is true.
- **autoUpdateViews**: True to manualy handle the calendar view updates by calling updateViews() or animatedUpdateViews(). False to let the calendar handle them. Default is true.
- **displaySelectLastEvent**: True to automatically go to the most recent event after loading the view. False to show the current month. Default is false.
- **displayOnlyPastEvents**: Works only if displaySelectLastEvent is true. True to limit displaySelectLastEvent to past events. Default is false.
- **eventColor**: Changes the event dot color.
- **selectedColor**: Changes the selected day circle color.
- **todayColor**: Changes the current day ring color:

For example:
```xml
<.CalendarView
        android:id="@+id/calendarView"
        app:firstDayOfTheWeek="2"
        app:displayOnlyPastEvents="true"
        app:displaySelectLastEvent="true"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
```

## Setup
#### Gradle
Add the jitpack repository to the general build.gradle:
```gradle
repositories {
	    maven { url "https://jitpack.io" }
}
```
Add the library dependency to the application build.gradle:
```gradle
implementation ''
```