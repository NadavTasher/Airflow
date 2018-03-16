package nadav.tasher.airflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.ScrollView;

import java.util.ArrayList;

import nadav.tasher.toollib.Stringer;

public class PickConfig extends Activity {
	static final String config="_CONFIG";
	int id;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final SharedPreferences sp=getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
		AlertDialog.Builder ad=new AlertDialog.Builder(this);
		ad.setTitle("Setup Widget");
		ad.setIcon(R.drawable.ic_launcher);
		LinearLayout ll=new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		Button newC=new Button(this);
		RadioGroup grp=new RadioGroup(this);
		newC.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent resultValue=new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
				setResult(RESULT_CANCELED, resultValue);
				startActivity(new Intent(getApplicationContext(), Main.class));
				finish();
			}
		});
		newC.setText(getString(R.string.newC));
		id=getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		final ArrayList<String> widgets;
		if(sp.getString("widgets", "").contains("::")){
			widgets=Stringer.cutOnEvery(sp.getString("widgets", null), "::");
		}else{
			widgets=new ArrayList<>();
			widgets.add(sp.getString("widgets", null));
		}
		for(int i=0; i<widgets.size(); i++){
			RadioButton rb=new RadioButton(this);
			rb.setText(widgets.get(i));
			rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if(b){
						sp.edit().putString(id + config, compoundButton.getText().toString()).apply();
					}
				}
			});
			grp.addView(rb);
		}
		ScrollView sv=new ScrollView(this);
		sv.addView(grp);
		ll.addView(sv);
		ad.setView(ll);
		ad.setCancelable(false);
		ad.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(getApplicationContext());
				RemoteViews views=new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
				appWidgetManager.updateAppWidget(getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID), views);
				Intent resultValue=new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
				setResult(RESULT_OK, resultValue);
				appWidgetManager.updateAppWidget(getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID), views);
				sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
				finish();
			}
		});
		ad.setNegativeButton("New", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				Intent resultValue=new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
				setResult(RESULT_CANCELED, resultValue);
				startActivity(new Intent(getApplicationContext(), Main.class));
				finish();
			}
		});
		ad.show();
	}
}

