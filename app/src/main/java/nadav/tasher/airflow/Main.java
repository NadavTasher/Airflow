package nadav.tasher.airflow;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nadav.tasher.lightool.Graphics;
import nadav.tasher.lightool.Tunnel;

public class Main extends Activity {
    static Tunnel<Action> bluetoothTunnel = new Tunnel<>();
    static Tunnel<Action> internetTunnel = new Tunnel<>();
    static Tunnel<Action> activateTunnel = new Tunnel<>();
    static Tunnel<String> widgetTunnel = new Tunnel<>();
    static {
        bluetoothTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(Action action) {
                BluetoothAdapter blueAdapter;
                SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
                Configuration configuration = new Configuration(sp.getString(action.config, ""));
                BluetoothManager manager = (BluetoothManager) action.c.getSystemService(Context.BLUETOOTH_SERVICE);
                if (manager != null) {
                    blueAdapter = manager.getAdapter();
                    if (blueAdapter.isEnabled()) {
                        blueAdapter.cancelDiscovery();
                        if (!configuration.getValue(Configuration.deviceName, "").equals("") && !configuration.getValue(Configuration.uuid, "").equals("")) {
                            BluetoothDevice device = blueAdapter.getRemoteDevice(configuration.getValue(Configuration.uuid, ""));
                            UUID uuid = device.getUuids()[0].getUuid();
                            try {
                                Toast.makeText(action.c, "Connecting...", Toast.LENGTH_LONG).show();
                                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                                socket.connect();
                                while (!socket.isConnected()) {
                                    Log.i("Connecting", "...");
                                }
                                Toast.makeText(action.c, "Connected.", Toast.LENGTH_LONG).show();
                                Toast.makeText(action.c, "Sending Data...", Toast.LENGTH_LONG).show();
                                socket.getOutputStream().write(configuration.getValue(Configuration.data, "").getBytes());
                                socket.getOutputStream().flush();
                                socket.getOutputStream().close();
                                socket.close();
                                Toast.makeText(action.c, "Sent Data.", Toast.LENGTH_LONG).show();
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
        activateTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(Action action) {
                SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
                Configuration configuration=new Configuration(sp.getString(action.config,"{}"));
                if(configuration.getValue(Configuration.type,-1)!=-1){
                    if(configuration.getValue(Configuration.type,Configuration.TYPE_BLUETOOTH)==Configuration.TYPE_BLUETOOTH){
                        bluetoothTunnel.send(action);
                    }else{
                        internetTunnel.send(action);
                    }
                }
            }
        });
    }

    static final String qs="qs";
    static final String shortcut="short";
    static final String bluetooth="bluetooth";
    static final String internet="internet";
    static final String configuration="requestedConfig";
    static final String widgets="widget_list";
    static final String widget="widget_";

    static final int color = 0xff568ff1;

    private FrameLayout masterLayout;
    private LinearLayout scrollable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStageA();
    }

    private void initStageA() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        initStageAB();
    }

    private void initStageAB() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                initStageB();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 304);
            }
        } else {
            initStageB();
        }
    }

    private void remakeTaskDescription() {
        Bitmap ico = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(getString(R.string.app_name), ico, color);
        setTaskDescription(taskDescription);
    }

    private void initStageB() {
        masterLayout = new FrameLayout(this);
        Graphics.DragNavigation dragNavigation = new Graphics.DragNavigation(getApplicationContext(), getDrawable(R.drawable.ic_launcher), 0x80333333);
        getWindow().setStatusBarColor(dragNavigation.calculateOverlayedColor(Values.color));
        masterLayout.setBackgroundColor(Values.color);
        scrollable = new LinearLayout(this);
        scrollable.setOrientation(LinearLayout.VERTICAL);
        scrollable.setGravity(Gravity.CENTER);
        scrollable.addView(new View(this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dragNavigation.spacerSize()));
        ScrollView sv = new ScrollView(this);
        sv.addView(scrollable);
        masterLayout.addView(sv);
        masterLayout.addView(dragNavigation);
        LinearLayout actionsView = new LinearLayout(this);
        actionsView.setOrientation(LinearLayout.VERTICAL);
        actionsView.setGravity(Gravity.CENTER);
        Button buttonNewConfig = new Button(this);
        Button buttonChooseQS = new Button(this);
        Button buttonChooseLP = new Button(this);
        Button buttonImportExport = new Button(this);
        buttonNewConfig.setBackground(getDrawable(R.drawable.rounded_rect));
        buttonChooseQS.setBackground(getDrawable(R.drawable.rounded_rect));
        buttonChooseLP.setBackground(getDrawable(R.drawable.rounded_rect));
        buttonImportExport.setBackground(getDrawable(R.drawable.rounded_rect));
        buttonNewConfig.setText("New Configuration");
        buttonChooseQS.setText("Choose QuickSettings Configuration");
        buttonChooseLP.setText("Choose Shortcut Configuration");
        buttonImportExport.setText("Import/Export");
        actionsView.addView(buttonNewConfig);
        actionsView.addView(buttonChooseQS);
        actionsView.addView(buttonChooseLP);
        actionsView.addView(buttonImportExport);
        dragNavigation.setContent(actionsView);
        setContentView(masterLayout);
        remakeTaskDescription();
    }

    static class Values {
        static final int color = 0xff568ff1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 304: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initStageB();
                } else {
                    initStageAB();
                }
                break;
            }
        }
    }
    //    void createShortcut(String widget) {
    //        if (Build.VERSION.SDK_INT >= 25) {
    //            SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
    //            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
    //            shortcutManager.removeAllDynamicShortcuts();
    //            ArrayList<ShortcutInfo> scs = new ArrayList<>();
    //            int higest = 0;
    //            String nn = null;
    //            for (int ww = 0; ww < widgets.size(); ww++) {
    //                if (sp.getInt(widgets.get(ww) + count, 0) > higest) {
    //                    nn = widgets.get(ww);
    //                    higest = sp.getInt(widgets.get(ww) + count, 0);
    //                }
    //            }
    //            if (nn != null) {
    //                String text = sp.getString(nn + ptext, "Send String");
    //                Intent send = new Intent(this, Executor.class);
    //                send.setAction(Intent.ACTION_MAIN);
    //                send.putExtra("config", nn);
    //                send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, nn).setShortLabel(text).setLongLabel(text).setIcon(Icon.createWithResource(this, R.drawable.ic_launcher)).setIntent(send).build();
    //                scs.add(shortcut);
    //                shortcutManager.setDynamicShortcuts(scs);
    //            }
    //        }
    //    }

    void createConfiguration() {
        final Configuration configuration = new Configuration();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("New Configuration");
        final EditText nameText = new EditText(this);
        alert.setView(nameText);
        alert.setPositiveButton("Bluetooth", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                configuration.setValue(Configuration.type, Configuration.TYPE_BLUETOOTH);
                sp.edit().putString(nameText.getText().toString(), configuration.getConfiguration()).apply();
                addConfigurationToList(getApplicationContext(),Configuration.TYPE_BLUETOOTH,nameText.getText().toString());
                editConfiguration(nameText.getText().toString());
            }
        });
        alert.setNegativeButton("Internet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                configuration.setValue(Configuration.type, Configuration.TYPE_INTERNET);
                sp.edit().putString(nameText.getText().toString(), configuration.getConfiguration()).apply();
                addConfigurationToList(getApplicationContext(),Configuration.TYPE_INTERNET,nameText.getText().toString());
                editConfiguration(nameText.getText().toString());
            }
        });
        alert.show();
    }

    static void addConfigurationToList(Context c,int type,String name){
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        if(type==Configuration.TYPE_BLUETOOTH){
            try {
                JSONArray array=new JSONArray(sp.getString(bluetooth,"[]"));
                array.put(name);
                sp.edit().putString(bluetooth,array.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            try {
                JSONArray array=new JSONArray(sp.getString(internet,"[]"));
                array.put(name);
                sp.edit().putString(internet,array.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    static ArrayList<String> getConfigurationsFromList(Context c,int type){
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        ArrayList<String> configs=new ArrayList<>();
        JSONArray array;
        if(type==Configuration.TYPE_BLUETOOTH){
            try {
                array=new JSONArray(sp.getString(bluetooth,"[]"));
            } catch (JSONException e) {
                e.printStackTrace();
                array=new JSONArray();
            }
        }else{
            try {
                array=new JSONArray(sp.getString(internet,"[]"));
            } catch (JSONException e) {
                e.printStackTrace();
                array=new JSONArray();
            }
        }
        for(int av=0;av<array.length();av++){
            try {
                configs.add(String.valueOf(array.get(av)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return configs;
    }

    static void setWidgetConfiguration(Context c,int widgetID,String configuration){
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        try {
            JSONObject widgetList=new JSONObject(sp.getString(widgets,"{}"));
            widgetList.put(widget+widgetID,configuration);
            sp.edit().putString(widgets,widgetList.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static String getWidgetConfiguration(Context c,int widgetID){
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        try {
            JSONObject widgetList=new JSONObject(sp.getString(widgets,"{}"));
            return widgetList.getString(widget+widgetID);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    void editConfiguration(final String name) {
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        final Configuration configuration = new Configuration(sp.getString(name, ""));
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Edit Configuration");
        LinearLayout all = new LinearLayout(this);
        all.setOrientation(LinearLayout.VERTICAL);
        all.setGravity(Gravity.CENTER);
        final EditText titleText = new EditText(this);
        titleText.setHint("Configuration's Title");
        all.addView(titleText);
        EditText bluetoothDataText = new EditText(this);
        bluetoothDataText.setHint("String To Send Over Bluetooth");
        all.addView(bluetoothDataText);
        EditText uuidText = new EditText(this);
        RadioGroup deviceName = new RadioGroup(this);
        EditText urlText = new EditText(this);
        EditText portText = new EditText(this);
        EditText fileText = new EditText(this);
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                sp.edit().putString(name, configuration.getConfiguration()).apply();
            }
        });
        alert.show();
    }

    TextView getText(String te) {
        TextView t = new TextView(this);
        t.setText(te);
        t.setTextColor(Color.WHITE);
        return t;
    }
    //    void newBlueConfig() {
    //        selected_name = null;
    //        selected_address = null;
    //        final SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
    //        AlertDialog.Builder alert = new AlertDialog.Builder(this);
    //        alert.setTitle("Create Configuration");
    //        LinearLayout ll = new LinearLayout(this);
    //        ll.setOrientation(LinearLayout.VERTICAL);
    //        ll.setGravity(Gravity.CENTER_HORIZONTAL);
    //        final EditText text = new EditText(this);
    //        text.setHint("What's Written On Your Widget");
    //        String t = "Send Data " + (new Random().nextInt(100) + 100);
    //        text.setText(t);
    //        final EditText data = new EditText(this);
    //        data.setHint("Data");
    //        ll.addView(text);
    //        ll.addView(data);
    //        ll.addView(getS(datas, data));
    //        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    //        BluetoothAdapter blueAdapter = manager.getAdapter();
    //        if (blueAdapter.isEnabled()) {
    //            RadioGroup grp = new RadioGroup(this);
    //            final BluetoothDevice[] bondedDevices = blueAdapter.getBondedDevices().toArray(new BluetoothDevice[blueAdapter.getBondedDevices().size()]);
    //            for (int i = 0; i < bondedDevices.length; i++) {
    //                RadioButton rb = new RadioButton(this);
    //                rb.setText(bondedDevices[i].getName());
    //                final int fi = i;
    //                rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    //                    @Override
    //                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
    //                        if (b) {
    //                            selected_address = bondedDevices[fi].getAddress();
    //                            selected_name = bondedDevices[fi].getName();
    //                        }
    //                    }
    //                });
    //                grp.addView(rb);
    //            }
    //            ll.addView(grp);
    //        } else {
    //            Toast.makeText(this, "Turn on Bluetooth", Toast.LENGTH_SHORT).show();
    //        }
    //        alert.setView(ll);
    //        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
    //            @Override
    //            public void onClick(DialogInterface dialogInterface, int i) {
    //                String con = Stringer.replaceEvery(text.getText().toString().toLowerCase(), ' ', '_');
    //                ArrayList<String> widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "!!");
    //                if (widgets == null) {
    //                    widgets = new ArrayList<>();
    //                }
    //                widgets.add(con);
    //                String finale = "";
    //                for (int s = 0; s < widgets.size(); s++) {
    //                    if (finale.equals("")) {
    //                        finale = finale + widgets.get(s);
    //                    } else {
    //                        finale = finale + "!!" + widgets.get(s);
    //                    }
    //                }
    //                sp.edit().putString("widgets", finale).apply();
    //                if (!text.getText().toString().equals("")) {
    //                    sp.edit().putString(con + ptext, text.getText().toString()).apply();
    //                }
    //                sp.edit().putString(con + pdata, data.getText().toString()).apply();
    //                if (selected_address != null && selected_name != null) {
    //                    sp.edit().putString(con + pbaddress, selected_address).apply();
    //                    sp.edit().putString(con + pdevname, selected_name).apply();
    //                }
    //                FileFactory.log(log, "Configuration " + con + " Created");
    //                doStart();
    //            }
    //        });
    //        alert.show();
    //    }
    //
    //    void newNetConfig() {
    //        final SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
    //        AlertDialog.Builder alert = new AlertDialog.Builder(this);
    //        alert.setTitle("Create Configuration");
    //        LinearLayout ll = new LinearLayout(this);
    //        ll.setOrientation(LinearLayout.VERTICAL);
    //        ll.setGravity(Gravity.CENTER_HORIZONTAL);
    //        final EditText text = new EditText(this);
    //        text.setHint("What's Written On Your Widget");
    //        String t = "Send Data " + new Random().nextInt(100);
    //        text.setText(t);
    //        final EditText url = new EditText(this);
    //        url.setHint("Your URL");
    //        final EditText port = new EditText(this);
    //        port.setHint("URL:port");
    //        port.setText(getString(R.string.defport));
    //        final EditText comm = new EditText(this);
    //        comm.setHint("Command To Send");
    //        final EditText cgif = new EditText(this);
    //        cgif.setHint("CGI File");
    //        ll.addView(text);
    //        ll.addView(url);
    //        ll.addView(getS(urls, url));
    //        ll.addView(port);
    //        ll.addView(getS(ports, port));
    //        ll.addView(cgif);
    //        ll.addView(getS(cgis, cgif));
    //        ll.addView(comm);
    //        ll.addView(getS(commands, comm));
    //        alert.setView(ll);
    //        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
    //            @Override
    //            public void onClick(DialogInterface dialogInterface, int i) {
    //                String con = Stringer.replaceEvery(text.getText().toString().toLowerCase(), ' ', '_');
    //                ArrayList<String> widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "!!");
    //                if (widgets == null) {
    //                    widgets = new ArrayList<>();
    //                }
    //                widgets.add(con);
    //                String finale = "";
    //                for (int s = 0; s < widgets.size(); s++) {
    //                    if (finale.equals("")) {
    //                        finale = finale + widgets.get(s);
    //                    } else {
    //                        finale = finale + "!!" + widgets.get(s);
    //                    }
    //                }
    //                sp.edit().putString("widgets", finale).apply();
    //                if (!text.getText().toString().equals("")) {
    //                    sp.edit().putString(con + ptext, text.getText().toString()).apply();
    //                }
    //                if (!cgif.getText().toString().equals("")) {
    //                    sp.edit().putString(con + pcgi, cgif.getText().toString()).apply();
    //                    addToList(cgis, cgif.getText().toString());
    //                }
    //                if (!url.getText().toString().equals("")) {
    //                    sp.edit().putString(con + purl, url.getText().toString()).apply();
    //                    addToList(urls, url.getText().toString());
    //                }
    //                if (!port.getText().toString().equals("")) {
    //                    sp.edit().putString(con + pport, port.getText().toString()).apply();
    //                    addToList(ports, port.getText().toString());
    //                }
    //                if (!comm.getText().toString().equals("")) {
    //                    sp.edit().putString(con + pcomm, comm.getText().toString()).apply();
    //                    addToList(commands, comm.getText().toString());
    //                }
    //                FileFactory.log(log, "Configuration " + con + " Created");
    //                doStart();
    //            }
    //        });
    //        alert.show();
    //    }
    //
    //    void export() {
    //        Toast.makeText(this, "File will be ready under " + new File(cache, "widgets.xml").toString(), Toast.LENGTH_LONG).show();
    //        FileFactory.log(log, "Export Main");
    //        SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
    //        ArrayList<String> widgets;
    //        if (sp.getString("widgets", "").contains("::")) {
    //            widgets = Stringer.cutOnEvery(sp.getString("widgets", null), "::");
    //        } else {
    //            widgets = new ArrayList<>();
    //            widgets.add(sp.getString("widgets", null));
    //        }
    //        String s = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
    //        for (int w = 0; w < widgets.size(); w++) {
    //            if (sp.contains(widgets.get(w) + pdata)) {
    //                s = s + "\n" + "<widget_blue>" + widgets.get(w) + "::" + sp.getString(widgets.get(w) + ptext, "none") + "::" + sp.getString(widgets.get(w) + pdata, "none") + "::" + sp.getString(widgets.get(w) + pbaddress, "none") + "::" + sp.getString(widgets.get(w) + pdevname, "none") + "::" + sp.getInt(widgets.get(w) + pcolor, Color.parseColor(colorstart)) + "</widget_blue>";
    //            } else {
    //                s = s + "\n" + "<widget>" + widgets.get(w) + "::" + sp.getString(widgets.get(w) + ptext, "none") + "::" + sp.getString(widgets.get(w) + purl, "none") + "::" + sp.getString(widgets.get(w) + pport, "80") + "::" + sp.getString(widgets.get(w) + pcgi, "none") + "::" + sp.getString(widgets.get(w) + pcomm, "none") + "::" + sp.getInt(widgets.get(w) + pcolor, Color.parseColor(colorstart)) + "</widget>";
    //            }
    //        }
    //        FileFactory.writeToFile(new File(cache, "widgets.xml"), s);
    //        FileFactory.log(log, "Export Finished");
    //    }
    //
    //    void importXML() {
    //        final SharedPreferences sp = getSharedPreferences(getPackageName() + "_PREFS", MODE_PRIVATE);
    //        AlertDialog.Builder alert = new AlertDialog.Builder(this);
    //        alert.setTitle("Import");
    //        alert.setMessage("Make Sure You Have Your 'widgets.xml'\nImport File Ready In " + cache.toString() + ".\nDo You Want To Continue?\nThis Will NOT Overwrite Any Existing\nConfigurations With These Names.");
    //        alert.setPositiveButton("Import", new DialogInterface.OnClickListener() {
    //            @Override
    //            public void onClick(DialogInterface dialogInterface, int i) {
    //                if (new File(cache, "widgets.xml").exists()) {
    //                    FileFactory.log(log, "Import Main");
    //                    ArrayList<XMLFactory.XMLTag> widgets = XMLFactory.read(getApplicationContext(), new File(cache, "widgets.xml"), null, XMLFactory.INTERNAL_STORAGE);
    //                    if (widgets != null) {
    //                        for (int w = 0; w < widgets.size(); w++) {
    //                            ArrayList<String> datas = Stringer.cutOnEvery(widgets.get(w).data, "::");
    //                            String name, text, url, port, cgi, command, color, data, address, devname;
    //                            if (datas.size() == 7) {
    //                                ArrayList<String> widgets2 = Stringer.cutOnEvery(sp.getString("widgets", null), "::");
    //                                if (widgets2 == null) {
    //                                    widgets2 = new ArrayList<>();
    //                                }
    //                                name = datas.get(0);
    //                                for (int wd = 0; wd < widgets2.size(); wd++) {
    //                                    if (widgets2.get(wd).equals(name)) {
    //                                        name = name + "_" + new Random().nextInt(1000);
    //                                        break;
    //                                    }
    //                                }
    //                                text = datas.get(1);
    //                                url = datas.get(2);
    //                                port = datas.get(3);
    //                                cgi = datas.get(4);
    //                                command = datas.get(5);
    //                                color = datas.get(6);
    //                                FileFactory.log(log, "Import: \n" + name + "\n" + text + "\n" + url + "\n" + port + "\n" + cgi + "\n" + command);
    //                                sp.edit().putString(name + ptext, text).apply();
    //                                sp.edit().putString(name + purl, url).apply();
    //                                sp.edit().putString(name + pport, port).apply();
    //                                sp.edit().putString(name + pcgi, cgi).apply();
    //                                sp.edit().putString(name + pcomm, command).apply();
    //                                sp.edit().putInt(name + pcolor, Integer.parseInt(color)).apply();
    //                                addToList(urls, url);
    //                                addToList(ports, port);
    //                                addToList(cgis, cgi);
    //                                addToList(commands, command);
    //                                widgets2.add(name);
    //                                String finale = "";
    //                                for (int s = 0; s < widgets2.size(); s++) {
    //                                    if (finale.equals("")) {
    //                                        finale = finale + widgets2.get(s);
    //                                    } else {
    //                                        finale = finale + "::" + widgets2.get(s);
    //                                    }
    //                                }
    //                                sp.edit().putString("widgets", finale).apply();
    //                            } else if (datas.size() == 6) {
    //                                ArrayList<String> widgets2 = Stringer.cutOnEvery(sp.getString("widgets", null), "::");
    //                                if (widgets2 == null) {
    //                                    widgets2 = new ArrayList<>();
    //                                }
    //                                name = datas.get(0);
    //                                for (int wd = 0; wd < widgets2.size(); wd++) {
    //                                    if (widgets2.get(wd).equals(name)) {
    //                                        name = name + "_" + new Random().nextInt(1000);
    //                                        break;
    //                                    }
    //                                }
    //                                text = datas.get(1);
    //                                data = datas.get(2);
    //                                address = datas.get(3);
    //                                devname = datas.get(4);
    //                                color = datas.get(5);
    //                                FileFactory.log(log, "Import: \n" + name + "\n" + text + "\n" + data + "\n" + address + "\n" + devname);
    //                                sp.edit().putString(name + ptext, text).apply();
    //                                sp.edit().putString(name + pdata, data).apply();
    //                                sp.edit().putString(name + pbaddress, address).apply();
    //                                sp.edit().putString(name + pdevname, devname).apply();
    //                                sp.edit().putInt(name + pcolor, Integer.parseInt(color)).apply();
    //                                addToList(Main.datas, data);
    //                                widgets2.add(name);
    //                                String finale = "";
    //                                for (int s = 0; s < widgets2.size(); s++) {
    //                                    if (finale.equals("")) {
    //                                        finale = finale + widgets2.get(s);
    //                                    } else {
    //                                        finale = finale + "::" + widgets2.get(s);
    //                                    }
    //                                }
    //                                sp.edit().putString("widgets", finale).apply();
    //                            } else {
    //                                FileFactory.log(log, "Widget Import Failed");
    //                                Toast.makeText(getApplicationContext(), "Failed To Import Widget", Toast.LENGTH_SHORT).show();
    //                            }
    //                        }
    //                    } else {
    //                        FileFactory.log(log, "Import Failed");
    //                    }
    //                } else {
    //                    Toast.makeText(getApplicationContext(), "File Not Available", Toast.LENGTH_SHORT).show();
    //                }
    //                doStart();
    //            }
    //        });
    //        alert.setNegativeButton("Cancel", null);
    //        alert.show();
    //    }

    static class Action {
        Context c;
        String config;

        public Action(Context c, String configurationName) {
            this.c = c;
            config = configurationName;
        }
    }

    static class Configuration {
        static final int TYPE_BLUETOOTH = 1;
        static final int TYPE_INTERNET = 0;
        static final String url = "internet_address";
        static final String type = "type";
        static final String title = "title";
        static final String port = "internet_port";
        static final String data = "bluetooth_string";
        static final String uuid = "bluetooth_uuid";
        static final String backgroundColor = "back_color";
        static final String textColor = "text_color";
        static final String file = "internet_file";
        static final String count = "uses";
        static final String deviceName = "bluetooth_device_name";
        JSONObject config = null;

        public Configuration(String json) {
            try {
                config = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
                config = new JSONObject();
            }
        }

        public Configuration() {
            config = new JSONObject();
        }

        public String getConfiguration() {
            return config.toString();
        }

        public void setValue(String name, String value) {
            try {
                config.put(name, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void setValue(String name, int value) {
            try {
                config.put(name, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void setValue(String name, boolean value) {
            try {
                config.put(name, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public int getValue(String name, int defval) {
            if (config.has(name)) {
                try {
                    return config.getInt(name);
                } catch (JSONException e) {
                    return defval;
                }
            } else {
                return defval;
            }
        }

        public String getValue(String name, String defval) {
            if (config.has(name)) {
                try {
                    return config.getString(name);
                } catch (JSONException e) {
                    return defval;
                }
            } else {
                return defval;
            }
        }

        public boolean getValue(String name, boolean defval) {
            if (config.has(name)) {
                try {
                    return config.getBoolean(name);
                } catch (JSONException e) {
                    return defval;
                }
            } else {
                return defval;
            }
        }
    }
}