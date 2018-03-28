package nadav.tasher.airflow;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import nadav.tasher.lightool.Device;
import nadav.tasher.lightool.Graphics;
import nadav.tasher.lightool.Net;
import nadav.tasher.lightool.Tunnel;

public class Main extends Activity {
    static final String qs = "qs";
    static final String importExportIndex = "index";
    static final String importExportConfigurations = "configurations";
    static final String bluetooth = "bluetooth";
    static final String internet = "internet";
    static final String configuration = "requestedConfig";
    static final String widgets = "widget_list";
    static final String widget = "widget_";
    static final int color = 0xff568ff1;
    static final int coasterColor = 0xff2245dd;
    static final int textSize = 23;
    static Tunnel<Action> bluetoothTunnel = new Tunnel<>();
    static Tunnel<Action> internetTunnel = new Tunnel<>();
    static Tunnel<Action> activateTunnel = new Tunnel<>();
    static Tunnel<ArrayList<Configuration>> configurationChangeTunnel = new Tunnel<>();
    static Tunnel<String> widgetTunnel = new Tunnel<>();
    static Tunnel<Status> statusTunnel = new Tunnel<>();

    static {
        bluetoothTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(final Action action) {
                BluetoothManager manager = (BluetoothManager) action.c.getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter bluetoothAdapter;
                if (manager != null) {
                    bluetoothAdapter = manager.getAdapter();
                    if (bluetoothAdapter.isEnabled()) {
                        BluetoothSession session = new BluetoothSession(action, new BluetoothSession.OnSessionEnd() {
                            @Override
                            public void onSessionEnd(Status result) {
                                statusTunnel.send(result);
                                if (result.status == Status.STATUS_SUCCEDED) {
                                    Toast.makeText(action.c, "Sent.", Toast.LENGTH_LONG).show();
                                } else if (result.status == Status.STATUS_FAILED) {
                                    Toast.makeText(action.c, "Failed.", Toast.LENGTH_LONG).show();
                                } else if (result.status == Status.STATUS_IN_PROGRESS) {
                                    Toast.makeText(action.c, "Not Finished.", Toast.LENGTH_LONG).show();
                                } else if (result.status == Status.STATUS_STARTING) {
                                    Toast.makeText(action.c, "Failed To Start.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        session.execute();
                    }else{
                        Toast.makeText(action.c, "Bluetooth Is Off", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(action.c, "Device Does Not Support Bluetooth / App Isn't Granted Bluetooth Permission.", Toast.LENGTH_LONG).show();
                }
            }
        });
        internetTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {

            Action action;

            private void verbose(String s) {
                if (action.verboser != null) action.verboser.send(s);
            }

            @Override
            public void onReceive(final Action action) {
                this.action = action;
                SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
                final Configuration configuration = new Configuration(sp.getString(action.config, "{}"));
                Net.Request.RequestParameter[] parms;
                verbose("Trying To Parse Parameters");
                try {
                    JSONArray requestParms = new JSONArray(configuration.getValue(Configuration.requestParameters, "[]"));
                    parms = new Net.Request.RequestParameter[requestParms.length()];
                    for (int rp = 0; rp < requestParms.length(); rp++) {
                        JSONObject jo = new JSONObject(requestParms.getString(rp));
                        parms[rp] = new Net.Request.RequestParameter(jo.getString("name"), jo.getString("value"));
                    }
                    verbose("Parameters Parsed");
                } catch (JSONException e) {
                    e.printStackTrace();
                    verbose("Failed Parsing Parameters, Running With None");
                    parms = new Net.Request.RequestParameter[0];
                }
                if (configuration.getValue(Configuration.method, Configuration.METHOD_INTERNET_GET).equals(Configuration.METHOD_INTERNET_GET)) {
                    verbose("Creating AsyncTask");
                    Net.Request.Get request = new Net.Request.Get(configuration.getValue(Configuration.urlBase, "") + ":" + configuration.getValue(Configuration.port, 80) + configuration.getValue(Configuration.urlPath, "/"), parms, new Net.Request.OnRequest() {
                        @Override
                        public void onRequest(String s) {
                            if (action.verboser == null) {
                                if (configuration.getValue(Configuration.displayOutput, false)) {
                                    Toast.makeText(action.c, s, Toast.LENGTH_LONG).show();
                                } else {
                                    if (s == null) {
                                        Toast.makeText(action.c, "Failed.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(action.c, "Sent.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                if (s == null) {
                                    verbose("Operation Failed");
                                } else {
                                    verbose("Operation Succeeded, Result:\n\"" + s + "\"");
                                }
                                verbose("Done");
                            }
                        }
                    });
                    verbose("AsyncTask Created");
                    verbose("Checking Network State");
                    if (Device.isOnline(action.c)) {
                        verbose("Device Is Online");
                        verbose("Running AsyncTask");
                        request.execute();
                    } else {
                        verbose("Device Offline, Canceling Execution");
                        verbose("Operation Failed");
                        if (action.verboser == null)
                            Toast.makeText(action.c, "Device Offline.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    verbose("Creating AsyncTask");
                    Net.Request.Post request = new Net.Request.Post(configuration.getValue(Configuration.urlBase, "") + ":" + configuration.getValue(Configuration.port, 80) + configuration.getValue(Configuration.urlPath, "/"), parms, new Net.Request.OnRequest() {
                        @Override
                        public void onRequest(String s) {
                            if (action.verboser == null) {
                                if (configuration.getValue(Configuration.displayOutput, false)) {
                                    Toast.makeText(action.c, s, Toast.LENGTH_LONG).show();
                                } else {
                                    if (s == null) {
                                        Toast.makeText(action.c, "Failed.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(action.c, "Sent.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                if (s == null) {
                                    verbose("Operation Failed");
                                } else {
                                    verbose("Operation Succeeded, Result:\n\"" + s + "\"");
                                }
                                verbose("Done");
                            }
                        }
                    });
                    verbose("AsyncTask Created");
                    verbose("Checking Network State");
                    if (Device.isOnline(action.c)) {
                        verbose("Device Is Online");
                        verbose("Running AsyncTask");
                        request.execute();
                    } else {
                        verbose("Device Offline, Canceling Execution");
                        verbose("Operation Failed");
                        if (action.verboser == null)
                            Toast.makeText(action.c, "Device Offline.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        activateTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(Action action) {
                SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
                Configuration configuration = new Configuration(sp.getString(action.config, "{}"));
                if (configuration.getValue(Configuration.type, -1) != -1) {
                    if (configuration.getValue(Configuration.type, Configuration.TYPE_BLUETOOTH) == Configuration.TYPE_BLUETOOTH) {
                        bluetoothTunnel.send(action);
                    } else {
                        internetTunnel.send(action);
                    }
                }
            }
        });
    }

    private FrameLayout masterLayout;
    private LinearLayout scrollable, itemScroll;
    private BluetoothManager manager;
    private BluetoothAdapter bluetoothAdapter;
    private TextView guideText;

    static void addConfigurationToList(Context c, int type, String name) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        if (type == Configuration.TYPE_BLUETOOTH) {
            try {
                JSONArray array = new JSONArray(sp.getString(bluetooth, "[]"));
                array.put(name);
                sp.edit().putString(bluetooth, array.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                JSONArray array = new JSONArray(sp.getString(internet, "[]"));
                array.put(name);
                sp.edit().putString(internet, array.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    static void removeConfigurationFromList(Context c, int type, String name) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        if (type == Configuration.TYPE_BLUETOOTH) {
            try {
                JSONArray array = new JSONArray(sp.getString(bluetooth, "[]"));
                for (int a = 0; a < array.length(); a++) {
                    if (array.getString(a).equals(name)) {
                        array.remove(a);
                    }
                }
                sp.edit().putString(bluetooth, array.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                JSONArray array = new JSONArray(sp.getString(internet, "[]"));
                for (int a = 0; a < array.length(); a++) {
                    if (array.getString(a).equals(name)) {
                        array.remove(a);
                    }
                }
                sp.edit().putString(internet, array.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    static ArrayList<String> getConfigurationsFromList(Context c, int type) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        ArrayList<String> configs = new ArrayList<>();
        JSONArray array;
        if (type == Configuration.TYPE_BLUETOOTH) {
            try {
                array = new JSONArray(sp.getString(bluetooth, "[]"));
            } catch (JSONException e) {
                e.printStackTrace();
                array = new JSONArray();
            }
        } else {
            try {
                array = new JSONArray(sp.getString(internet, "[]"));
            } catch (JSONException e) {
                e.printStackTrace();
                array = new JSONArray();
            }
        }
        for (int av = 0; av < array.length(); av++) {
            try {
                configs.add(String.valueOf(array.get(av)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return configs;
    }

    static void setWidgetConfiguration(Context c, int widgetID, String configuration) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        try {
            JSONObject widgetList = new JSONObject(sp.getString(widgets, "{}"));
            widgetList.put(widget + widgetID, configuration);
            sp.edit().putString(widgets, widgetList.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static String getWidgetConfiguration(Context c, int widgetID) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        try {
            JSONObject widgetList = new JSONObject(sp.getString(widgets, "{}"));
            if (widgetList.has(widget + widgetID)) {
                return widgetList.getString(widget + widgetID);
            } else {
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void share(Context c, String text, String title) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/*");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        c.startActivity(Intent.createChooser(sharingIntent, title));
    }

    private void removeConfiguration(Context c, String name) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        Configuration configuration = new Configuration(sp.getString(name, "{}"));
        removeConfigurationFromList(c, configuration.getValue(Configuration.type, Configuration.TYPE_BLUETOOTH), name);
        sp.edit().remove(name).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStageA();
    }

    private void initStageA() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        initStageB();
    }

    private void remakeTaskDescription(int color) {
        Bitmap ico = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(getString(R.string.app_name), ico, color);
        setTaskDescription(taskDescription);
    }

    private String generateExport() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        JSONObject output = new JSONObject();
        try {
            JSONArray index = new JSONArray();
            final ArrayList<String> confs = new ArrayList<>();
            confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
            confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
            for (int a = 0; a < confs.size(); a++) {
                index.put(confs.get(a));
            }
            output.put(importExportIndex, index);
            JSONObject configs = new JSONObject();
            for (int a = 0; a < confs.size(); a++) {
                configs.put(confs.get(a), new Configuration(sp.getString(confs.get(a), "{}")).getConfiguration());
            }
            output.put(importExportConfigurations, configs);
        } catch (JSONException ignored) {
        }
        return output.toString();
    }

    private void importConfigurations(String json) {
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        try {
            JSONObject input = new JSONObject(json);
            if (input.has(importExportConfigurations) && input.has(importExportIndex)) {
                JSONArray index = input.getJSONArray(importExportIndex);
                JSONObject configurations = input.getJSONObject(importExportConfigurations);
                for (int c = 0; c < index.length(); c++) {
                    String originalName = index.getString(c);
                    String name = index.getString(c);
                    if (generateList().contains(name)) {
                        name += "_" + new Random().nextInt(10);
                        while (generateList().contains(name)) {
                            name += new Random().nextInt(10);
                        }
                    }
                    JSONObject config = new JSONObject(configurations.getString(originalName));
                    Configuration configuration = new Configuration(config.toString());
                    configuration.setValue(Configuration.name, name);
                    addConfigurationToList(getApplicationContext(), configuration.getValue(Configuration.type, Configuration.TYPE_BLUETOOTH), name);
                    sp.edit().putString(name, configuration.getConfiguration()).apply();
                    addToListView(configuration);
                }
            }
        } catch (JSONException ignored) {
        }
    }

    private ArrayList<String> generateList() {
        final ArrayList<String> confs = new ArrayList<>();
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
        return confs;
    }

    private void initStageB() {
        configurationChangeTunnel.addReceiver(new Tunnel.OnTunnel<ArrayList<Configuration>>() {
            @Override
            public void onReceive(ArrayList<Configuration> configurations) {
                sendBroadcast(new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE));
            }
        });
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null)
            bluetoothAdapter = manager.getAdapter();
        masterLayout = new FrameLayout(this);
        Graphics.DragNavigation dragNavigation = new Graphics.DragNavigation(getApplicationContext(), getDrawable(R.drawable.ic_flow), 0x80333333);
        getWindow().setStatusBarColor(dragNavigation.calculateOverlayedColor(color));
        getWindow().setNavigationBarColor(color);
        masterLayout.setBackgroundColor(color);
        itemScroll = new LinearLayout(this);
        itemScroll.setOrientation(LinearLayout.VERTICAL);
        itemScroll.setGravity(Gravity.CENTER);
        itemScroll.setPadding(0, 15, 0, 0);
        scrollable = new LinearLayout(this);
        scrollable.setOrientation(LinearLayout.VERTICAL);
        scrollable.setGravity(Gravity.CENTER);
        scrollable.setPadding(15, 0, 15, 15);
        scrollable.addView(new View(this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dragNavigation.spacerSize()));
        scrollable.addView(itemScroll);
        final ScrollView sv = new ScrollView(this);
        sv.addView(scrollable);
        masterLayout.addView(sv);
        masterLayout.addView(dragNavigation);
        LinearLayout actionsView = new LinearLayout(this);
        actionsView.setOrientation(LinearLayout.VERTICAL);
        actionsView.setGravity(Gravity.CENTER);
        Button buttonNewConfig = new Button(this);
        Button buttonImportExport = new Button(this);
        buttonNewConfig.setBackground(null);
        buttonImportExport.setBackground(null);
        buttonNewConfig.setText(R.string.new_configuration_long);
        buttonImportExport.setText(R.string.text_import_export);
        actionsView.addView(buttonNewConfig);
        actionsView.addView(buttonImportExport);
        buttonNewConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createConfiguration("");
            }
        });
        buttonImportExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupIE();
            }
        });
        dragNavigation.setContent(actionsView);
        initStageC();
        setContentView(masterLayout);
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(View.FOCUS_UP);
            }
        });
        remakeTaskDescription(dragNavigation.calculateOverlayedColor(color));
    }

    private void popupIE() {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle("Import / Export");
        ad.setMessage("Would You Like To Import Or Export Configurations?");
        ad.setPositiveButton("Export", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                share(getApplicationContext(), generateExport(), "Share Exported Configurations");
            }
        });
        ad.setNegativeButton("Import", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                popupImport();
            }
        });
        ad.show();
    }

    private void popupImport() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Import From Text");
        final EditText nameText = new EditText(this);
        FrameLayout fl = new FrameLayout(this);
        nameText.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        nameText.setHint("JSON Text Goes Here");
        fl.addView(nameText);
        fl.setPadding(15, 15, 15, 15);
        alert.setView(fl);
        alert.setPositiveButton("Import Configurations", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                importConfigurations(nameText.getText().toString());
            }
        });
        alert.show();
    }

    private void initStageC() {
        itemScroll.removeAllViews();
        final ArrayList<String> confs = new ArrayList<>();
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        guideText = getText("Pull The Icon Bar Down To Access The Menu");
        itemScroll.addView(guideText);
        guideText.setVisibility(View.GONE);
        guideText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (confs.size() == 0) {
            guideText.setVisibility(View.VISIBLE);
        }
        for (int c = confs.size() - 1; c >= 0; c--) {
            addToListView(new Configuration(sp.getString(confs.get(c), "{}")));
        }
    }

    private void addToListView(Configuration c) {
        guideText.setVisibility(View.GONE);
        final ConfigurationView cv = new ConfigurationView(getApplicationContext(), c);
        cv.setOnEdit(new ConfigurationView.OnPress() {
            @Override
            public void onPress(String conf) {
                editConfiguration(conf);
            }
        });
        cv.setOnRemove(new ConfigurationView.OnPress() {
            @Override
            public void onPress(String conf) {
                removeConfiguration(getApplicationContext(), conf);
                cv.setVisibility(View.GONE);
            }
        });
        cv.setOnSetFavorite(new ConfigurationView.OnPress() {
            @Override
            public void onPress(String conf) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                sp.edit().putString(qs, conf).apply();
                Toast.makeText(getApplicationContext(), "\"" + conf + "\" is now the favored configuration.", Toast.LENGTH_LONG).show();
                Main.widgetTunnel.send("Favorite Configuration Changed.");
            }
        });
        cv.setOnRunDebug(new ConfigurationView.OnPress() {
            @Override
            public void onPress(String conf) {
                runDebug(conf);
            }
        });
        cv.setLayoutParams(new LinearLayout.LayoutParams(Device.screenX(getApplicationContext()) - scrollable.getPaddingRight() - scrollable.getPaddingLeft(), Device.screenY(getApplicationContext()) / 2));
        itemScroll.addView(cv);
    }

    private void runDebug(String conf) {
        Tunnel<String> mVerboser = new Tunnel<>();
        Action a = new Action(getApplicationContext(), conf);
        a.verboser = mVerboser;
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle("Run In Debug");
        final ScrollView sv = new ScrollView(this);
        final TextView tv = new TextView(this);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(textSize - 7);
        sv.addView(tv);
        String init = "Running Configuration '" + conf + "' In Debug";
        tv.setText(init);
        mVerboser.addReceiver(new Tunnel.OnTunnel<String>() {
            @Override
            public void onReceive(final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String newText = tv.getText().toString() + "\n\n" + s + ".";
                        tv.setText(newText);
                        sv.post(new Runnable() {

                            @Override
                            public void run() {
                                sv.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                });
            }
        });
        ad.setView(sv);
        ad.setPositiveButton("Close", null);
        ad.setNegativeButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                share(getApplicationContext(), tv.getText().toString(), "Share Debug Log");
            }
        });
        ad.show();
        Main.activateTunnel.send(a);
    }

    private void createConfiguration(String name) {
        final ArrayList<String> confs = new ArrayList<>();
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
        final Configuration configuration = new Configuration();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("New Configuration");
        final EditText nameText = new EditText(this);
        nameText.setHint("Configuration's Name");
        FrameLayout fl = new FrameLayout(this);
        nameText.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        nameText.setText(name);
        fl.addView(nameText);
        fl.setPadding(15, 15, 15, 15);
        alert.setView(fl);
        alert.setPositiveButton("Bluetooth", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (bluetoothAdapter.isEnabled()) {
                    if (nameText.getText().toString().length() != 0) {
                        if (!confs.contains(nameText.getText().toString())) {
                            SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                            configuration.setValue(Configuration.type, Configuration.TYPE_BLUETOOTH);
                            configuration.setValue(Configuration.name, nameText.getText().toString());
                            sp.edit().putString(nameText.getText().toString(), configuration.getConfiguration()).apply();
                            addConfigurationToList(getApplicationContext(), Configuration.TYPE_BLUETOOTH, nameText.getText().toString());
                            addToListView(configuration);
                            editConfiguration(nameText.getText().toString());
                        } else {
                            Toast.makeText(getApplicationContext(), "Cannot Create Configuration;\nA Configuration Named \"" + nameText.getText().toString() + "\" Is Already Available.", Toast.LENGTH_LONG).show();
                            createConfiguration("");
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot Create Configuration With An Empty Name!", Toast.LENGTH_LONG).show();
                        createConfiguration("");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Turn On Bluetooth In Order To Create A Bluetooth Configuration.", Toast.LENGTH_LONG).show();
                    createConfiguration(nameText.getText().toString());
                }
            }
        });
        alert.setNegativeButton("Internet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (nameText.getText().toString().length() != 0) {
                    if (!confs.contains(nameText.getText().toString())) {
                        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                        configuration.setValue(Configuration.type, Configuration.TYPE_INTERNET);
                        configuration.setValue(Configuration.name, nameText.getText().toString());
                        sp.edit().putString(nameText.getText().toString(), configuration.getConfiguration()).apply();
                        addConfigurationToList(getApplicationContext(), Configuration.TYPE_INTERNET, nameText.getText().toString());
                        addToListView(configuration);
                        editConfiguration(nameText.getText().toString());
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot Create Configuration;\nA Configuration Named \"" + nameText.getText().toString() + "\" Is Already Available.", Toast.LENGTH_LONG).show();
                        createConfiguration("");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot Create Configuration With An Empty Name!", Toast.LENGTH_LONG).show();
                    createConfiguration("");
                }
            }
        });
        alert.setNeutralButton("Import From Text", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (nameText.getText().toString().length() != 0) {
                    if (!confs.contains(nameText.getText().toString())) {
                        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                        configuration.setValue(Configuration.type, Configuration.TYPE_INTERNET);
                        configuration.setValue(Configuration.name, nameText.getText().toString());
                        sp.edit().putString(nameText.getText().toString(), configuration.getConfiguration()).apply();
                        addConfigurationToList(getApplicationContext(), Configuration.TYPE_INTERNET, nameText.getText().toString());
                        addToListView(configuration);
                        createFromText(nameText.getText().toString());
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot Create Configuration;\nA Configuration Named \"" + nameText.getText().toString() + "\" Is Already Available.", Toast.LENGTH_LONG).show();
                        createConfiguration("");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot Create Configuration With An Empty Name!", Toast.LENGTH_LONG).show();
                    createConfiguration("");
                }
            }
        });
        alert.show();
    }

    private void createFromText(final String name) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Import From Text");
        final EditText nameText = new EditText(this);
        FrameLayout fl = new FrameLayout(this);
        nameText.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        nameText.setHint("JSON Text Goes Here");
        fl.addView(nameText);
        fl.setPadding(15, 15, 15, 15);
        alert.setView(fl);
        alert.setPositiveButton("Load Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    JSONObject object = new JSONObject(nameText.getText().toString());
                    if (object.has(Configuration.type)) {
                        final Configuration configuration = new Configuration(nameText.getText().toString());
                        configuration.setValue(Configuration.name, name);
                        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                        sp.edit().putString(name, configuration.getConfiguration()).apply();
                        Main.configurationChangeTunnel.send(generateConfigurations());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    editConfiguration(name);
                }
            }
        });
        alert.setNegativeButton("Just Edit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editConfiguration(name);
            }
        });
        alert.show();
    }

    private void editConfiguration(final String name) {
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        final Configuration configuration = new Configuration(sp.getString(name, ""));
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Edit Configuration");
        LinearLayout all = new LinearLayout(this);
        all.setOrientation(LinearLayout.VERTICAL);
        all.setGravity(Gravity.CENTER);
        all.setPadding(0, 0, 0, 0);
        final EditText titleText = new EditText(this);
        titleText.setHint("Configuration's Title");
        titleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (s.toString().equals(titleText.getText().toString())) {
                            configuration.setValue(Configuration.title, s.toString());
                        }
                    }
                }, 100);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        all.addView(getText("Title:"));
        titleText.setText(configuration.getValue(Configuration.title, ""));
        all.addView(titleText);
        if (configuration.getValue(Configuration.type, Configuration.TYPE_BLUETOOTH) == Configuration.TYPE_BLUETOOTH) {
            RadioGroup encodingType = new RadioGroup(this);
            all.addView(getText("Select Data Type:"));
            all.addView(encodingType);
            RadioButton typeJson = new RadioButton(this);
            RadioButton typeText = new RadioButton(this);
            typeJson.setText(R.string.type_json);
            typeText.setText(R.string.type_text);
            typeJson.setGravity(Gravity.CENTER);
            typeText.setGravity(Gravity.CENTER);
            typeJson.setTextSize(textSize);
            typeText.setTextSize(textSize);
            typeJson.setTextColor(0xffffffff);
            typeText.setTextColor(0xffffffff);
            encodingType.addView(typeJson);
            encodingType.addView(typeText);
            final EditText bluetoothDataText = new EditText(this);
            bluetoothDataText.setHint("String To Send Over Bluetooth");
            bluetoothDataText.setText(configuration.getValue(Configuration.data, ""));
            bluetoothDataText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (s.toString().equals(bluetoothDataText.getText().toString())) {
                                configuration.setValue(Configuration.data, s.toString());
                            }
                        }
                    }, 100);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            int size = ((int) (Device.screenX(getApplicationContext()) / 2.5));
            String[] titles = new String[]{"Name", "Value"};
            Table bluetoothJsonTableCreate;
            try {
                bluetoothJsonTableCreate = new Table(this, Color.WHITE, size, Table.MODE_RW, new JSONArray(configuration.getValue(Configuration.bluetoothJSONParameters, "[]")), titles);
            } catch (JSONException e) {
                bluetoothJsonTableCreate = new Table(this, Color.WHITE, size, Table.MODE_RW, new JSONArray(), titles);
            }
            final Table bluetoothJsonTable = bluetoothJsonTableCreate;
            bluetoothJsonTable.setPadding(15, 15, 15, 15);
            bluetoothJsonTable.setShowRemoveButton(true, size / 4);
            bluetoothJsonTable.setOnChanged(new Table.OnChanged() {
                @Override
                public void onChanged(JSONArray nowData) {
                    configuration.setValue(Configuration.bluetoothJSONParameters, nowData.toString());
                }
            });
            typeJson.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        configuration.setValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_JSON);
                        bluetoothJsonTable.setVisibility(View.VISIBLE);
                        bluetoothDataText.setVisibility(View.GONE);
                    }
                }
            });
            typeText.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        configuration.setValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_TEXT);
                        bluetoothDataText.setVisibility(View.VISIBLE);
                        bluetoothJsonTable.setVisibility(View.GONE);
                    }
                }
            });
            if (configuration.getValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_JSON).equals(Configuration.ENCODING_BLUETOOTH_JSON)) {
                typeJson.setChecked(true);
                bluetoothJsonTable.setVisibility(View.VISIBLE);
                bluetoothDataText.setVisibility(View.GONE);
            } else {
                typeText.setChecked(true);
                bluetoothDataText.setVisibility(View.VISIBLE);
                bluetoothJsonTable.setVisibility(View.GONE);
            }
            all.addView(getText("Data:"));
            all.addView(bluetoothDataText);
            all.addView(bluetoothJsonTable);
            final TextView uuidText = getText("(Device Address)");
            RadioGroup deviceName = new RadioGroup(this);
            all.addView(getText("Select Device:"));
            all.addView(deviceName);
            all.addView(uuidText);
            ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>(Arrays.asList(bluetoothAdapter.getBondedDevices().toArray(new BluetoothDevice[bluetoothAdapter.getBondedDevices().size()])));
            for (int p = 0; p < pairedDevices.size(); p++) {
                final RadioButton device = new RadioButton(this);
                final String devName = pairedDevices.get(p).getName();
                final String devAddress = pairedDevices.get(p).getAddress();
                device.setText(devName);
                device.setTextColor(0xffffffff);
                device.setTextSize(textSize);
                device.setGravity(Gravity.CENTER);
                device.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            configuration.setValue(Configuration.deviceName, devName);
                            configuration.setValue(Configuration.deviceAddress, devAddress);
                            String addressWithName = "(" + devAddress + ")";
                            uuidText.setText(addressWithName);
                        }
                    }
                });
                deviceName.addView(device);
                if (p == 0 && configuration.getValue(Configuration.deviceName, null) == null) {
                    device.setChecked(true);
                }
                if (devName.equals(configuration.getValue(Configuration.deviceName, ""))) {
                    device.setChecked(true);
                }
            }
        } else {
            final EditText urlBaseText = new EditText(this);
            final EditText urlPathText = new EditText(this);
            final EditText portText = new EditText(this);
            RadioGroup method = new RadioGroup(this);
            RadioButton post = new RadioButton(this);
            RadioButton get = new RadioButton(this);
            post.setText(R.string.text_post);
            get.setText(R.string.text_get);
            post.setGravity(Gravity.CENTER);
            get.setGravity(Gravity.CENTER);
            method.addView(post);
            method.addView(get);
            if (configuration.getValue(Configuration.method, Configuration.METHOD_INTERNET_GET).equals(Configuration.METHOD_INTERNET_GET)) {
                get.setChecked(true);
            } else {
                post.setChecked(true);
            }
            post.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        configuration.setValue(Configuration.method, Configuration.METHOD_INTERNET_POST);
                    }
                }
            });
            get.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        configuration.setValue(Configuration.method, Configuration.METHOD_INTERNET_GET);
                    }
                }
            });
            all.addView(method);
            CheckBox displayOutput = new CheckBox(this);
            urlBaseText.setHint("e.g. http://example.com");
            urlBaseText.setText(configuration.getValue(Configuration.urlBase, ""));
            urlBaseText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    if (!s.toString().startsWith("http://") && !s.toString().startsWith("https://")) {
                        urlBaseText.setError("URL Must Begin With 'http://' Or 'https://'");
                    } else {
                        urlBaseText.setError(null);
                    }
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (s.toString().equals(urlBaseText.getText().toString())) {
                                configuration.setValue(Configuration.urlBase, s.toString());
                            }
                        }
                    }, 100);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            all.addView(getText("Base URL:"));
            all.addView(urlBaseText);
            portText.setHint("e.g. 80");
            portText.setText(String.valueOf(configuration.getValue(Configuration.port, 80)));
            portText.setInputType(InputType.TYPE_CLASS_NUMBER);
            portText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (s.toString().equals(portText.getText().toString()) && s.toString().length() > 0) {
                                configuration.setValue(Configuration.port, Integer.parseInt(s.toString()));
                            }
                        }
                    }, 100);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            all.addView(getText("Port:"));
            all.addView(portText);
            urlPathText.setHint("e.g. /index.php");
            urlPathText.setText(configuration.getValue(Configuration.urlPath, "/"));
            urlPathText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    if (!s.toString().startsWith("/")) {
                        urlBaseText.setError("Path Must Begin With '/'");
                    } else {
                        urlBaseText.setError(null);
                    }
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (s.toString().equals(urlPathText.getText().toString())) {
                                configuration.setValue(Configuration.urlPath, s.toString());
                            }
                        }
                    }, 100);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            all.addView(getText("URL Path:"));
            all.addView(urlPathText);
            displayOutput.setText(R.string.display_results);
            displayOutput.setChecked(configuration.getValue(Configuration.displayOutput, false));
            displayOutput.setTextSize(20);
            displayOutput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    configuration.setValue(Configuration.displayOutput, isChecked);
                }
            });
            all.addView(displayOutput);
            int size = ((int) (Device.screenX(getApplicationContext()) / 2.5));
            String[] titles = new String[]{"Name", "Value"};
            Table dataTable;
            try {
                dataTable = new Table(this, Color.WHITE, size, Table.MODE_RW, new JSONArray(configuration.getValue(Configuration.requestParameters, "[]")), titles);
            } catch (JSONException e) {
                dataTable = new Table(this, Color.WHITE, size, Table.MODE_RW, new JSONArray(), titles);
            }
            dataTable.setShowRemoveButton(true, size / 4);
            all.addView(getText("Request Parameters:"));
            all.addView(dataTable);
            dataTable.setOnChanged(new Table.OnChanged() {
                @Override
                public void onChanged(JSONArray nowData) {
                    configuration.setValue(Configuration.requestParameters, nowData.toString());
                }
            });
            dataTable.setPadding(15, 15, 15, 15);
        }
        all.addView(getText("Text Color:"));
        ColorPicker textColorPicker = new ColorPicker(this, configuration.getValue(Configuration.textColor, 0xff000000));
        textColorPicker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Device.screenY(getApplication()) / 4));
        textColorPicker.setOnColorChanged(new ColorPicker.OnColorChanged() {
            @Override
            public void onColorChange(int color) {
                configuration.setValue(Configuration.textColor, color);
            }
        });
        all.addView(textColorPicker);
        all.addView(getText("Background Color:"));
        ColorPicker backgroundColorPicker = new ColorPicker(this, configuration.getValue(Configuration.backgroundColor, coasterColor));
        backgroundColorPicker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Device.screenY(getApplication()) / 4));
        backgroundColorPicker.setOnColorChanged(new ColorPicker.OnColorChanged() {
            @Override
            public void onColorChange(int color) {
                configuration.setValue(Configuration.backgroundColor, color);
            }
        });
        all.addView(backgroundColorPicker);
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                sp.edit().putString(name, configuration.getConfiguration()).apply();
                Main.configurationChangeTunnel.send(generateConfigurations());
            }
        });
        ScrollView sv = new ScrollView(this);
        sv.addView(all);
        sv.setPadding(10, 10, 10, 10);
        alert.setView(sv);
        alert.show();
    }

    TextView getText(String te) {
        TextView t = new TextView(this);
        t.setText(te);
        t.setTextSize(textSize);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(Color.WHITE);
        return t;
    }

    ArrayList<Configuration> generateConfigurations() {
        ArrayList<Configuration> allConfs = new ArrayList<>();
        final ArrayList<String> confs = new ArrayList<>();
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        for (int c = 0; c < confs.size(); c++) {
            allConfs.add(new Configuration(sp.getString(confs.get(c), "{}")));
        }
        return allConfs;
    }

    static class Status {
        static final int STATUS_STARTING = 0;
        static final int STATUS_IN_PROGRESS = 1;
        static final int STATUS_SUCCEDED = 2;
        static final int STATUS_FAILED = 3;
        static final int STATUS_IDLE = -1;
        int status;
        Configuration c = null;

        public Status(int whatsOn) {
            status = whatsOn;
        }

        public Status(int whatsOn, Configuration c) {
            status = whatsOn;
            this.c = c;
        }
    }

    static class Action {
        Context c;
        String config;
        Tunnel<String> verboser;

        public Action(Context c, String configurationName) {
            this.c = c;
            config = configurationName;
        }
    }

    static class Configuration {
        static final int TYPE_BLUETOOTH = 1;
        static final int TYPE_INTERNET = 0;
        static final String METHOD_INTERNET_POST = "post";
        static final String METHOD_INTERNET_GET = "get";
        static final String ENCODING_BLUETOOTH_TEXT = "text";
        static final String ENCODING_BLUETOOTH_JSON = "json";
        static final String urlBase = "internet_address_base";
        static final String urlPath = "internet_address_path";
        static final String type = "type";
        static final String title = "title";
        static final String port = "internet_port";
        static final String data = "bluetooth_string";
        static final String deviceAddress = "bluetooth_uuid";
        static final String backgroundColor = "back_color";
        static final String textColor = "text_color";
        static final String method = "internet_method";
        static final String bluetoothEncoding = "bluetooth_encoding";
        static final String bluetoothJSONParameters = "bluetooth_json_parameters";
        static final String requestParameters = "internet_parameters";
        static final String displayOutput = "internet_display_output";
        static final String count = "uses";
        static final String name = "name";
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

    static class ConfigurationView extends LinearLayout {

        Configuration configuration;
        LinearLayout bottomButtons, divider, left, right;
        OnPress onEdit = null;
        OnPress onRemove = null;
        OnPress onSetFavorite = null;
        OnPress onRunDebug = null;

        public ConfigurationView(Context c) {
            super(c);
        }

        public ConfigurationView(Context cont, Configuration c) {
            super(cont);
            configuration = c;
            initStageA();
            Main.configurationChangeTunnel.addReceiver(new Tunnel.OnTunnel<ArrayList<Configuration>>() {
                @Override
                public void onReceive(ArrayList<Configuration> configurations) {
                    Configuration oldConfiguration = configuration;
                    for (int c = 0; c < configurations.size(); c++) {
                        if (configurations.get(c).getValue(Configuration.name, null).equals(configuration.getValue(Configuration.name, null))) {
                            configuration = configurations.get(c);
                            break;
                        }
                    }
                    if (!oldConfiguration.getConfiguration().equals(configuration.getConfiguration())) {
                        initStageA();
                    }
                }
            });
        }

        @Override
        public void setLayoutParams(ViewGroup.LayoutParams params) {
            super.setLayoutParams(params);
            reside(params);
            redivide(params);
        }

        private void reside(ViewGroup.LayoutParams params) {
            if (left != null && right != null && params != null) {
                LinearLayout.LayoutParams lp = new LayoutParams(params.width / 2, ViewGroup.LayoutParams.MATCH_PARENT);
                left.setLayoutParams(lp);
                right.setLayoutParams(lp);
                if (configuration.getValue(Configuration.type, Configuration.TYPE_BLUETOOTH) == Configuration.TYPE_INTERNET) {
                    initStageEInternet();
                } else {
                    initStageEBluetooth();
                }
            }
        }

        private void redivide(ViewGroup.LayoutParams params) {
            if (divider != null && bottomButtons != null && bottomButtons.getLayoutParams() != null && params != null) {
                divider.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getLayoutParams().height - bottomButtons.getLayoutParams().height - getPaddingTop() - getPaddingBottom()));
            }
        }

        private void initStageA() {
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setBackground(generateCoaster(configuration.getValue(Configuration.backgroundColor, Main.coasterColor)));
            setPadding(10, 10, 10, 10);
            initStageB();
        }

        private void initStageB() {
            removeAllViews();
            initStageC();
        }

        private void initStageC() {
            divider = new LinearLayout(getContext());
            left = new LinearLayout(getContext());
            right = new LinearLayout(getContext());
            divider.setOrientation(LinearLayout.HORIZONTAL);
            left.setOrientation(LinearLayout.VERTICAL);
            right.setOrientation(LinearLayout.VERTICAL);
            divider.setGravity(Gravity.CENTER);
            left.setGravity(Gravity.CENTER);
            right.setGravity(Gravity.CENTER);
            TextView title = new TextView(getContext());
            title.setGravity(Gravity.CENTER);
            title.setTextSize(25);
            String nameText = configuration.getValue(Configuration.title, "No Title") + " (" + configuration.getValue(Configuration.name, "No Name!") + ")";
            title.setText(nameText);
            title.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            left.addView(title);
            bottomButtons = new LinearLayout(getContext());
            bottomButtons.setGravity(Gravity.CENTER);
            bottomButtons.setOrientation(LinearLayout.HORIZONTAL);
            ImageButton share, delete, setqs, edit, rundebug, flow;
            share = new ImageButton(getContext());
            edit = new ImageButton(getContext());
            flow = new ImageButton(getContext());
            delete = new ImageButton(getContext());
            setqs = new ImageButton(getContext());
            rundebug = new ImageButton(getContext());
            share.setScaleType(ImageView.ScaleType.CENTER_CROP);
            edit.setScaleType(ImageView.ScaleType.CENTER_CROP);
            flow.setScaleType(ImageView.ScaleType.CENTER_CROP);
            delete.setScaleType(ImageView.ScaleType.CENTER_CROP);
            setqs.setScaleType(ImageView.ScaleType.CENTER_CROP);
            rundebug.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int size = (int) (Device.screenX(getContext()) *0.15);
            LinearLayout.LayoutParams buttons = new LayoutParams(size, size);
            share.setBackground(null);
            edit.setBackground(null);
            flow.setBackground(null);
            delete.setBackground(null);
            setqs.setBackground(null);
            rundebug.setBackground(null);
            share.setImageDrawable(getContext().getDrawable(R.drawable.ic_share));
            edit.setImageDrawable(getContext().getDrawable(R.drawable.ic_create));
            flow.setImageDrawable(getContext().getDrawable(R.drawable.ic_run));
            delete.setImageDrawable(getContext().getDrawable(R.drawable.ic_delete));
            setqs.setImageDrawable(getContext().getDrawable(R.drawable.ic_set_favorite));
            rundebug.setImageDrawable(getContext().getDrawable(R.drawable.ic_bug));
            share.setLayoutParams(buttons);
            edit.setLayoutParams(buttons);
            flow.setLayoutParams(buttons);
            delete.setLayoutParams(buttons);
            setqs.setLayoutParams(buttons);
            rundebug.setLayoutParams(buttons);
            bottomButtons.addView(edit);
            bottomButtons.addView(delete);
            bottomButtons.addView(share);
            bottomButtons.addView(setqs);
            bottomButtons.addView(rundebug);
            bottomButtons.addView(flow);
            bottomButtons.setPadding(5, 5, 5, 5);
            bottomButtons.setBackground(generateCoaster(configuration.getValue(Configuration.textColor, 0xff000000)));
            share.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Main.share(getContext(), configuration.getConfiguration(), "Share Configuration As JSON");
                }
            });
            flow.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Main.activateTunnel.send(new Action(getContext(), configuration.getValue(Configuration.name, null)));
                }
            });
            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onEdit != null)
                        onEdit.onPress(configuration.getValue(Configuration.name, null));
                }
            });
            setqs.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onSetFavorite != null)
                        onSetFavorite.onPress(configuration.getValue(Configuration.name, null));
                }
            });
            delete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onRemove != null)
                        onRemove.onPress(configuration.getValue(Configuration.name, null));
                }
            });
            rundebug.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onRunDebug != null)
                        onRunDebug.onPress(configuration.getValue(Configuration.name, null));
                }
            });
            bottomButtons.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size + bottomButtons.getPaddingTop() + bottomButtons.getPaddingBottom()));
            left.setPadding(20, 0, 10, 0);
            right.setPadding(10, 0, 20, 0);
            divider.addView(left);
            divider.addView(right);
            addView(divider);
            addView(bottomButtons);
            reside(getLayoutParams());
            redivide(getLayoutParams());
            if (configuration.getValue(Configuration.type, Configuration.TYPE_BLUETOOTH) == Configuration.TYPE_BLUETOOTH) {
                initStageDBluetooth();
            } else {
                initStageDInternet();
            }
        }

        public void setOnEdit(OnPress onEdit) {
            this.onEdit = onEdit;
        }

        public void setOnRemove(OnPress onRemove) {
            this.onRemove = onRemove;
        }

        public void setOnSetFavorite(OnPress onSet) {
            this.onSetFavorite = onSet;
        }

        public void setOnRunDebug(OnPress onSet) {
            this.onRunDebug = onSet;
        }

        private void initStageDBluetooth() {
            TextView deviceName = new TextView(getContext());
            TextView deviceMac = new TextView(getContext());
            TextView dataType = new TextView(getContext());
            deviceName.setGravity(Gravity.CENTER);
            deviceName.setTextSize(20);
            deviceMac.setGravity(Gravity.CENTER);
            deviceMac.setTextSize(20);
            dataType.setGravity(Gravity.CENTER);
            dataType.setTextSize(20);
            deviceName.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            deviceMac.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            dataType.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            String deviceNameText = "Device Name: " + configuration.getValue(Configuration.deviceName, "No Device");
            deviceName.setText(deviceNameText);
            String deviceMacText = "Device MAC: " + configuration.getValue(Configuration.deviceAddress, "No Mac");
            deviceMac.setText(deviceMacText);
            String dataTypeText = "Data-Type: " + configuration.getValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_JSON).substring(0, 1).toUpperCase() + configuration.getValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_JSON).substring(1);
            dataType.setText(dataTypeText);
            left.addView(deviceName);
            left.addView(dataType);
            initStageEBluetooth();
        }

        private void initStageDInternet() {
            TextView url = new TextView(getContext());
            url.setGravity(Gravity.CENTER);
            url.setTextSize(20);
            url.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            String urlText = "Server: " + configuration.getValue(Configuration.urlBase, "No Base URL").replaceAll("https://", "").replaceAll("http://", "");
            url.setText(urlText);
            TextView port = new TextView(getContext());
            port.setGravity(Gravity.CENTER);
            port.setTextSize(20);
            port.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            String portText = "Port: " + configuration.getValue(Configuration.port, 80);
            port.setText(portText);
            TextView method = new TextView(getContext());
            method.setGravity(Gravity.CENTER);
            method.setTextSize(20);
            method.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            String methodText = "Method: " + configuration.getValue(Configuration.method, Configuration.METHOD_INTERNET_GET).substring(0, 1).toUpperCase() + configuration.getValue(Configuration.method, Configuration.METHOD_INTERNET_GET).substring(1);
            method.setText(methodText);
            left.addView(url);
            left.addView(port);
            left.addView(method);
            initStageEInternet();
        }

        private void initStageEInternet() {
            right.removeAllViews();
            int size = right.getLayoutParams().width / 2;
            String[] titles = new String[]{"Name", "Value"};
            Table dataTable;
            try {
                dataTable = new Table(getContext(), configuration.getValue(Configuration.textColor, Color.BLACK), size, Table.MODE_RO, new JSONArray(configuration.getValue(Configuration.requestParameters, "[]")), titles);
            } catch (JSONException e) {
                dataTable = new Table(getContext(), configuration.getValue(Configuration.textColor, Color.BLACK), size, Table.MODE_RO, new JSONArray(), titles);
            }
            right.addView(dataTable);
            dataTable.setPadding(5, 5, 5, 5);
        }

        private void initStageEBluetooth() {
            right.removeAllViews();
            if (configuration.getValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_JSON).equals(Configuration.ENCODING_BLUETOOTH_JSON)) {
                int size = right.getLayoutParams().width / 2;
                String[] titles = new String[]{"Name", "Value"};
                Table dataTable;
                try {
                    dataTable = new Table(getContext(), configuration.getValue(Configuration.textColor, Color.BLACK), size, Table.MODE_RO, new JSONArray(configuration.getValue(Configuration.bluetoothJSONParameters, "[]")), titles);
                } catch (JSONException e) {
                    dataTable = new Table(getContext(), configuration.getValue(Configuration.textColor, Color.BLACK), size, Table.MODE_RO, new JSONArray(), titles);
                }
                right.addView(dataTable);
                dataTable.setPadding(5, 5, 5, 5);
            } else {
                TextView dataTitle = new TextView(getContext());
                dataTitle.setGravity(Gravity.CENTER);
                dataTitle.setTextSize(25);
                dataTitle.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
                TextView data = new TextView(getContext());
                data.setGravity(Gravity.CENTER);
                data.setTextSize(25);
                data.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
                String dataTitleText = "Data:";
                dataTitle.setText(dataTitleText);
                data.setText(configuration.getValue(Configuration.data, "No Data"));
                data.setTextIsSelectable(true);
                right.addView(dataTitle);
                right.addView(data);
            }
        }

        private Drawable generateCoaster(int color) {
            GradientDrawable gd = (GradientDrawable) getContext().getDrawable(R.drawable.rounded_rect);
            if (gd != null) {
                gd.setColor(color);
            }
            return gd;
        }

        public interface OnPress {
            void onPress(String conf);
        }
    }

    static class ColorPicker extends LinearLayout {
        private int defaultColor = 0xFFFFFFFF, currentColor = defaultColor;
        private SeekBar redSeekBar, greenSeekBar, blueSeekBar;
        private OnColorChanged onColor = null;

        public ColorPicker(Context context) {
            super(context);
            addViews();
        }

        public ColorPicker(Context context, int defaultColor) {
            super(context);
            this.defaultColor = defaultColor;
            currentColor = defaultColor;
            addViews();
        }

        private void addViews() {
            SeekBar.OnSeekBarChangeListener onChange = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    currentColor = Color.rgb(redSeekBar.getProgress(), greenSeekBar.getProgress(), blueSeekBar.getProgress());
                    drawThumbs(currentColor);
                    setCoasterColor(currentColor);
                    if (onColor != null) onColor.onColorChange(currentColor);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            };
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setLayoutDirection(LAYOUT_DIRECTION_LTR);
            setPadding(15, 15, 15, 15);
            GradientDrawable redDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF000000, 0xFFFF0000});
            GradientDrawable greenDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF000000, 0xFF00FF00});
            GradientDrawable blueDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF000000, 0xFF0000FF});
            redDrawable.setCornerRadius(8);
            greenDrawable.setCornerRadius(8);
            blueDrawable.setCornerRadius(8);
            redSeekBar = new SeekBar(getContext());
            greenSeekBar = new SeekBar(getContext());
            blueSeekBar = new SeekBar(getContext());
            redSeekBar.setPadding(10, 10, 10, 10);
            greenSeekBar.setPadding(10, 10, 10, 10);
            blueSeekBar.setPadding(10, 10, 10, 10);
            redSeekBar.setProgressDrawable(redDrawable);
            greenSeekBar.setProgressDrawable(greenDrawable);
            blueSeekBar.setProgressDrawable(blueDrawable);
            redSeekBar.setMax(255);
            greenSeekBar.setMax(255);
            blueSeekBar.setMax(255);
            addView(redSeekBar);
            addView(greenSeekBar);
            addView(blueSeekBar);
            redSeekBar.setProgress(Color.red(defaultColor));
            greenSeekBar.setProgress(Color.green(defaultColor));
            blueSeekBar.setProgress(Color.blue(defaultColor));
            redSeekBar.setOnSeekBarChangeListener(onChange);
            greenSeekBar.setOnSeekBarChangeListener(onChange);
            blueSeekBar.setOnSeekBarChangeListener(onChange);
            drawThumbs(defaultColor);
            setCoasterColor(defaultColor);
        }

        public void setOnColorChanged(OnColorChanged onc) {
            onColor = onc;
        }

        private void drawThumbs(int color) {
            int redAmount = Color.red(color);
            int greenAmount = Color.green(color);
            int blueAmount = Color.blue(color);
            int xy = ((redSeekBar.getLayoutParams().height - redSeekBar.getPaddingTop() - redSeekBar.getPaddingBottom()) + (greenSeekBar.getLayoutParams().height - greenSeekBar.getPaddingTop() - greenSeekBar.getPaddingBottom()) + (blueSeekBar.getLayoutParams().height - blueSeekBar.getPaddingTop() - blueSeekBar.getPaddingBottom())) / 3;
            redSeekBar.setThumb(getRoundedRect(Color.rgb(redAmount, 0, 0), xy));
            greenSeekBar.setThumb(getRoundedRect(Color.rgb(0, greenAmount, 0), xy));
            blueSeekBar.setThumb(getRoundedRect(Color.rgb(0, 0, blueAmount), xy));
        }

        @Override
        public void setLayoutParams(ViewGroup.LayoutParams l) {
            LinearLayout.LayoutParams l2;
            if (l instanceof LinearLayout.LayoutParams) {
                l2 = ((LinearLayout.LayoutParams) l);
                l2.setMargins(0, 10, 0, 10);
                super.setLayoutParams(l2);
            } else {
                super.setLayoutParams(l);
            }
            redSeekBar.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, l.height / 4));
            greenSeekBar.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, l.height / 4));
            blueSeekBar.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, l.height / 4));
            drawThumbs(currentColor);
        }

        private void setCoasterColor(int color) {
            float corner = 16;
            float[] corners = new float[]{corner, corner, corner, corner, corner, corner, corner, corner};
            RoundRectShape shape = new RoundRectShape(corners, new RectF(), corners);
            ShapeDrawable coaster = new ShapeDrawable(shape);
            coaster.getPaint().setColor(color);
            setBackground(coaster);
        }

        private LayerDrawable getRoundedRect(int color, int size) {
            float corner = 16;
            float[] corners = new float[]{corner, corner, corner, corner, corner, corner, corner, corner};
            RoundRectShape shape = new RoundRectShape(corners, new RectF(), corners);
            RoundRectShape shape2 = new RoundRectShape(corners, new RectF(8, 8, 8, 8), corners);
            ShapeDrawable rectBack = new ShapeDrawable(shape2);
            rectBack.setIntrinsicHeight(size);
            rectBack.setIntrinsicWidth(size);
            rectBack.getPaint().setColor(Color.WHITE);
            ShapeDrawable rect = new ShapeDrawable(shape);
            rect.setIntrinsicHeight((size));
            rect.setIntrinsicWidth((size));
            rect.getPaint().setColor(color);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{rect, rectBack});
            return ld;
        }

        public interface OnColorChanged {
            void onColorChange(int color);
        }
    }

    static class Table extends LinearLayout {
        static final int MODE_RW = 0;
        static final int MODE_RO = 1;
        static final int MODE_VO = 2;
        JSONArray currentData;
        String[] titles;
        int mode, size, textcolor;
        OnChanged onChanged;
        int removeButtonSize;
        boolean removeButton = false;

        public Table(Context context) {
            super(context);
        }

        public Table(Context context, int textcolor, int size, int mode, JSONArray currentData, String[] titles) {
            super(context);
            this.currentData = currentData;
            this.mode = mode;
            this.size = size;
            this.textcolor = textcolor;
            this.titles = titles;
            init();
        }

        public void setOnChanged(OnChanged onChanged) {
            this.onChanged = onChanged;
        }

        public void setShowRemoveButton(boolean isShown, int buttonSize) {
            removeButton = isShown;
            removeButtonSize = buttonSize;
            init();
        }

        private void init() {
            removeAllViews();
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            TableRow title = new TableRow(getContext(), textcolor, size, MODE_VO, titles, titles);
            addView(title);
            ScrollView rowScroll = new ScrollView(getContext());
            final LinearLayout rows = new LinearLayout(getContext());
            rows.setOrientation(LinearLayout.VERTICAL);
            rows.setGravity(Gravity.CENTER);
            rowScroll.addView(rows);
            addView(rowScroll);
            addListElements(rows);
            ImageButton addNew = new ImageButton(getContext());
            addNew.setImageDrawable(getContext().getDrawable(R.drawable.ic_add));
            addNew.setBackground(null);
            addNew.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentData.put(new JSONObject());
                    addListElements(rows);
                    if (onChanged != null) onChanged.onChanged(currentData);
                }
            });
            if (mode == MODE_RW)
                addView(addNew);
        }

        private void addListElements(final LinearLayout list) {
            list.removeAllViews();
            for (int t = 0; t < currentData.length(); t++) {
                String[] current = new String[titles.length];
                try {
                    JSONObject obj = currentData.getJSONObject(t);
                    for (int a = 0; a < titles.length; a++) {
                        if (obj.has(titles[a].toLowerCase())) {
                            current[a] = obj.getString(titles[a].toLowerCase());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final int finalT = t;
                final TableRow row = new TableRow(getContext(), textcolor, size, mode, current, titles);
                if (removeButton) {
                    row.showRemoveButton(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            currentData.remove(finalT);
                            addListElements(list);
                            if (onChanged != null) onChanged.onChanged(currentData);
                        }
                    }, removeButtonSize);
                }
                list.addView(row);
                row.setOnChanged(new TableRow.OnChanged() {
                    @Override
                    public void onChanged(String name, String value) {
                        try {
                            JSONObject buildNew = new JSONObject();
                            for (int b = 0; b < titles.length; b++) {
                                buildNew.put(titles[b].toLowerCase(), row.getCurrentValues()[b]);
                            }
                            currentData.put(finalT, buildNew);
                            if (onChanged != null) onChanged.onChanged(currentData);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        interface OnChanged {
            void onChanged(JSONArray nowData);
        }

        static class TableRow extends LinearLayout {
            int mode;
            int size;
            int textcolor;
            String[] currentValues;
            String[] hints;
            OnChanged onChanged;
            ImageButton remove;

            public TableRow(Context context) {
                super(context);
            }

            public TableRow(Context context, int textcolor, int size, int mode, String[] values, String[] hints) {
                super(context);
                this.mode = mode;
                currentValues = values;
                this.hints = hints;
                this.size = size;
                this.textcolor = textcolor;
                init();
            }

            public void setOnChanged(OnChanged onChanged) {
                this.onChanged = onChanged;
            }

            public String[] getCurrentValues() {
                return currentValues;
            }

            public void showRemoveButton(View.OnClickListener todo, int s) {
                remove.setLayoutParams(new LayoutParams(s, ViewGroup.LayoutParams.MATCH_PARENT));
                int c = 1;
                getChildAt(c).setLayoutParams(new LayoutParams(getChildAt(c).getLayoutParams().width - remove.getLayoutParams().width, getChildAt(c).getLayoutParams().height));
                remove.setVisibility(View.VISIBLE);
                remove.setOnClickListener(todo);
            }

            private void init() {
                setOrientation(HORIZONTAL);
                setGravity(Gravity.CENTER);
                remove = new ImageButton(getContext());
                remove.setImageDrawable(getContext().getDrawable(R.drawable.ic_delete));
                remove.setBackground(null);
                addView(remove);
                remove.setVisibility(View.GONE);
                for (int i = 0; i < currentValues.length; i++) {
                    final TextView ctv;
                    if (mode == Table.MODE_VO) {
                        ctv = new TextView(getContext());
                    } else if (mode == Table.MODE_RO) {
                        ctv = new EditText(getContext());
                        ctv.setInputType(InputType.TYPE_NULL);
                        ctv.setTextIsSelectable(true);
                    } else {
                        ctv = new EditText(getContext());
                    }
                    ctv.setTextColor(textcolor);
                    if (hints.length == currentValues.length) {
                        ctv.setHint(hints[i]);
                    }
                    ctv.setTextSize(20);
                    ctv.setText(currentValues[i]);
                    final int finalI = i;
                    ctv.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(final CharSequence s, int start, int before, int count) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (s.toString().equals(ctv.getText().toString())) {
                                        currentValues[finalI] = s.toString();
                                        if (onChanged != null && hints.length == currentValues.length) {
                                            onChanged.onChanged(hints[finalI], currentValues[finalI]);
                                        }
                                    }
                                }
                            }, 100);
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });
                    ctv.setLayoutParams(new LayoutParams(size, ViewGroup.LayoutParams.WRAP_CONTENT));
                    ctv.setGravity(Gravity.CENTER);
                    addView(ctv);
                }
            }

            interface OnChanged {
                void onChanged(String name, String value);
            }
        }
    }

    static class BluetoothSession extends AsyncTask<String, Status, Status> {
        private Action action;
        private OnSessionEnd onSessionEnd;

        public BluetoothSession(Action action, OnSessionEnd onSessionEnd) {
            this.action = action;
            this.onSessionEnd = onSessionEnd;
            verbose("AsyncTask Initialized");
        }

        @Override
        protected Main.Status doInBackground(String... configurations) {
            Main.Status returnStatus = new Main.Status(Main.Status.STATUS_STARTING);
            verbose("Starting Configuration");
            BluetoothAdapter blueAdapter;
            BluetoothManager manager = (BluetoothManager) action.c.getSystemService(Context.BLUETOOTH_SERVICE);
            SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
            Configuration configuration = new Configuration(sp.getString(action.config, "{}"));
            String sending;
            if (configuration.getValue(Configuration.bluetoothEncoding, Configuration.ENCODING_BLUETOOTH_JSON).equals(Configuration.ENCODING_BLUETOOTH_JSON)) {
                verbose("Generating Text From JSON");
                try {
                    JSONArray jsonConfig = new JSONArray(configuration.getValue(Configuration.bluetoothJSONParameters, "[]"));
                    JSONObject object = new JSONObject();
                    for (int a = 0; a < jsonConfig.length(); a++) {
                        JSONObject currentObject = jsonConfig.getJSONObject(a);
                        if (currentObject.has("name") && currentObject.has("value")) {
                            object.put(currentObject.getString("name"), currentObject.getString("value"));
                        }
                    }
                    sending = object.toString();
                    verbose("Generated Text From JSON");
                } catch (JSONException e) {
                    e.printStackTrace();
                    verbose("Failed Generating Text From JSON. Using Empty");
                    sending = "";
                }
            } else {
                verbose("Getting Saved Data");
                sending = configuration.getValue(Configuration.data, "");
            }
            verbose("Text To Send: " + sending);
            if (manager != null) {
                verbose("Bluetooth Permission Granted");
                blueAdapter = manager.getAdapter();
                if (blueAdapter.isEnabled()) {
                    verbose("Bluetooth Is Enabled");
                    blueAdapter.cancelDiscovery();
                    if (!configuration.getValue(Configuration.deviceName, "").equals("") && !configuration.getValue(Configuration.deviceAddress, "").equals("")) {
                        verbose("Device Name Found");
                        final BluetoothDevice device = blueAdapter.getRemoteDevice(configuration.getValue(Configuration.deviceAddress, ""));
                        UUID uuid = device.getUuids()[0].getUuid();
                        verbose("Device UUID: " + uuid.toString());
                        try {
                            verbose("Trying To Create Socket");
                            final BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                            returnStatus.status = Main.Status.STATUS_IN_PROGRESS;
                            verbose("Socket Created");
                            try {
                                verbose("Connecting To Socket");
                                socket.connect();
                                while (!socket.isConnected())
                                    returnStatus.status = Main.Status.STATUS_IN_PROGRESS;
                                verbose("Connected To Socket");
                                if (socket.isConnected()) {
                                    verbose("Sending Data Via Socket");
                                    socket.getOutputStream().write(sending.getBytes());
                                    verbose("Data Sent");
                                    try {
                                        verbose("Trying To Close Output Stream And Socket");
                                        socket.getOutputStream().flush();
                                        socket.getOutputStream().close();
                                        socket.close();
                                        verbose("Socket And Output Stream Closed");
                                    } catch (IOException ignored) {
                                        verbose("Failed Closing Socket");
                                    }
                                    returnStatus.status = Main.Status.STATUS_SUCCEDED;
                                    verbose("Operation Successful");
                                } else {
                                    returnStatus.status = Main.Status.STATUS_FAILED;
                                    verbose("Could Not Connect Socket, Operation Failed");
                                }
                            } catch (IOException e) {
                                verbose("Operation Could Not Be Completed, Socket Closed");
                                returnStatus.status = Main.Status.STATUS_FAILED;
                            }
                        } catch (IOException e) {
                            verbose("Operation Failed, Could Not Create Socket");
                            returnStatus.status = Main.Status.STATUS_FAILED;
                        }
                    }
                } else {
                    verbose("Operation Could Not Begin, Bluetooth Is Off");
                    returnStatus.status = Main.Status.STATUS_FAILED;
                }
            } else {
                verbose("Bluetooth Permission Not Granted");
            }
            verbose("Done");
            return returnStatus;
        }

        @Override
        protected void onPostExecute(Main.Status status) {
            super.onPostExecute(status);
            if (onSessionEnd != null) onSessionEnd.onSessionEnd(status);
        }

        private void verbose(String s) {
            if (action.verboser != null) action.verboser.send(s);
        }

        public interface OnSessionEnd {
            void onSessionEnd(Main.Status result);
        }
    }
}