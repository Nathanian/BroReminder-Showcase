package com.bro.broreminder.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.provider.MediaStore;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bro.broreminder.R;
import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.utils.ButtonUtils;
import com.bro.broreminder.viewmodels.ReminderViewModel;
import com.bro.broreminder.viewmodels.SharedReminderViewModel;
import com.bro.broreminder.voice.FieldDictationController;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import java.util.ArrayList;
import java.util.Calendar;
import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Fragment used to create or edit a reminder.
 */
public class ReminderCreateFragment extends Fragment {

    private final CheckBox[] dayChecks = new CheckBox[7];
    private CheckBox promptTypeCheck;
    private CheckBox repeatNoCheck;
    private CheckBox autoAnswerCheck;
    private CheckBox priorityCheck;
    private CheckBox alarmCheck;
    @Nullable
    private Reminder editing;
    private Flow timesFlow;
    private ConstraintLayout rootLayout;
    private AppCompatButton addTimeButton;
    private AppCompatButton hideKeyboardButton;
    private AppCompatButton recordButton;
    private AppCompatButton photoButton;
    private View recordInfoGroup;
    private TextView recordInfo;
    private TextView recordingIndicator;
    private Button deleteRecordButton;
    private View photoInfoGroup;
    private Button deletePhotoButton;
    private CheckBox repeatIntervalCheck;
    private TextView repeatIntervalSummary;
    private Integer repeatIntervalHours;
    private EditText ttsField;
    private ScrollView scrollView;
    private AppCompatButton dateButton;
    private Calendar selectedDate;
    private boolean oneTime = false;
    private ReminderViewModel reminderViewModel;
    private MediaRecorder recorder;
    private String audioPath;
    private String imagePath;
    private long recordStart;
    private long audioDuration;
    private static final int REQ_RECORD_AUDIO = 1001;
    private static final int REQ_CAMERA = 1002;

    private FieldDictationController dictationController;

