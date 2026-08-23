package com.odelly.epilepsysafety;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import android.graphics.Color;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONObject;

public class PythonPetAcademyActivity extends Activity {
    private PyObject bridge;
    private TextView petStatus;
    private TextView lessonTitle;
    private TextView lessonInfo;
    private EditText editor;
    private TextView output;
    private int lessonIndex = 0;
    private JSONArray lessons;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
        bridge = Python.getInstance().getModule("pet_academy.bridge");
        buildUi();
        loadLessons();
        refreshPet();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        scroll.addView(root);

        TextView title = text("Python Pet Academy", 28, true);
        root.addView(title);
        root.addView(text("Learn Python by caring for a pet, solving quests, and writing real Python code.\n\nEpilepsy-safety design: this screen uses static UI and avoids flashing, rapid color changes, screen shake, and unnecessary haptics.", 16, false));

        petStatus = text("Loading Pip...", 18, true);
        root.addView(petStatus);
        Button feed = button("Feed Pip");
        feed.setOnClickListener(v -> { bridge.callAttr("action", "feed"); refreshPet(); });
        root.addView(feed);
        Button play = button("Play a calm game");
        play.setOnClickListener(v -> { bridge.callAttr("action", "play"); refreshPet(); });
        root.addView(play);

        lessonTitle = text("Lesson", 22, true);
        root.addView(lessonTitle);
        lessonInfo = text("", 16, false);
        root.addView(lessonInfo);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button previous = button("Previous");
        Button next = button("Next");
        previous.setOnClickListener(v -> showLesson(Math.max(0, lessonIndex - 1)));
        next.setOnClickListener(v -> showLesson(Math.min(lessons == null ? 0 : lessons.length() - 1, lessonIndex + 1)));
        nav.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(nav);

        root.addView(text("Hands-on Python editor", 22, true));
        editor = new EditText(this);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setGravity(48);
        editor.setMinLines(10);
        editor.setText("print('Hello, Pip!')\n\nname = 'Pip'\nprint(name)");
        editor.setBackgroundColor(Color.LTGRAY);
        root.addView(editor);

        Button run = button("Run Python");
        run.setOnClickListener(v -> runCode());
        root.addView(run);
        output = text("Output will appear here.", 15, false);
        output.setTypeface(Typeface.MONOSPACE);
        root.addView(output);

        Button teach = button("Give Pip this lesson badge");
        teach.setOnClickListener(v -> {
            if (lessons != null && lessonIndex < lessons.length()) {
                try { bridge.callAttr("action", "teach:" + lessons.getJSONObject(lessonIndex).getString("id")); } catch (Exception ignored) {}
                refreshPet();
            }
        });
        root.addView(teach);

        Button back = button("Back to Safety Filter");
        back.setOnClickListener(v -> finish());
        root.addView(back);
        setContentView(scroll);
    }

    private void loadLessons() {
        try {
            lessons = new JSONArray(bridge.callAttr("curriculum").toString());
            showLesson(0);
        } catch (Exception e) {
            lessonTitle.setText("Curriculum unavailable");
            lessonInfo.setText(e.toString());
        }
    }

    private void showLesson(int index) {
        if (lessons == null || index < 0 || index >= lessons.length()) return;
        lessonIndex = index;
        try {
            JSONObject l = lessons.getJSONObject(index);
            lessonTitle.setText("Lesson " + l.getString("id") + ": " + l.getString("title"));
            lessonInfo.setText("Topics: " + l.getJSONArray("topics").join(", ") + "\n\nWrite code below and run it. Each completed lesson can earn Pip a badge.");
        } catch (Exception ignored) {}
    }

    private void runCode() {
        try {
            String result = bridge.callAttr("execute", editor.getText().toString()).toString();
            JSONObject r = new JSONObject(result);
            output.setText((r.optBoolean("ok") ? "SUCCESS\n" : "ERROR\n") + r.optString("stdout") + r.optString("error"));
        } catch (Exception e) {
            output.setText("Runner error: " + e);
        }
    }

    private void refreshPet() {
        try {
            JSONObject s = new JSONObject(bridge.callAttr("status").toString());
            petStatus.setText(s.getString("name") + "\nHunger: " + s.getInt("hunger") + "%  Energy: " + s.getInt("energy") + "%  Happiness: " + s.getInt("happiness") + "%\nKnowledge: " + s.getInt("knowledge") + "%  Friendship: " + s.getInt("friendship") + "%");
        } catch (Exception e) { petStatus.setText("Pip is unavailable: " + e); }
    }

    private TextView text(String value, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setPadding(0, 10, 0, 10);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this); b.setText(value); return b;
    }
}
