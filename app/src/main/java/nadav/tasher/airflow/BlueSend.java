package nadav.tasher.airflow;

import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import nadav.tasher.toollib.FileFactory;

public class BlueSend extends BroadcastReceiver {
	BluetoothSocket mmSocket;
	OutputStream mmOutputStream;
	BluetoothAdapter blueAdapter;
	private Context cont;
	@Override
	public void onReceive(Context context, Intent intent) {
		final SharedPreferences sp=context.getSharedPreferences(context.getPackageName() + "_PREFS", Context.MODE_PRIVATE);
		final String cconfig=intent.getExtras().getString("config", null);
		if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)){
			final int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
			FileFactory.log(Main.log, "Widget Pressed");
			FileFactory.log(Main.log, "Widget ID:" + id);
			AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(context);
			RemoteViews views=new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setTextViewText(R.id.textView, "Sending");
			appWidgetManager.updateAppWidget(id, views);
		}
		sp.edit().putInt(cconfig + Main.count, sp.getInt(cconfig + Main.count, 0) + 1).apply();
		cont=context;
		try{
			openBT(cconfig);
		}catch(IOException e){
			Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
			context.sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
			FileFactory.error(Main.log, "Failed To Open Bluetooth");
			e.printStackTrace();
		}
	}
	void openBT(String config) throws IOException {
		final SharedPreferences sp=cont.getSharedPreferences(cont.getPackageName() + "_PREFS", Context.MODE_PRIVATE);
		BluetoothManager manager=(BluetoothManager)cont.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager!=null) {
			blueAdapter = manager.getAdapter();
			if (blueAdapter.isEnabled()) {
				blueAdapter.cancelDiscovery();
				BluetoothDevice device = null;
				if (sp.getString(config + Main.pbaddress, null) != null) {
					device = blueAdapter.getRemoteDevice(sp.getString(config + Main.pbaddress, "NULL"));
				}
				if (device != null) {
					Log.i("Bluetooth Send","Got Device UUID/MAC");
					UUID uuid = device.getUuids()[0].getUuid();
					mmSocket = null;
					mmSocket = device.createRfcommSocketToServiceRecord(uuid);
					mmSocket.connect();
					if (mmSocket.isConnected()) {
						Log.i("Bluetooth Send","Sending Something Over Bluetooth");
						sendData(sp.getString(config + Main.pdata, "NULL"));
					} else {
						closeBT();
					}
				}
			} else {
				Toast.makeText(cont, "Bluetooth is off", Toast.LENGTH_SHORT).show();
				cont.sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
				FileFactory.warning(Main.log, "Detected Bluetooth OFF");
			}
			cont.sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
		}
	}
	void sendData(String s) throws IOException {
		mmOutputStream=mmSocket.getOutputStream();
		if(mmOutputStream!=null){
			mmOutputStream.write(s.getBytes());
			mmOutputStream.flush();
			mmOutputStream.close();
			mmOutputStream=null;
			FileFactory.log(Main.log, "Sent via Bluetooth");
			Toast.makeText(cont, "Sent", Toast.LENGTH_SHORT).show();
		}
		closeBT();
	}
	void closeBT() throws IOException {
		mmSocket.close();
		mmSocket=null;
		cont.sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
	}
}
