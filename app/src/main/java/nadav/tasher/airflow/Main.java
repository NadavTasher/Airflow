package nadav.tasher.airflow;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import nadav.tasher.lightool.Tunnel;
import nadav.tasher.toollib.Animations;
import nadav.tasher.toollib.Extras;
import nadav.tasher.toollib.FileFactory;
import nadav.tasher.toollib.Stringer;
import nadav.tasher.toollib.XMLFactory;

public class Main extends Activity {
    static Tunnel<Action> bluetoothTunnel=new Tunnel<>();
    static Tunnel<Action> internetTunnel=new Tunnel<>();
    static{
        bluetoothTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(Action action) {
                BluetoothAdapter blueAdapter;
                SharedPreferences sp=action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
                Configuration configuration=new Configuration(sp.getString(action.config,""));
                BluetoothManager manager=(BluetoothManager)action.c.getSystemService(Context.BLUETOOTH_SERVICE);
                if(manager!=null) {
                    blueAdapter = manager.getAdapter();
                    if (blueAdapter.isEnabled()) {
                        blueAdapter.cancelDiscovery();
                        if (!configuration.getValue(Configuration.deviceName,"") .equals("")&&!configuration.getValue(Configuration.uuid,"").equals("")) {
                            BluetoothDevice device = blueAdapter.getRemoteDevice(configuration.getValue(Configuration.uuid,""));
                            UUID uuid = device.getUuids()[0].getUuid();
                            try {
                                Toast.makeText(action.c,"Connecting...",Toast.LENGTH_LONG).show();
                                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                                socket.connect();
                                while(!socket.isConnected()){
                                    Log.i("Connecting","...");
                                }
                                Toast.makeText(action.c,"Connected.",Toast.LENGTH_LONG).show();
                                Toast.makeText(action.c,"Sending Data...",Toast.LENGTH_LONG).show();
                                socket.getOutputStream().write(configuration.getValue(Configuration.data,"").getBytes());
                                socket.getOutputStream().flush();
                                socket.getOutputStream().close();
                                socket.close();
                                Toast.makeText(action.c,"Sent Data.",Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        Toast.makeText(action.c, "Turn On Bluetooth", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        internetTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(Action action) {
            }
        });
    }
    String colorstart = "#0074C6";
    private Drawable iconDr;
    private String selected_address;
    private String selected_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setup();
        start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 304: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doStart();
                } else {
                    finish();
                }
                break;
            }
        }
    }

    void setup() {
        iconDr = getDrawable(R.drawable.ic_launcher);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    void doStart() {
        final Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor(colorstart));
        window.setNavigationBarColor(Color.parseColor(colorstart));
        final Bitmap ico = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(getString(R.string.app_name), ico, Color.parseColor(colorstart));
        setTaskDescription(taskDescription);
        home();
    }

    void start() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                doStart();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 304);
            }
        } else {
            doStart();
        }
    }

    void createShortcut(ArrayList<String> widgets) {
        if (Build.VERSION.SDK_INT >= 25) {
            SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            shortcutManager.removeAllDynamicShortcuts();
            ArrayList<ShortcutInfo> scs = new ArrayList<>();
            int higest = 0;
            String nn = null;
            for (int ww = 0; ww < widgets.size(); ww++) {
                if (sp.getInt(widgets.get(ww) + count, 0) > higest) {
                    nn = widgets.get(ww);
                    higest = sp.getInt(widgets.get(ww) + count, 0);
                }
            }
            if (nn != null) {
                String text = sp.getString(nn + ptext, "Send String");
                Intent send = new Intent(this, SendActivity.class);
                send.setAction(Intent.ACTION_MAIN);
                send.putExtra("config", nn);
                send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, nn).setShortLabel(text).setLongLabel(text).setIcon(Icon.createWithResource(this, R.drawable.ic_launcher)).setIntent(send).build();
                scs.add(shortcut);
                shortcutManager.setDynamicShortcuts(scs);
            }
        }
    }

    void home() {
        SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
        ArrayList<String> widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "!!");
        if (widgets == null) {
            widgets = new ArrayList<>();
        }
        LinearLayout actualAll = new LinearLayout(this);
        actualAll.setOrientation(LinearLayout.VERTICAL);
        actualAll.setGravity(Gravity.START);
        final LinearLayout navbarAll = new LinearLayout(this);
        navbarAll.setBackgroundColor(Color.parseColor(colorstart));
        navbarAll.setOrientation(LinearLayout.HORIZONTAL);
        navbarAll.setGravity(Gravity.CENTER);
        final ImageView nutIcon = new ImageView(this);
        final int screenY = Extras.screenY(this);
        final int nutSize = (screenY / 8) - screenY / 30;
        LinearLayout.LayoutParams nutParms = new LinearLayout.LayoutParams(nutSize, nutSize);
        nutIcon.setLayoutParams(nutParms);
        nutIcon.setImageDrawable(getDrawable(R.drawable.ic_launcher));
        final ObjectAnimator anim = ObjectAnimator.ofFloat(nutIcon, View.TRANSLATION_X, Animations.VIBRATE_SMALL);
        anim.setDuration(1500);
        anim.setRepeatMode(ObjectAnimator.RESTART);
        anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.start();
        navbarAll.addView(nutIcon);
        int navY = screenY / 8;
        getWindow().setNavigationBarColor(Color.parseColor(colorstart));
        LinearLayout.LayoutParams navParms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, navY);
        navParms.gravity = Gravity.START;
        navbarAll.setLayoutParams(navParms);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor(colorstart));
        actualAll.setBackgroundColor(Color.parseColor(colorstart));
        actualAll.addView(navbarAll);
        actualAll.addView(sv);
        LinearLayout ll = new LinearLayout(this);
        ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ll.setBackgroundColor(Color.parseColor(colorstart));
        ll.setGravity(Gravity.CENTER);
        ll.setOrientation(LinearLayout.VERTICAL);
        for (int w = 0; w < widgets.size(); w++) {
            if (sp.contains(widgets.get(w) + pdata)) {
                ll.addView(getBlueConfig(widgets.get(w)));
            } else {
                ll.addView(getNetConfig(widgets.get(w)));
            }
        }
        createShortcut(widgets);
        Button add = new Button(this);
        add.setText(R.string.add);
        add.setBackground(null);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newConfig();
            }
        });
        Button export = new Button(this);
        export.setBackground(null);
        export.setText(R.string.export);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                export();
            }
        });
        Button importXML = new Button(this);
        importXML.setText(R.string.importXML);
        importXML.setBackground(null);
        importXML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                importXML();
            }
        });
        Button update = new Button(this);
        update.setBackground(null);
        update.setText(R.string.update);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent("nadav.tasher.airflow.UPDATE"));
            }
        });
        ll.addView(add);
        ll.addView(export);
        ll.addView(importXML);
        ll.addView(update);
        sv.addView(ll);
        setContentView(actualAll);
    }

    void createConfiguration(boolean nameEditable,String configurationName) {
        Configuration configuration=new Configuration();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("New Configuration");
        LinearLayout all=new LinearLayout(this);
        all.setOrientation(LinearLayout.VERTICAL);
        all.setGravity(Gravity.CENTER);
        all.addView(getText("Configuration Type:"));
        RadioGroup typeGroup=new RadioGroup(this);
        RadioButton typeBluetooth=new RadioButton(this);
        RadioButton typeInternet=new RadioButton(this);
        typeGroup.addView(typeBluetooth);
        typeGroup.addView(typeInternet);
        EditText nameText=new EditText(this);
        EditText dataText=new EditText(this);
        EditText uuidText=new EditText(this);
        RadioGroup deviceName=new RadioGroup(this);
        EditText urlText=new EditText(this);
        EditText portText=new EditText(this);
        EditText fileText=new EditText(this);
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp=getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                sp.edit().putString()
            }
        });
        alert.show();
    }

    TextView getText(String te){
        TextView t=new TextView(this);
        t.setText(te);
        t.setTextColor(Color.WHITE);
        return t;
    }

    void newBlueConfig() {
        selected_name = null;
        selected_address = null;
        final SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Create Configuration");
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        final EditText text = new EditText(this);
        text.setHint("What's Written On Your Widget");
        String t = "Send Data " + (new Random().nextInt(100) + 100);
        text.setText(t);
        final EditText data = new EditText(this);
        data.setHint("Data");
        ll.addView(text);
        ll.addView(data);
        ll.addView(getS(datas, data));
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter blueAdapter = manager.getAdapter();
        if (blueAdapter.isEnabled()) {
            RadioGroup grp = new RadioGroup(this);
            final BluetoothDevice[] bondedDevices = blueAdapter.getBondedDevices().toArray(new BluetoothDevice[blueAdapter.getBondedDevices().size()]);
            for (int i = 0; i < bondedDevices.length; i++) {
                RadioButton rb = new RadioButton(this);
                rb.setText(bondedDevices[i].getName());
                final int fi = i;
                rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            selected_address = bondedDevices[fi].getAddress();
                            selected_name = bondedDevices[fi].getName();
                        }
                    }
                });
                grp.addView(rb);
            }
            ll.addView(grp);
        } else {
            Toast.makeText(this, "Turn on Bluetooth", Toast.LENGTH_SHORT).show();
        }
        alert.setView(ll);
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String con = Stringer.replaceEvery(text.getText().toString().toLowerCase(), ' ', '_');
                ArrayList<String> widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "!!");
                if (widgets == null) {
                    widgets = new ArrayList<>();
                }
                widgets.add(con);
                String finale = "";
                for (int s = 0; s < widgets.size(); s++) {
                    if (finale.equals("")) {
                        finale = finale + widgets.get(s);
                    } else {
                        finale = finale + "!!" + widgets.get(s);
                    }
                }
                sp.edit().putString("widgets", finale).apply();
                if (!text.getText().toString().equals("")) {
                    sp.edit().putString(con + ptext, text.getText().toString()).apply();
                }
                sp.edit().putString(con + pdata, data.getText().toString()).apply();
                if (selected_address != null && selected_name != null) {
                    sp.edit().putString(con + pbaddress, selected_address).apply();
                    sp.edit().putString(con + pdevname, selected_name).apply();
                }
                FileFactory.log(log, "Configuration " + con + " Created");
                doStart();
            }
        });
        alert.show();
    }

    void newNetConfig() {
        final SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Create Configuration");
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        final EditText text = new EditText(this);
        text.setHint("What's Written On Your Widget");
        String t = "Send Data " + new Random().nextInt(100);
        text.setText(t);
        final EditText url = new EditText(this);
        url.setHint("Your URL");
        final EditText port = new EditText(this);
        port.setHint("URL:port");
        port.setText(getString(R.string.defport));
        final EditText comm = new EditText(this);
        comm.setHint("Command To Send");
        final EditText cgif = new EditText(this);
        cgif.setHint("CGI File");
        ll.addView(text);
        ll.addView(url);
        ll.addView(getS(urls, url));
        ll.addView(port);
        ll.addView(getS(ports, port));
        ll.addView(cgif);
        ll.addView(getS(cgis, cgif));
        ll.addView(comm);
        ll.addView(getS(commands, comm));
        alert.setView(ll);
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String con = Stringer.replaceEvery(text.getText().toString().toLowerCase(), ' ', '_');
                ArrayList<String> widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "!!");
                if (widgets == null) {
                    widgets = new ArrayList<>();
                }
                widgets.add(con);
                String finale = "";
                for (int s = 0; s < widgets.size(); s++) {
                    if (finale.equals("")) {
                        finale = finale + widgets.get(s);
                    } else {
                        finale = finale + "!!" + widgets.get(s);
                    }
                }
                sp.edit().putString("widgets", finale).apply();
                if (!text.getText().toString().equals("")) {
                    sp.edit().putString(con + ptext, text.getText().toString()).apply();
                }
                if (!cgif.getText().toString().equals("")) {
                    sp.edit().putString(con + pcgi, cgif.getText().toString()).apply();
                    addToList(cgis, cgif.getText().toString());
                }
                if (!url.getText().toString().equals("")) {
                    sp.edit().putString(con + purl, url.getText().toString()).apply();
                    addToList(urls, url.getText().toString());
                }
                if (!port.getText().toString().equals("")) {
                    sp.edit().putString(con + pport, port.getText().toString()).apply();
                    addToList(ports, port.getText().toString());
                }
                if (!comm.getText().toString().equals("")) {
                    sp.edit().putString(con + pcomm, comm.getText().toString()).apply();
                    addToList(commands, comm.getText().toString());
                }
                FileFactory.log(log, "Configuration " + con + " Created");
                doStart();
            }
        });
        alert.show();
    }

    void export() {
        Toast.makeText(this, "File will be ready under " + new File(cache, "widgets.xml").toString(), Toast.LENGTH_LONG).show();
        FileFactory.log(log, "Export Main");
        SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
        ArrayList<String> widgets;
        if (sp.getString("widgets", "").contains("::")) {
            widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "::");
        } else {
            widgets = new ArrayList<>();
            widgets.add(sp.getString("widgets", null));
        }
        String s = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
        for (int w = 0; w < widgets.size(); w++) {
            if (sp.contains(widgets.get(w) + pdata)) {
                s = s + "\n" + "<widget_blue>" + widgets.get(w) + "::" + sp.getString(widgets.get(w) + ptext, "none") + "::" + sp.getString(widgets.get(w) + pdata, "none") + "::" + sp.getString(widgets.get(w) + pbaddress, "none") + "::" + sp.getString(widgets.get(w) + pdevname, "none") + "::" + sp.getInt(widgets.get(w) + pcolor, Color.parseColor(colorstart)) + "</widget_blue>";
            } else {
                s = s + "\n" + "<widget>" + widgets.get(w) + "::" + sp.getString(widgets.get(w) + ptext, "none") + "::" + sp.getString(widgets.get(w) + purl, "none") + "::" + sp.getString(widgets.get(w) + pport, "80") + "::" + sp.getString(widgets.get(w) + pcgi, "none") + "::" + sp.getString(widgets.get(w) + pcomm, "none") + "::" + sp.getInt(widgets.get(w) + pcolor, Color.parseColor(colorstart)) + "</widget>";
            }
        }
        FileFactory.writeToFile(new File(cache, "widgets.xml"), s);
        FileFactory.log(log, "Export Finished");
    }

    void importXML() {
        final SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Import");
        alert.setMessage("Make Sure You Have Your 'widgets.xml'\nImport File Ready In " + cache.toString() + ".\nDo You Want To Continue?\nThis Will NOT Overwrite Any Existing\nConfigurations With These Names.");
        alert.setPositiveButton("Import", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (new File(cache, "widgets.xml").exists()) {
                    FileFactory.log(log, "Import Main");
                    ArrayList<XMLFactory.XMLTag> widgets = XMLFactory.read(getApplicationContext(), new File(cache, "widgets.xml"), null, XMLFactory.INTERNAL_STORAGE);
                    if (widgets != null) {
                        for (int w = 0; w < widgets.size(); w++) {
                            ArrayList<String> datas = Stringer.cutOnEvery(widgets.get(w).data, "::");
                            String name, text, url, port, cgi, command, color, data, address, devname;
                            if (datas.size() == 7) {
                                ArrayList<String> widgets2 = Stringer.cutOnEvery(sp.getString("widgets", null), "::");
                                if (widgets2 == null) {
                                    widgets2 = new ArrayList<>();
                                }
                                name = datas.get(0);
                                for (int wd = 0; wd < widgets2.size(); wd++) {
                                    if (widgets2.get(wd).equals(name)) {
                                        name = name + "_" + new Random().nextInt(1000);
                                        break;
                                    }
                                }
                                text = datas.get(1);
                                url = datas.get(2);
                                port = datas.get(3);
                                cgi = datas.get(4);
                                command = datas.get(5);
                                color = datas.get(6);
                                FileFactory.log(log, "Import: \n" + name + "\n" + text + "\n" + url + "\n" + port + "\n" + cgi + "\n" + command);
                                sp.edit().putString(name + ptext, text).apply();
                                sp.edit().putString(name + purl, url).apply();
                                sp.edit().putString(name + pport, port).apply();
                                sp.edit().putString(name + pcgi, cgi).apply();
                                sp.edit().putString(name + pcomm, command).apply();
                                sp.edit().putInt(name + pcolor, Integer.parseInt(color)).apply();
                                addToList(urls, url);
                                addToList(ports, port);
                                addToList(cgis, cgi);
                                addToList(commands, command);
                                widgets2.add(name);
                                String finale = "";
                                for (int s = 0; s < widgets2.size(); s++) {
                                    if (finale.equals("")) {
                                        finale = finale + widgets2.get(s);
                                    } else {
                                        finale = finale + "::" + widgets2.get(s);
                                    }
                                }
                                sp.edit().putString("widgets", finale).apply();
                            } else if (datas.size() == 6) {
                                ArrayList<String> widgets2 = Stringer.cutOnEvery(sp.getString("widgets", null), "::");
                                if (widgets2 == null) {
                                    widgets2 = new ArrayList<>();
                                }
                                name = datas.get(0);
                                for (int wd = 0; wd < widgets2.size(); wd++) {
                                    if (widgets2.get(wd).equals(name)) {
                                        name = name + "_" + new Random().nextInt(1000);
                                        break;
                                    }
                                }
                                text = datas.get(1);
                                data = datas.get(2);
                                address = datas.get(3);
                                devname = datas.get(4);
                                color = datas.get(5);
                                FileFactory.log(log, "Import: \n" + name + "\n" + text + "\n" + data + "\n" + address + "\n" + devname);
                                sp.edit().putString(name + ptext, text).apply();
                                sp.edit().putString(name + pdata, data).apply();
                                sp.edit().putString(name + pbaddress, address).apply();
                                sp.edit().putString(name + pdevname, devname).apply();
                                sp.edit().putInt(name + pcolor, Integer.parseInt(color)).apply();
                                addToList(Main.datas, data);
                                widgets2.add(name);
                                String finale = "";
                                for (int s = 0; s < widgets2.size(); s++) {
                                    if (finale.equals("")) {
                                        finale = finale + widgets2.get(s);
                                    } else {
                                        finale = finale + "::" + widgets2.get(s);
                                    }
                                }
                                sp.edit().putString("widgets", finale).apply();
                            } else {
                                FileFactory.log(log, "Widget Import Failed");
                                Toast.makeText(getApplicationContext(), "Failed To Import Widget", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        FileFactory.log(log, "Import Failed");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "File Not Available", Toast.LENGTH_SHORT).show();
                }
                doStart();
            }
        });
        alert.setNegativeButton("Cancel", null);
        alert.show();
    }

    class Action{
        Context c;
        String config;
        public Action(Context c,String configurationName){
            this.c=c;
            config=configurationName;
        }
    }
    static class Configuration{
        static final int TYPE_BLUETOOTH=1;
        static final int TYPE_INTERNET=0;
        static final String url = "url";
        static final String type = "type";
        static final String command = "command";
        static final String title = "title";
        static final String port = "port";
        static final String data = "data";
        static final String uuid = "uuid";
        static final String backgroundColor = "back_color";
        static final String textColor = "text_color";
        static final String file = "file";
        static final String commands = "commands";
        static final String count = "uses";
        static final String deviceName = "deviceName";
        JSONObject config=null;
        public Configuration(String json){
            try {
                config=new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
                config=new JSONObject();
            }
        }

        public Configuration(){
            config=new JSONObject();
        }

        public String getConfiguration(){
            return config.toString();
        }

        public void setValue(String name,String value){
            try {
                config.put(name,value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public void setValue(String name,int value){
            try {
                config.put(name,value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public void setValue(String name,boolean value){
            try {
                config.put(name,value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public int getValue(String name,int defval){
            if(config.has(name)){
                try {
                    return config.getInt(name);
                } catch (JSONException e) {
                    return defval;
                }
            }else{
                return defval;
            }
        }
        public String getValue(String name,String defval){
            if(config.has(name)){
                try {
                    return config.getString(name);
                } catch (JSONException e) {
                    return defval;
                }
            }else{
                return defval;
            }
        }
        public boolean getValue(String name,boolean defval){
            if(config.has(name)){
                try {
                    return config.getBoolean(name);
                } catch (JSONException e) {
                    return defval;
                }
            }else{
                return defval;
            }
        }
    }
}