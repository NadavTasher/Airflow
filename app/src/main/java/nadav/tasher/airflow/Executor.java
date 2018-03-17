package nadav.tasher.airflow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class Executor extends Activity {
	private String configuration=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(configuration==null){
			configuration=getIntent().getExtras().getString(Main.configuration,"");
		}
		Main.activateTunnel.send(new Main.Action(getApplicationContext(),configuration));
		Main.widgetTunnel.send("A Widget Was Pressed.");
		finish();
	}
}
