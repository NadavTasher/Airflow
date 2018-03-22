package nadav.tasher.airflow;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.util.Log;

import nadav.tasher.lightool.Tunnel;

@TargetApi(Build.VERSION_CODES.N)
public class Tile extends TileService {
    Tunnel.OnTunnel<String> mTunnelReceiver = new Tunnel.OnTunnel<String>() {
        @Override
        public void onReceive(String s) {
            updateTile(s);
        }
    };
    Main.Configuration configuration;
    private void updateTile(String s) {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String wid = sp.getString(Main.qs, null);
        if (wid != null) {
            configuration = new Main.Configuration(sp.getString(wid, "{}"));
            getQsTile().setLabel(configuration.getValue(Main.Configuration.title, "No Title"));
            getQsTile().updateTile();
            Log.i("Tile", "Updated, Cause: " + s);
        } else {
            Log.i("Tile", "Not Updated. Cause For Try: " + s);
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        Main.activateTunnel.send(new Main.Action(getApplicationContext(),configuration.getValue(Main.Configuration.name,"")));
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Main.widgetTunnel.addReceiver(mTunnelReceiver);
        updateTile("Started Service");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Main.widgetTunnel.removeReceiver(mTunnelReceiver);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile("Tile Creation");
    }
}
