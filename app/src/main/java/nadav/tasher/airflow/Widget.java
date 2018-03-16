package nadav.tasher.airflow;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

import java.util.Random;

import nadav.tasher.toollib.FileFactory;

public class Widget extends AppWidgetProvider {
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		final SharedPreferences sp=context.getSharedPreferences(context.getPackageName() + "_PREFS", Context.MODE_PRIVATE);
		RemoteViews views=new RemoteViews(context.getPackageName(), R.layout.widget);
		String cuconfig=sp.getString(appWidgetId + PickConfig.config, "");
		views.setInt(R.id.f1, "setBackgroundColor", sp.getInt(cuconfig + Main.pcolor, Color.CYAN) + 0x50000000);
		views.setInt(R.id.textView, "setTextColor", sp.getInt(cuconfig + Main.ptcolor, Color.BLACK));
		Intent in;
		if(sp.contains(cuconfig + Main.pdata)){
			in=new Intent(Main.send_broadcast_bluetooth);
			in.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			in.putExtra("config", sp.getString(appWidgetId + PickConfig.config, null));
			views.setTextViewText(R.id.textView, "[B] " + sp.getString(cuconfig + Main.ptext, "TEXT"));
		}else{
			in=new Intent(Main.send_broadcast_network);
			in.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			in.putExtra("config", sp.getString(appWidgetId + PickConfig.config, null));
			views.setTextViewText(R.id.textView, "[N] " + sp.getString(cuconfig + Main.ptext, "TEXT"));
		}
		PendingIntent pi=PendingIntent.getBroadcast(context, new Random().nextInt(), in, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.f1, pi);
		views.setOnClickPendingIntent(R.id.textView, pi);
		FileFactory.log(Main.log, "Widget Update " + appWidgetId);
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for(int appWidgetId : appWidgetIds){
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		FileFactory.log(Main.log, "Widget Deleted");
	}
	@Override
	public void onEnabled(Context context) {
		FileFactory.log(Main.log, "Widget Created");
	}
	@Override
	public void onDisabled(Context context) {
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		FileFactory.log(Main.log, "Force Widget Update");
		AppWidgetManager gm=AppWidgetManager.getInstance(context);
		int[] ids=gm.getAppWidgetIds(new ComponentName(context, Widget.class));
		this.onUpdate(context, gm, ids);
	}
}

