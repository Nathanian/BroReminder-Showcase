package com.bro.broreminder.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bro.broreminder.R;
import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.viewmodels.SharedReminderViewModel;

/**
 * Displays details for a single reminder.
 */
public class ReminderDetailFragment extends Fragment {

    private SharedReminderViewModel sharedReminderViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminder_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedReminderViewModel = new ViewModelProvider(requireActivity()).get(SharedReminderViewModel.class);

        TextView title = view.findViewById(R.id.detailTitle);
        TextView text = view.findViewById(R.id.detailText);
        Button editButton = view.findViewById(R.id.editButton);

        sharedReminderViewModel.getSelected().observe(getViewLifecycleOwner(), r -> display(title, text, r));

        editButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ReminderCreateFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void display(TextView title, TextView text, Reminder r) {
        if (r == null) return;
        title.setText(r.title);
        text.setText(r.text);
    }
}