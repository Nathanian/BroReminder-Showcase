package com.bro.broreminder.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.os.Environment;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bro.broreminder.R;
import com.bro.broreminder.report.ReportStorage;
import com.bro.broreminder.utils.ButtonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = "BroReminder";
    private static final int REQ_PERMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GridView grid = new GridView(this);
        grid.setNumColumns(3);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        AppCompatButton back = new AppCompatButton(this);
        back.setText(R.string.back);
        back.setBackgroundResource(R.drawable.black_button_background);
        back.setTextColor(android.graphics.Color.WHITE);
        ButtonUtils.applyHaptic(back);

        File dir = new File(getFilesDir(), "reports");
        File[] files = dir.listFiles();
        if (files == null) files = new File[0];
        java.util.Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName()));

        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            String date = files[i].getName().replace("report_", "").replace(".txt", "");
            String[] p = date.split("-");
            if (p.length == 3) date = p[2] + "-" + p[1] + "-" + p[0];
            names[i] = getString(R.string.report_heading, date);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setGravity(Gravity.CENTER);
                tv.setBackgroundResource(R.drawable.checkbox_container_background);
                int pad = (int) (getResources().getDisplayMetrics().density * 8);
                tv.setPadding(pad, pad, pad, pad);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                return tv;
            }
        };
        grid.setAdapter(adapter);

        File[] finalFiles = files;
        grid.setOnItemClickListener((parent, view, position, id) -> {
            File f = finalFiles[position];
            try {
                String text = readFile(f);
                String date = f.getName().replace("report_", "").replace(".txt", "");
                String[] p = date.split("-");
                if (p.length == 3) date = p[2] + "-" + p[1] + "-" + p[0];
                int padm = (int) (getResources().getDisplayMetrics().density * 16);
                int barHeight = (int) (getResources().getDisplayMetrics().density * 24);

                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);

                View tBar = new View(this);
                tBar.setBackgroundColor(getResources().getColor(R.color.metallic_blue));
                layout.addView(tBar, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, barHeight));

                TextView heading = new TextView(this);
                heading.setText(getString(R.string.report_heading, date));
                heading.setGravity(Gravity.CENTER);
                heading.setPadding(padm, padm, padm, padm);
                layout.addView(heading);

                ScrollView outerScroll = new ScrollView(this);
                outerScroll.setFillViewport(true);
                LinearLayout scrollContent = new LinearLayout(this);
                scrollContent.setOrientation(LinearLayout.VERTICAL);

                ScrollView innerScroll = new ScrollView(this);
                TableLayout table = new TableLayout(this);
                table.setStretchAllColumns(true);

                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty() || line.startsWith("Date:")) continue;
                    TableRow row = new TableRow(this);
                    String[] parts = line.split(" - ");
                    for (String part : parts) {
                        TextView tv = new TextView(this);
                        tv.setText(part);
                        tv.setPadding(padm / 2, padm / 2, padm / 2, padm / 2);
                        tv.setBackgroundResource(R.drawable.edit_text_border);
                        row.addView(tv);
                    }
                    table.addView(row);
                }
                innerScroll.addView(table);
                scrollContent.addView(innerScroll);
                outerScroll.addView(scrollContent);

                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0);
                scrollParams.weight = 1f;
                scrollParams.bottomMargin = padm;
                layout.addView(outerScroll, scrollParams);

                // BOTTOM BAR mit sichtbarem OK-Button
                LinearLayout bottomBar = new LinearLayout(this);
                bottomBar.setOrientation(LinearLayout.VERTICAL);
                bottomBar.setBackgroundColor(getResources().getColor(R.color.metallic_blue));
                bottomBar.setPadding(padm, padm, padm, padm);

                AppCompatButton okButton = new AppCompatButton(this);
                okButton.setText(R.string.ok);
                okButton.setBackgroundResource(R.drawable.black_button_background);
                okButton.setTextColor(android.graphics.Color.WHITE);
                okButton.setTextSize(16f);
                okButton.setAllCaps(false);
                ButtonUtils.applyHaptic(okButton);

                LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                okLp.gravity = Gravity.CENTER;
                bottomBar.addView(okButton, okLp);
                layout.addView(bottomBar);

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(layout)
                        .create();
                okButton.setOnClickListener(v2 -> dialog.dismiss());
                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to read report", e);
            }
        });

        back.setOnClickListener(v -> finish());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.main_background);

        int pad = (int) (getResources().getDisplayMetrics().density * 8);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1f;
        root.addView(grid, lp);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(pad, 0, pad, 0);
        buttonRow.addView(back, blp);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = pad;
        rowLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(buttonRow, rowLp);

        setContentView(root);
    }

    private static String readFile(File f) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private boolean ensurePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!write || !read) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMS);
                return false;
            }
        }
        return true;
    }

    private void exportReports() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "WRITE_EXTERNAL_STORAGE permission not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMS);
            return;
        }
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File out = new File(dir, "bro_reports.json");
            ReportStorage.exportToFile(this, out);
            Log.i(TAG, getString(R.string.export_success, out.getAbsolutePath()));
        } catch (Exception e) {
            Log.e(TAG, "Report export failed", e);
        }
    }

    private void importReports() {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File in = new File(dir, "bro_reports.json");
            if (!in.exists()) {
                Log.i(TAG, getString(R.string.file_not_found));
                return;
            }
            ReportStorage.importFromFile(this, in);
            Log.i(TAG, getString(R.string.import_success));
            recreate();
        } catch (Exception e) {
            Log.e(TAG, "Report import failed", e);
        }
    }
}
