package com.bro.broreminder.reminder;

public class Reminder {
    public String id;

    public String title;
    public String text;
    public String ttsMessage;
    public String audioPath;
    public long audioDuration;
    public String imagePath;
    public boolean playAlarm = true;
    public String[] times;
    public boolean[] days; // length 7, starting with Monday
    public boolean enabled;
    public boolean[] timeEnabled;

    public boolean yesNoPrompt = true;

    // Whether a declined reminder should be rescheduled 15 minutes later
    public boolean repeatOnNo = true;

    // Automatically log as "Unanswered" if no response was given
    public boolean autoAnswer = false;

    // If true, this reminder can preempt others when triggered
    public boolean priority = false;

    // Repeat every X hours if set
    public Integer repeatIntervalHours;

    // Flag for one-time reminders
    public boolean oneTime = false;

    // Absolute trigger time for one-time reminders (epoch millis)
    public long oneTimeDate = 0;

}