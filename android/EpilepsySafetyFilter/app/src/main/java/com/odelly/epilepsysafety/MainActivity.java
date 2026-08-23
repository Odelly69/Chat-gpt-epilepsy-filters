package com.odelly.epilepsysafety;

import android.app.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;

public class MainActivity extends Activity {
  static final String PREF="safety";
  SeekBar dim, contrast; Switch enabled, vibration, motion;
  @Override public void onCreate(Bundle b){super.onCreate(b); LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,32,32,32);
    TextView title=new TextView(this);title.setText("Epilepsy Safety Filter");title.setTextSize(26);root.addView(title);
    TextView info=new TextView(this);info.setText("Risk-reduction tool for visually sensitive users. It cannot guarantee seizure prevention.");root.addView(info);
    enabled=new Switch(this);enabled.setText("Enable safety overlay");root.addView(enabled);
    dim=bar("Brightness reduction",80,root); contrast=bar("Contrast reduction",60,root);
    motion=new Switch(this);motion.setText("Reduce motion / animation where possible");root.addView(motion);
    vibration=new Switch(this);vibration.setText("Vibration-sensitive profile");root.addView(vibration);
    Button access=new Button(this);access.setText("Open Accessibility Settings");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(access);
    Button safe=new Button(this);safe.setText("Maximum Safety Preset");safe.setOnClickListener(v->{dim.setProgress(90);contrast.setProgress(80);motion.setChecked(true);vibration.setChecked(true);enabled.setChecked(true);save();});root.addView(safe);
    enabled.setOnCheckedChangeListener((v,c)->save()); motion.setOnCheckedChangeListener((v,c)->save()); vibration.setOnCheckedChangeListener((v,c)->save());
    dim.setOnSeekBarChangeListener(simple()); contrast.setOnSeekBarChangeListener(simple()); load(); setContentView(root);
  }
  SeekBar bar(String label,int value,LinearLayout r){TextView t=new TextView(this);t.setText(label);r.addView(t);SeekBar s=new SeekBar(this);s.setMax(100);s.setProgress(value);r.addView(s);return s;}
  SeekBar.OnSeekBarChangeListener simple(){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){save();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
  void save(){getSharedPreferences(PREF,0).edit().putBoolean("enabled",enabled!=null&&enabled.isChecked()).putInt("dim",dim==null?80:dim.getProgress()).putInt("contrast",contrast==null?60:contrast.getProgress()).putBoolean("motion",motion!=null&&motion.isChecked()).putBoolean("vibration",vibration!=null&&vibration.isChecked()).apply();}
  void load(){android.content.SharedPreferences p=getSharedPreferences(PREF,0);enabled.setChecked(p.getBoolean("enabled",false));dim.setProgress(p.getInt("dim",80));contrast.setProgress(p.getInt("contrast",60));motion.setChecked(p.getBoolean("motion",true));vibration.setChecked(p.getBoolean("vibration",false));}
}
