package nadav.tasher.airflow;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

import nadav.tasher.lightool.Tunnel;

import static android.content.Context.MODE_PRIVATE;

public class Widget extends AppWidgetProvider {

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        Main.configurationChangeTunnel.addReceiver(new Tunnel.OnTunnel<ArrayList<Main.Configuration>>() {
            @Override
            public void onReceive(ArrayList<Main.Configuration> configurations) {
                for (int appWidgetId : appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId);
                }
            }
        });
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Main.widgetTunnel.send("A Widget Was Deleted.");
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager gm = AppWidgetManager.getInstance(context);
        int[] ids = gm.getAppWidgetIds(new ComponentName(context, Widget.class));
        this.onUpdate(context, gm, ids);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        final SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        String confName = Main.getWidgetConfiguration(context, appWidgetId);
        Main.Configuration configuration = new Main.Configuration(sp.getString(confName, "{}"));
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.widget);
//        view.setInt(R.id.background, "setColorFilter", configuration.getValue(Main.Configuration.backgroundColor, 0xff123456));
        view.setTextColor(R.id.title,configuration.getValue(Main.Configuration.textColor, 0xff000000));
        view.setTextViewText(R.id.title, configuration.getValue(Main.Configuration.title, "No Title"));
        view.setImageViewResource(R.id.background, R.drawable.rounded_rect);
        view.setInt(R.id.background, "setColorFilter", configuration.getValue(Main.Configuration.backgroundColor,0xff123456));
        view.setInt(R.id.background, "setAlpha", 192);
        Intent mIntent = new Intent(context, Executor.class);
        mIntent.setAction(Intent.ACTION_MAIN);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.putExtra(Main.configuration, confName);
        PendingIntent pi = PendingIntent.getActivity(context, appWidgetId, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.title, pi);
        appWidgetManager.updateAppWidget(appWidgetId, view);
    }
}