    private static class TimeItem {
        String time;
        AppCompatButton button;
        boolean active;
        boolean generated;
    }
    private final List<TimeItem> timeItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminder_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reminderViewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);
        SharedReminderViewModel shared = new ViewModelProvider(requireActivity()).get(SharedReminderViewModel.class);
        scrollView = view.findViewById(R.id.addReminderScroll);

        EditText titleField = view.findViewById(R.id.reminderTitle);
        EditText textField = view.findViewById(R.id.reminderText);
        ttsField = view.findViewById(R.id.reminderTts);
        dateButton = view.findViewById(R.id.dateButton);
        recordButton = view.findViewById(R.id.recordButton);
        photoButton = view.findViewById(R.id.photoButton);
        recordInfoGroup = view.findViewById(R.id.recordInfoGroup);
        recordInfo = view.findViewById(R.id.recordInfo);
        recordingIndicator = view.findViewById(R.id.recordingIndicator);
        deleteRecordButton = view.findViewById(R.id.deleteRecordButton);
        photoInfoGroup = view.findViewById(R.id.photoInfoGroup);
        deletePhotoButton = view.findViewById(R.id.deletePhotoButton);
        repeatIntervalCheck = view.findViewById(R.id.repeatIntervalCheck);
        repeatIntervalSummary = view.findViewById(R.id.repeatIntervalSummary);
        rootLayout = view.findViewById(R.id.rootLayout);
        timesFlow = view.findViewById(R.id.timesFlow);
        addTimeButton = view.findViewById(R.id.addTimeButton);
        hideKeyboardButton = view.findViewById(R.id.hideKeyboardButton);
        View dayContainer = view.findViewById(R.id.dayContainer);
        dayChecks[0] = view.findViewById(R.id.dayMon);
        dayChecks[1] = view.findViewById(R.id.dayTue);
        dayChecks[2] = view.findViewById(R.id.dayWed);
        dayChecks[3] = view.findViewById(R.id.dayThu);
        dayChecks[4] = view.findViewById(R.id.dayFri);
        dayChecks[5] = view.findViewById(R.id.daySat);
        dayChecks[6] = view.findViewById(R.id.daySun);
        promptTypeCheck = view.findViewById(R.id.promptTypeCheck);
        repeatNoCheck = view.findViewById(R.id.repeatNoCheck);
        autoAnswerCheck = view.findViewById(R.id.autoAnswerCheck);
        priorityCheck = view.findViewById(R.id.priorityCheck);
        alarmCheck = view.findViewById(R.id.alarmCheck);
        AppCompatImageView titleSpeechButton = view.findViewById(R.id.titleSpeechButton);
        AppCompatImageView textSpeechButton = view.findViewById(R.id.textSpeechButton);
        AppCompatImageView ttsSpeechButton = view.findViewById(R.id.ttsSpeechButton);
        Button previewButton = view.findViewById(R.id.previewButton);

        dictationController = new FieldDictationController();
        attachMic(titleSpeechButton, titleField);
        attachMic(textSpeechButton, textField);
        attachMic(ttsSpeechButton, ttsField);
        recordButton.setVisibility(View.GONE);


        ButtonUtils.applyHaptic(deleteRecordButton);
        ButtonUtils.applyHaptic(photoButton);
        ButtonUtils.applyHaptic(deletePhotoButton);
        ButtonUtils.applyHaptic(previewButton);
        deleteRecordButton.setOnClickListener(v -> deleteRecording());
        photoButton.setOnClickListener(v -> handlePhoto());
        deletePhotoButton.setOnClickListener(v -> deletePhoto());
        previewButton.setOnClickListener(v -> previewReminder(titleField, textField));
        repeatIntervalCheck.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                if (repeatIntervalHours == null) {
                    showIntervalDialog();
                } else {
                    repeatIntervalSummary.setVisibility(View.VISIBLE);
                }
            } else {
                repeatIntervalHours = null;
                repeatIntervalSummary.setVisibility(View.GONE);
                removeGeneratedTimes();
            }
        });

        titleField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                v.clearFocus();
                return true;
            }
            return false;
        });
        textField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                v.clearFocus();
                return true;
            }
            return false;
        });
        ttsField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                v.clearFocus();
                return true;
            }
            return false;
        });

        oneTime = getArguments() != null && getArguments().getBoolean("oneTime", false);
        if (oneTime) {
            dateButton.setVisibility(View.VISIBLE);
            dateButton.setOnClickListener(v -> {
                Calendar c = selectedDate != null ? selectedDate : Calendar.getInstance();
                new android.app.DatePickerDialog(requireContext(), AlertDialog.THEME_HOLO_LIGHT,
                        (view1, year, month, dayOfMonth) -> {
                            if (selectedDate == null) selectedDate = Calendar.getInstance();
                            selectedDate.set(Calendar.YEAR, year);
                            selectedDate.set(Calendar.MONTH, month);
                            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            dateButton.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", dayOfMonth, month + 1, year));
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            });
            dayContainer.setVisibility(View.GONE);
            promptTypeCheck.setVisibility(View.GONE);
            repeatNoCheck.setVisibility(View.GONE);
            autoAnswerCheck.setVisibility(View.GONE);
            priorityCheck.setVisibility(View.GONE);
            repeatIntervalCheck.setVisibility(View.GONE);
            repeatIntervalSummary.setVisibility(View.GONE);
            promptTypeCheck.setChecked(false);
            repeatNoCheck.setChecked(false);
            autoAnswerCheck.setChecked(false);
            priorityCheck.setChecked(false);
        }
        ButtonUtils.applyHaptic(addTimeButton);
        ButtonUtils.applyHaptic(hideKeyboardButton);
        hideKeyboardButton.setOnClickListener(this::hideKeyboard);

        shared.getSelected().observe(getViewLifecycleOwner(), r -> {
            editing = r;
                if (r != null) populateForEdit(r, titleField, textField, ttsField);
        });

        Button save = view.findViewById(R.id.saveReminder);
        Button back = view.findViewById(R.id.backButton);
        ButtonUtils.applyHaptic(save);
        ButtonUtils.applyHaptic(back);
        back.setOnClickListener(v -> {
            if (oneTime) {
                requireActivity().getSupportFragmentManager().popBackStack();
            } else if (editing == null &&
                    titleField.getText().toString().trim().isEmpty() &&
                    textField.getText().toString().trim().isEmpty() &&
                    ttsField.getText().toString().trim().isEmpty() &&
                    audioPath == null &&
                    imagePath == null &&
                    timeItems.isEmpty()) {
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                save.performClick();
            }
        });
        addTimeButton.setOnClickListener(v -> {
            if (oneTime && timeItems.size() >= 1) {
                Toast.makeText(getContext(), getString(R.string.max_times_reached), Toast.LENGTH_SHORT).show();
                return;
            } else if (!oneTime && timeItems.size() >= 10) {
                Toast.makeText(getContext(), getString(R.string.max_times_reached), Toast.LENGTH_SHORT).show();
                return;
            }
            Calendar c = Calendar.getInstance();
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            new TimePickerDialog(requireContext(), AlertDialog.THEME_HOLO_LIGHT,
                    (view1, hour, minute) -> {
                        addTimeButton(String.format("%02d:%02d", hour, minute), true);
                        if (repeatIntervalHours != null) generateIntervalTimes();
                        if (oneTime) addTimeButton.setVisibility(View.GONE);
                    },
                    h, m, true).show();
        });

        save.setOnClickListener(v -> {
            if (editing == null &&
                    titleField.getText().toString().trim().isEmpty() &&
                    textField.getText().toString().trim().isEmpty() &&
                    ttsField.getText().toString().trim().isEmpty() &&
                    audioPath == null &&
                    imagePath == null &&
                    timeItems.isEmpty()) {
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }
            if (oneTime && (selectedDate == null || timeItems.size() != 1)) {
                Toast.makeText(getContext(), R.string.select_date, Toast.LENGTH_SHORT).show();
                return;
            }
            Reminder r = editing != null ? editing : new Reminder();
            if (editing == null) {
                r.id = java.util.UUID.randomUUID().toString();
            }
            r.title = titleField.getText().toString();
            r.text = textField.getText().toString();
            r.playAlarm = alarmCheck.isChecked();
            r.audioPath = audioPath;
            r.audioDuration = audioDuration;
            r.imagePath = imagePath;
            if (audioPath != null) {
                r.ttsMessage = null;
            } else {
                r.ttsMessage = ttsField.getText().toString();
            }
            List<String> allTimes = new ArrayList<>();
            boolean[] enabledArr = new boolean[timeItems.size()];
            for (int i = 0; i < timeItems.size(); i++) {
                TimeItem ti = timeItems.get(i);
                allTimes.add(ti.time);
                enabledArr[i] = ti.active;
            }
            r.times = allTimes.toArray(new String[0]);
            r.timeEnabled = enabledArr;
            if (!oneTime) {
                r.days = new boolean[7];
                for (int i = 0; i < 7; i++) r.days[i] = dayChecks[i].isChecked();
                r.yesNoPrompt = promptTypeCheck.isChecked();
                r.repeatOnNo = repeatNoCheck.isChecked();
                r.autoAnswer = autoAnswerCheck.isChecked();
                r.priority = priorityCheck.isChecked();
                if (repeatIntervalCheck.isChecked() && repeatIntervalHours != null) {
                    r.repeatIntervalHours = repeatIntervalHours;
                } else {
                    r.repeatIntervalHours = null;
                }
                r.oneTime = false;
                r.oneTimeDate = 0;
            } else {
                r.days = null;
                r.yesNoPrompt = false;
                r.repeatOnNo = false;
                r.autoAnswer = false;
                r.priority = false;
                r.repeatIntervalHours = null;
                r.oneTime = true;
                Calendar c = (Calendar) selectedDate.clone();
                String[] parts = timeItems.get(0).time.split(":" );
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                c.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                r.oneTimeDate = c.getTimeInMillis();
            }
            r.enabled = true;
            reminderViewModel.addOrUpdate(r);
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }


    private void previewReminder(EditText titleField, EditText textField) {
        Intent svc = new Intent(requireContext(), com.bro.broreminder.utils.OverlayService.class);
        String title = titleField.getText().toString();
        String message = textField.getText().toString();
        if (!title.trim().isEmpty()) svc.putExtra("title", title);
        if (!message.trim().isEmpty()) svc.putExtra("message", message);
        if (audioPath != null) svc.putExtra("audioPath", audioPath);
        if (imagePath != null) svc.putExtra("imagePath", imagePath);
        if (audioPath == null) {
            String ttsText = ttsField.getText().toString();
            if (!ttsText.trim().isEmpty()) svc.putExtra("tts", ttsText);
        }
        svc.putExtra("playAlarm", alarmCheck.isChecked());
        svc.putExtra("yesNo", promptTypeCheck.isChecked());
        svc.putExtra("priority", priorityCheck.isChecked());
        svc.putExtra("skipLog", true);
        requireContext().startService(svc);
    }

    private void populateForEdit(Reminder r, EditText titleField, EditText textField, EditText ttsField) {
        titleField.setText(r.title);
        textField.setText(r.text);
        ttsField.setText(r.ttsMessage);
        alarmCheck.setChecked(r.playAlarm);
        if (r.audioPath != null) {
            audioPath = r.audioPath;
            audioDuration = r.audioDuration;
            ttsField.setVisibility(View.INVISIBLE);
            showRecordingInfo();
            recordButton.setText(R.string.record_again);
        }
        if (r.imagePath != null) {
            imagePath = r.imagePath;
            showPhotoInfo();
        }
        if (r.repeatIntervalHours != null && r.repeatIntervalHours > 0) {
            repeatIntervalHours = r.repeatIntervalHours;
            repeatIntervalSummary.setText(getString(R.string.every_hours, repeatIntervalHours));
            repeatIntervalSummary.setVisibility(View.VISIBLE);
        }
        if (r.times != null) {
            int baseIdx = -1;
            if (repeatIntervalHours != null) {
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < r.times.length; i++) {
                    int m = timeToMinutes(r.times[i]);
                    if (m >= 0 && m < min) {
                        min = m;
                        baseIdx = i;
                    }
                }
            }
            for (int i = 0; i < r.times.length; i++) {
                boolean active = true;
                if (r.timeEnabled != null && i < r.timeEnabled.length) {
                    active = r.timeEnabled[i];
                }
                boolean generated = repeatIntervalHours != null && i != baseIdx;
                addTimeButton(r.times[i], active, generated);
            }
            if (oneTime) addTimeButton.setVisibility(View.GONE);
        }
        if (!oneTime && r.days != null) {
            for (int i = 0; i < 7 && i < r.days.length; i++) {
                dayChecks[i].setChecked(r.days[i]);
            }
        }
        if (!oneTime) {
            promptTypeCheck.setChecked(r.yesNoPrompt);
            repeatNoCheck.setChecked(r.repeatOnNo);
            autoAnswerCheck.setChecked(r.autoAnswer);
            priorityCheck.setChecked(r.priority);
            if (r.repeatIntervalHours != null && r.repeatIntervalHours > 0) {
                repeatIntervalCheck.setChecked(true);
            }
        }
        if (oneTime && r.oneTimeDate > 0) {
            selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(r.oneTimeDate);
            dateButton.setVisibility(View.VISIBLE);
            dateButton.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d",
                    selectedDate.get(Calendar.DAY_OF_MONTH), selectedDate.get(Calendar.MONTH) + 1,
                    selectedDate.get(Calendar.YEAR)));
        }
    }

    private void handlePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);

        File photoFile = createImageFile();
        if (photoFile == null) {
            Toast.makeText(getContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        imagePath = photoFile.getAbsolutePath();
        Uri photoUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQ_CAMERA);
    }

    private File createImageFile() {
        try {
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(new java.util.Date());
            File storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            return new File(storageDir, "IMG_" + timeStamp + ".jpg");
        } catch (Exception e) {
            Log.e("BroReminder", "Failed to create image file", e);
            return null;
        }
    }

    private void showPhotoInfo() {
        photoInfoGroup.setVisibility(View.VISIBLE);
    }

    private void deletePhoto() {
        if (imagePath != null) {
            new File(imagePath).delete();
        }
        imagePath = null;
        photoInfoGroup.setVisibility(View.GONE);
    }

    private void handleRecord() {
        if (recorder != null) {
            stopRecording();
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        startCountdown();
    }

    private void startCountdown() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).create();
        TextView tv = new TextView(requireContext());
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(48f);
        dialog.setView(tv);
        dialog.setCancelable(false);
        dialog.show();

        tv.postDelayed(new Runnable() {
            int sec = 3;
            @Override
            public void run() {
                if (sec > 0) {
                    tv.setText(String.valueOf(sec--));
                    tv.postDelayed(this, 1000);
                } else {
                    tv.setText(getString(R.string.countdown_go));
                    tv.postDelayed(() -> {
                        dialog.dismiss();
                        startRecording();
                    }, 500);
                }
            }
        }, 500);
    }

    private void startRecording() {
        try {
            if (audioPath != null) {
                new File(audioPath).delete();
            }
            File out = new File(requireContext().getFilesDir(), "rec_" + System.currentTimeMillis() + ".m4a");
            audioPath = out.getAbsolutePath();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioPath);
            recorder.prepare();
            recorder.start();
            recordStart = System.currentTimeMillis();
            ttsField.setVisibility(View.INVISIBLE);
            recordInfoGroup.setVisibility(View.GONE);
            recordButton.setText(R.string.stop_record);
            recordingIndicator.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e("BroReminder", "Failed to start recording", e);
            recorder = null;
            Toast.makeText(getContext(), R.string.record_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            recorder.stop();
        } catch (Exception e) {
            Log.e("BroReminder", "Stop recording failed", e);
        }
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        audioDuration = System.currentTimeMillis() - recordStart;
        recordButton.setText(R.string.record_again);
        recordingIndicator.setVisibility(View.GONE);
        showRecordingInfo();
    }

    private void showRecordingInfo() {
        recordInfoGroup.setVisibility(View.VISIBLE);
        long sec = audioDuration / 1000;
        recordInfo.setText(getString(R.string.recording_info, sec));
    }

    private void deleteRecording() {
        if (audioPath != null) {
            new File(audioPath).delete();
        }
        audioPath = null;
        audioDuration = 0;
        recordInfoGroup.setVisibility(View.GONE);
        ttsField.setVisibility(View.VISIBLE);
        recordButton.setText(R.string.record);
        recordingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCountdown();
            } else {
                Toast.makeText(getContext(), R.string.record_permission_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePhoto();
            } else {
                Toast.makeText(getContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == Activity.RESULT_OK) {
            if (imagePath != null) {
                showPhotoInfo();
            } else {
                Log.e("BroReminder", "Image path was null after camera result");
            }
        }
    }

    private int timeToMinutes(String t) {
        try {
            String[] parts = t.split(":");
            if (parts.length != 2) return -1;
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (Exception e) {
            Log.e("BroReminder", "Invalid time: " + t, e);
            return -1;
        }
    }

    private void insertSorted(TimeItem item) {
        int minutes = timeToMinutes(item.time);
        if (minutes < 0) {
            timeItems.add(item);
            return;
        }
        int idx = 0;
        while (idx < timeItems.size() && timeToMinutes(timeItems.get(idx).time) <= minutes) {
            idx++;
        }
        timeItems.add(idx, item);
    }

    private void addTimeButton(String time, boolean active) {
        addTimeButton(time, active, false);
    }

    private void addTimeButton(String time, boolean active, boolean generated) {
        TimeItem item = new TimeItem();
        item.time = time;
        item.active = active;
        item.generated = generated;
        AppCompatButton b = new AppCompatButton(requireContext());
        b.setId(View.generateViewId());
        b.setText(time);
        item.button = b;
        ButtonUtils.applyHaptic(b);
        updateButtonColor(item);
        b.setOnClickListener(v -> {
            item.active = !item.active;
            updateButtonColor(item);
        });
        b.setOnLongClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.delete_time)
                    .setPositiveButton(R.string.ok, (d, w) -> {
                        rootLayout.removeView(b);
                        timeItems.remove(item);
                        updateFlow();
                        if (repeatIntervalHours != null && !item.generated) {
                            generateIntervalTimes();
                        }
                        if (oneTime) addTimeButton.setVisibility(View.VISIBLE);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });
        rootLayout.addView(b);
        insertSorted(item);
        updateFlow();
    }

    private void updateButtonColor(TimeItem item) {
        item.button.setBackgroundResource(item.active ? R.drawable.yes_button : R.drawable.no_button);
        item.button.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void updateFlow() {
        int[] ids = new int[timeItems.size()];
        for (int i = 0; i < timeItems.size(); i++) {
            ids[i] = timeItems.get(i).button.getId();
        }
        timesFlow.setReferencedIds(ids);
        timesFlow.requestLayout();
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void removeGeneratedTimes() {
        List<TimeItem> toRemove = new ArrayList<>();
        for (TimeItem ti : timeItems) {
            if (ti.generated) {
                rootLayout.removeView(ti.button);
                toRemove.add(ti);
            }
        }
        timeItems.removeAll(toRemove);
        updateFlow();
    }

    private void generateIntervalTimes() {
        removeGeneratedTimes();
        if (repeatIntervalHours == null) return;
        int base = -1;
        for (TimeItem ti : timeItems) {
            if (!ti.generated) {
                int m = timeToMinutes(ti.time);
                if (m >= 0 && (base < 0 || m < base)) {
                    base = m;
                }
            }
        }
        if (base < 0) return;
        int interval = repeatIntervalHours * 60;
        int cur = base + interval;
        while (cur < 24 * 60) {
            String t = String.format(Locale.getDefault(), "%02d:%02d", cur / 60, cur % 60);
            boolean exists = false;
            for (TimeItem ti : timeItems) {
                if (ti.time.equals(t)) { exists = true; break; }
            }
            if (!exists) {
                addTimeButton(t, true, true);
            }
            cur += interval;
        }
    }

    private void attachMic(AppCompatImageView micBtn, EditText target) {
        micBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 0);
                return;
            }
            micBtn.setEnabled(false);
            Animation anim = new AlphaAnimation(1f, 0.3f);
            anim.setDuration(500);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            micBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange));
            micBtn.startAnimation(anim);
            dictationController.startDictation(requireActivity(), new FieldDictationController.Listener() {
                @Override
                public void onPartialText(String text) { }

                @Override
                public void onFinalText(String text) {
                    requireActivity().runOnUiThread(() -> {
                        int start = Math.max(target.getSelectionStart(), 0);
                        int end = Math.max(target.getSelectionEnd(), 0);
                        target.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
                        micBtn.clearAnimation();
                        micBtn.clearColorFilter();
                        micBtn.setEnabled(true);
                    });
                }

                @Override
                public void onError(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.dictation_error, Toast.LENGTH_SHORT).show();
                        micBtn.clearAnimation();
                        micBtn.clearColorFilter();
                        micBtn.setEnabled(true);
                    });
                }

                @Override
                public void onTimeout() {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.dictation_timeout, Toast.LENGTH_SHORT).show();
                        micBtn.clearAnimation();
                        micBtn.clearColorFilter();
                        micBtn.setEnabled(true);
                    });
                }
            });
        });
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void showIntervalDialog() {
        String[] options = new String[8];
        for (int i = 0; i < 8; i++) options[i] = String.valueOf(i + 1);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_interval)
                .setSingleChoiceItems(options, -1, (d, which) -> {
                    repeatIntervalHours = which + 1;
                    repeatIntervalSummary.setText(getString(R.string.every_hours, repeatIntervalHours));
                    repeatIntervalSummary.setVisibility(View.VISIBLE);
                    generateIntervalTimes();
                    d.dismiss();
                })
                .setOnCancelListener(d -> repeatIntervalCheck.setChecked(false))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dictationController != null) {
            dictationController.cancel();
        }
    }
}