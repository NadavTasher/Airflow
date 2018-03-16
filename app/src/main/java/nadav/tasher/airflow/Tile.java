package nadav.tasher.airflow;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.TileService;

@TargetApi(Build.VERSION_CODES.N)
public class Tile extends TileService {
	BroadcastReceiver update=new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			SharedPreferences sp=getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
			String wid=sp.getString("SELECTED_QS", null);
			if(wid!=null){
				String text=sp.getString(wid + Main.ptext, "Send Flow");
				getQsTile().setLabel(text);
				getQsTile().updateTile();
			}
		}
	};
	@Override
	public void onClick() {
		super.onClick();
		SharedPreferences sp=getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
		String wid=sp.getString("SELECTED_QS", null);
		if(wid!=null){
			String text=sp.getString(wid + Main.ptext, "Send Flow");
			getQsTile().setLabel(text);
			getQsTile().updateTile();
			Intent in;
			if(sp.contains(wid + Main.pdata)){
				in=new Intent(Main.send_broadcast_bluetooth);
				in.putExtra("config", wid);
			}else{
				in=new Intent(Main.send_broadcast_network);
				in.putExtra("config", wid);
			}
			sendBroadcast(in);
			sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
		}
	}
	@Override
	public void onStartListening() {
		super.onStartListening();
		registerReceiver(update, new IntentFilter("nadav.tasher.airflow.UPDATE"));
		SharedPreferences sp=getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
		String wid=sp.getString("SELECTED_QS", null);
		if(wid!=null){
			String text=sp.getString(wid + Main.ptext, "Send Flow");
			getQsTile().setLabel(text);
			getQsTile().updateTile();
		}
	}
	@Override
	public void onStopListening() {
		super.onStopListening();
		unregisterReceiver(update);
	}
	@Override
	public void onTileAdded() {
		super.onTileAdded();
		SharedPreferences sp=getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
		String wid=sp.getString("SELECTED_QS", null);
		if(wid!=null){
			String text=sp.getString(wid + Main.ptext, "Send Flow");
			getQsTile().setLabel(text);
			getQsTile().updateTile();
		}
	}
}
