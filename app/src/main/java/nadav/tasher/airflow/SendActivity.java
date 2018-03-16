package nadav.tasher.airflow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class SendActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final SharedPreferences sp=getSharedPreferences(getPackageName() + "_PREFS", Context.MODE_PRIVATE);
		Intent in;
		if(sp.contains(getIntent().getStringExtra("config") + Main.pdata)){
			in=new Intent(Main.send_broadcast_bluetooth);
			in.putExtra("config", getIntent().getStringExtra("config"));
		}else{
			in=new Intent(Main.send_broadcast_network);
			in.putExtra("config", getIntent().getStringExtra("config"));
		}
		sendBroadcast(in);
		sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
		finish();
	}
}
