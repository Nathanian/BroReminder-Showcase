package com.bro.broreminder.fragments;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;
import android.text.InputType;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.bro.broreminder.R;
import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.utils.ButtonUtils;
import com.bro.broreminder.viewmodels.ReminderViewModel;
import com.bro.broreminder.viewmodels.SharedReminderViewModel;

import java.util.List;

/**
 * Displays all reminders and allows navigation to details or creation.
 */
public class ReminderListFragment extends Fragment {

    private ReminderViewModel reminderViewModel;
    private SharedReminderViewModel sharedReminderViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminder_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reminderViewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);
        sharedReminderViewModel = new ViewModelProvider(requireActivity()).get(SharedReminderViewModel.class);

        Button addButton = view.findViewById(R.id.addReminderButton);
        Button oneTimeButton = view.findViewById(R.id.addOneTimeButton);
        Button settingsButton = view.findViewById(R.id.settingsButton);
        Button reportButton = view.findViewById(R.id.reportButton);
        ImageButton exitButton = view.findViewById(R.id.exitButton);

        LinearLayout container = view.findViewById(R.id.reminderContainer);

        ButtonUtils.applyHaptic(addButton);
        ButtonUtils.applyHaptic(oneTimeButton);
        ButtonUtils.applyHaptic(settingsButton);
        ButtonUtils.applyHaptic(reportButton);
        ButtonUtils.applyHaptic(exitButton);

        addButton.setOnClickListener(v -> {
            sharedReminderViewModel.select(null);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ReminderCreateFragment())
                    .addToBackStack(null)
                    .commit();
        });
        oneTimeButton.setOnClickListener(v -> {
            sharedReminderViewModel.select(null);
            ReminderCreateFragment frag = new ReminderCreateFragment();
            Bundle args = new Bundle();
            args.putBoolean("oneTime", true);
            frag.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, frag)
                    .addToBackStack(null)
                    .commit();
        });
        settingsButton.setOnLongClickListener(v -> {
            showPasswordDialog();
            return true;
        });
        exitButton.setOnClickListener(v -> {
            Activity activity = requireActivity();
            activity.finish();

            Context ctx = activity.getApplicationContext();
            PackageManager pm = ctx.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.bro.brokiosk4sora");
            if (launchIntent != null) {
                ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
                boolean broughtToFront = false;
                if (am != null) {
                    List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(Integer.MAX_VALUE);
                    for (ActivityManager.RunningTaskInfo task : tasks) {
                        if (task.baseActivity != null &&
                                "com.bro.brokiosk4sora".equals(task.baseActivity.getPackageName())) {
                            am.moveTaskToFront(task.id, 0);
                            broughtToFront = true;
                            break;
                        }
                    }
                }
                if (!broughtToFront) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(launchIntent);
                }
            } else {
                Toast.makeText(ctx, "kiosk not installed", Toast.LENGTH_SHORT).show();
            }
        });

        reportButton.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(requireContext(), com.bro.broreminder.activities.ReportActivity.class);
            startActivity(i);
        });

        reminderViewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> populateList(container, reminders));
    }

    private void populateList(LinearLayout container, List<Reminder> list) {
        container.removeAllViews();
        int margin = (int) (getResources().getDisplayMetrics().density * 8);
        for (Reminder r : list) {
            LinearLayout row = new LinearLayout(container.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(margin, margin, margin, margin);
            row.setLayoutParams(lp);

            TextView labelView = new TextView(container.getContext());
            labelView.setText(r.title != null && !r.title.isEmpty() ? r.title : r.text);
            if (r.oneTime) {
                labelView.setBackgroundResource(R.drawable.one_time_button);
            } else {
                labelView.setBackgroundResource(r.enabled ? R.drawable.yes_button : R.drawable.no_button);
            }
            labelView.setTextColor(Color.WHITE);
            labelView.setPadding(16, 16, 16, 16);
            labelView.setGravity(Gravity.CENTER);
            labelView.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams lpl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            labelView.setLayoutParams(lpl);
            if (!r.oneTime) {
                labelView.setOnClickListener(v -> {
                    r.enabled = !r.enabled;
                    reminderViewModel.addOrUpdate(r);
                });
            }

            AppCompatButton edit = new AppCompatButton(container.getContext());
            edit.setText(R.string.edit);
            edit.setBackgroundResource(R.drawable.black_button_background);
            edit.setTextColor(Color.WHITE);
            ButtonUtils.applyHaptic(edit);
            edit.setOnClickListener(v -> {
                sharedReminderViewModel.select(r);
                ReminderCreateFragment frag = new ReminderCreateFragment();
                Bundle args = new Bundle();
                if (r.oneTime) args.putBoolean("oneTime", true);
                frag.setArguments(args);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, frag)
                        .addToBackStack(null)
                        .commit();
            });
            AppCompatButton del = new AppCompatButton(container.getContext());
            del.setText(R.string.delete);
            del.setBackgroundResource(R.drawable.black_button_background);
            del.setTextColor(Color.WHITE);
            ButtonUtils.applyHaptic(del);
            del.setOnClickListener(v -> reminderViewModel.delete(r));

            row.addView(labelView);
            row.addView(edit);
            row.addView(del);
            container.addView(row);        }
    }
    private void showPasswordDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.enter_password)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    if ("4444".equals(input.getText().toString())) {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, new SettingsFragment())
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Toast.makeText(getContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}