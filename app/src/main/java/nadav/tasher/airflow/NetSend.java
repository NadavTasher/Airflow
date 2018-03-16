package nadav.tasher.airflow;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import nadav.tasher.toollib.FileFactory;

public class NetSend extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final SharedPreferences sp=context.getSharedPreferences(context.getPackageName() + "_PREFS", Context.MODE_PRIVATE);
		//final String cconfig=sp.getString(id + PickConfig.config, null);
		final String cconfig=intent.getExtras().getString("config", null);
		String url=sp.getString(cconfig + Main.purl, null) + ":" + sp.getString(cconfig + Main.pport, "80") + "/" + sp.getString(cconfig + Main.pcgi, "");
		String command=sp.getString(cconfig + Main.pcomm, null);
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
		new SendData(new SendData.OnSend() {
			@Override
			public void onSend() {
				if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)){
					AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(context);
					RemoteViews views=new RemoteViews(context.getPackageName(), R.layout.widget);
					views.setTextViewText(R.id.textView, sp.getString(cconfig + Main.ptext, "Sent"));
					appWidgetManager.updateAppWidget(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0), views);
					context.sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
				}
			}
			@Override
			public void onFail() {
				if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)){
					AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(context);
					RemoteViews views=new RemoteViews(context.getPackageName(), R.layout.widget);
					views.setTextViewText(R.id.textView, "Failed");
					appWidgetManager.updateAppWidget(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0), views);
					context.sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
				}
			}
		}).execute(url, command);
	}
}

class SendData extends AsyncTask<String, String, String> {
	private OnSend os;
	SendData(OnSend os) {
		this.os=os;
	}
	@Override
	protected String doInBackground(String... strings) {
		Log.i("AirFlow", Send(strings[0], strings[1]));
		return null;
	}
	private String Send(String string, String postdata) {
		String ret="LOST";
		try{
			HttpURLConnection connection=(HttpURLConnection) new URL(string).openConnection();
			FileFactory.log(Main.log, "Starting Connection:\n" + string + "\n" + postdata);
			if(postdata!=null){
				connection.setConnectTimeout(4000);
				connection.setReadTimeout(4000);
				connection.setRequestMethod("POST");
				byte[] postData=postdata.getBytes(Charset.forName("UTF-8"));
				int postDataLength=postData.length;
				connection.setRequestProperty("Content-Type", "application/*");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
				connection.setUseCaches(false);
				connection.connect();
				try(DataOutputStream wr=new DataOutputStream(connection.getOutputStream())){
					wr.write(postData);
					if(os!=null){
						os.onSend();
					}
					FileFactory.log(Main.log, "Data Sent");
					Log.i("AirFlow", "SENT");
					wr.flush();
					wr.close();
				}
				BufferedInputStream in=new BufferedInputStream(connection.getInputStream());
				StringBuilder text=new StringBuilder();
				BufferedReader inR=new BufferedReader(new InputStreamReader(in));
				String str;
				while((str=inR.readLine())!=null){
					text.append(str);
					text.append('\n');
				}
				inR.close();
				FileFactory.log(Main.log, "Data Recived: " + text.toString());
				connection.disconnect();
				return text.toString();
			}
		}catch(IOException e){
			e.printStackTrace();
			FileFactory.log(Main.log, "Failed To Connect: " + e.toString());
			if(os!=null)
				os.onFail();
		}
		return ret;
	}
	interface OnSend {
		void onSend();
		void onFail();
	}
}