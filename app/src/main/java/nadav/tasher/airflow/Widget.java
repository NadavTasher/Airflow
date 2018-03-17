package nadav.tasher.airflow;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import static android.content.Context.MODE_PRIVATE;

public class Widget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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
        view.setInt(R.id.background, "setColorFilter", configuration.getValue(Main.Configuration.backgroundColor, 0xff123456));
        view.setInt(R.id.title, "setTextColor", configuration.getValue(Main.Configuration.textColor, 0xff000000));
        view.setString(R.id.title, "setText", configuration.getValue(Main.Configuration.title, "No Title"));
        Intent mIntent = new Intent(context, Executor.class);
        mIntent.setAction(Intent.ACTION_MAIN);
        mIntent.putExtra(Main.configuration, confName);
        PendingIntent pi = PendingIntent.getActivity(context, appWidgetId, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.title, pi);
        appWidgetManager.updateAppWidget(appWidgetId, view);
    }
}