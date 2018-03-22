package nadav.tasher.airflow;

import android.Manifest;
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
import android.content.pm.PackageManager;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import java.util.UUID;

import nadav.tasher.lightool.Device;
import nadav.tasher.lightool.Graphics;
import nadav.tasher.lightool.Net;
import nadav.tasher.lightool.Tunnel;

public class Main extends Activity {
    static final String qs = "qs";
    static final String shortcut = "short";
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
            }
        });
        internetTunnel.addReceiver(new Tunnel.OnTunnel<Action>() {
            @Override
            public void onReceive(final Action action) {
                SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
                final Configuration configuration = new Configuration(sp.getString(action.config, "{}"));
                Net.Request.RequestParameter[] parms;
                try {
                    JSONArray requestParms = new JSONArray(configuration.getValue(Configuration.requestParameters, "[]"));
                    parms = new Net.Request.RequestParameter[requestParms.length()];
                    for (int rp = 0; rp < requestParms.length(); rp++) {
                        JSONObject jo = new JSONObject(requestParms.getString(rp));
                        parms[rp] = new Net.Request.RequestParameter(jo.getString("name"), jo.getString("value"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    parms = new Net.Request.RequestParameter[0];
                }
                if (configuration.getValue(Configuration.method, Configuration.METHOD_INTERNET_GET).equals(Configuration.METHOD_INTERNET_GET)) {
                    new Net.Request.Get(configuration.getValue(Configuration.urlBase, "") + ":" + configuration.getValue(Configuration.port, 80) + configuration.getValue(Configuration.urlPath, "/"), parms, new Net.Request.OnRequest() {
                        @Override
                        public void onRequest(String s) {
                            if (s == null) {
                                Toast.makeText(action.c, "Failed.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(action.c, "Sent.", Toast.LENGTH_LONG).show();
                            }
                            if(configuration.getValue(Configuration.displayOutput,false)){
                                Toast.makeText(action.c, s, Toast.LENGTH_LONG).show();
                            }
                        }
                    }).execute();
                } else {
                    new Net.Request.Post(configuration.getValue(Configuration.urlBase, "") + ":" + configuration.getValue(Configuration.port, 80) + configuration.getValue(Configuration.urlPath, "/"), parms, new Net.Request.OnRequest() {
                        @Override
                        public void onRequest(String s) {
                            if (s == null) {
                                Toast.makeText(action.c, "Failed.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(action.c, "Sent.", Toast.LENGTH_LONG).show();
                            }
                            if(configuration.getValue(Configuration.displayOutput,false)){
                                Toast.makeText(action.c, s, Toast.LENGTH_LONG).show();
                            }
                        }
                    }).execute();
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
    private LinearLayout scrollable;
    private BluetoothManager manager;
    private BluetoothAdapter bluetoothAdapter;

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
        Graphics.DragNavigation dragNavigation = new Graphics.DragNavigation(getApplicationContext(), getDrawable(R.drawable.ic_launcher), 0x80333333);
        getWindow().setStatusBarColor(dragNavigation.calculateOverlayedColor(color));
        getWindow().setNavigationBarColor(color);
        masterLayout.setBackgroundColor(color);
        scrollable = new LinearLayout(this);
        scrollable.setOrientation(LinearLayout.VERTICAL);
        scrollable.setGravity(Gravity.CENTER);
        scrollable.setPadding(15, 15, 15, 15);
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
        buttonNewConfig.setBackground(null);
        buttonChooseQS.setBackground(null);
        buttonChooseLP.setBackground(null);
        buttonImportExport.setBackground(null);
        buttonNewConfig.setText("New Configuration");
        buttonChooseQS.setText("Choose QuickSettings");
        buttonChooseLP.setText("Choose Shortcut");
        buttonImportExport.setText("Import/Export");
        actionsView.addView(buttonNewConfig);
        actionsView.addView(buttonChooseQS);
        actionsView.addView(buttonChooseLP);
        actionsView.addView(buttonImportExport);
        buttonNewConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createConfiguration("");
            }
        });
        dragNavigation.setContent(actionsView);
        final ArrayList<String> confs = new ArrayList<>();
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        for (int c = 0; c < confs.size(); c++) {
            addToListView(new Configuration(sp.getString(confs.get(c), "{}")));
        }
        setContentView(masterLayout);
        remakeTaskDescription();
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

    private void addToListView(Configuration c) {
        ConfigurationView cv = new ConfigurationView(getApplicationContext(), c);
        cv.setOnEdit(new ConfigurationView.OnEdit() {
            @Override
            public void onEdit(String conf) {
                editConfiguration(conf);
            }
        });
        cv.setLayoutParams(new LinearLayout.LayoutParams(Device.screenX(getApplicationContext()) - scrollable.getPaddingRight() - scrollable.getPaddingLeft(), Device.screenY(getApplicationContext()) / 2));
        scrollable.addView(cv, 1);
    }

    private void createConfiguration(String name) {
        final ArrayList<String> confs = new ArrayList<>();
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_BLUETOOTH));
        confs.addAll(Main.getConfigurationsFromList(getApplicationContext(), Main.Configuration.TYPE_INTERNET));
        final Configuration configuration = new Configuration();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("New Configuration");
        final EditText nameText = new EditText(this);
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
            all.addView(getText("Data:"));
            all.addView(bluetoothDataText);
            final TextView uuidText = getText("(Device Address)");
            RadioGroup deviceName = new RadioGroup(this);
            all.addView(getText("Select Device:"));
            all.addView(deviceName);
            all.addView(uuidText);
            ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>(Arrays.asList(bluetoothAdapter.getBondedDevices().toArray(new BluetoothDevice[bluetoothAdapter.getBondedDevices().size()])));
            for (int p = 0; p < pairedDevices.size(); p++) {
                final RadioButton device = new RadioButton(getApplicationContext());
                final String devName = pairedDevices.get(p).getName();
                final String devAddress = pairedDevices.get(p).getAddress();
                device.setText(devName);
                device.setTextColor(Color.WHITE);
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
                    device.toggle();
                }
                if (devName.equals(configuration.getValue(Configuration.deviceName, ""))) {
                    device.toggle();
                }
            }
        } else {
            final EditText urlBaseText = new EditText(this);
            final EditText urlPathText = new EditText(this);
            final EditText portText = new EditText(this);
            EditText fileText = new EditText(this);
            CheckBox displayOutput = new CheckBox(this);
            urlBaseText.setHint("e.g. http://example.com");
            urlBaseText.setText(configuration.getValue(Configuration.urlBase, ""));
            urlBaseText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    if(!s.toString().startsWith("http://")&&!s.toString().startsWith("https://")){
                        urlBaseText.setError("URL Must Begin With 'http://' Or 'https://'");
                    }else{
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
            portText.setText(configuration.getValue(Configuration.port, ""));
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
                            if (s.toString().equals(portText.getText().toString())&&s.toString().length()>0) {
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
                    if(!s.toString().startsWith("/")){
                        urlBaseText.setError("Path Must Begin With '/'");
                    }else{
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
        }
        all.addView(getText("Text Color:"));
        ConfigurationView.ColorPicker textColorPicker = new ConfigurationView.ColorPicker(this, configuration.getValue(Configuration.textColor, 0xff000000));
        textColorPicker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Device.screenY(getApplication()) / 4));
        textColorPicker.setOnColorChanged(new ConfigurationView.ColorPicker.OnColorChanged() {
            @Override
            public void onColorChange(int color) {
                configuration.setValue(Configuration.textColor, color);
            }
        });
        all.addView(textColorPicker);
        all.addView(getText("Background Color:"));
        ConfigurationView.ColorPicker backgroundColorPicker = new ConfigurationView.ColorPicker(this, configuration.getValue(Configuration.backgroundColor, coasterColor));
        backgroundColorPicker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Device.screenY(getApplication()) / 4));
        backgroundColorPicker.setOnColorChanged(new ConfigurationView.ColorPicker.OnColorChanged() {
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
        static final String urlBase = "internet_address_base";
        static final String urlPath = "internet_address_path";
        static final String type = "type";
        static final String title = "title";
        static final String port = "internet_port";
        static final String data = "bluetooth_string";
        static final String deviceAddress = "bluetooth_uuid";
        static final String backgroundColor = "back_color";
        static final String textColor = "text_color";
        static final String file = "internet_file";
        static final String method = "internet_method";
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
        OnEdit onEdit = null;

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
            ImageButton share, edit, flow;
            share = new ImageButton(getContext());
            edit = new ImageButton(getContext());
            flow = new ImageButton(getContext());
            share.setScaleType(ImageView.ScaleType.CENTER_CROP);
            edit.setScaleType(ImageView.ScaleType.CENTER_CROP);
            flow.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int size = Device.screenY(getContext()) / 11;
            LinearLayout.LayoutParams buttons = new LayoutParams(size, size);
            share.setBackground(null);
            edit.setBackground(null);
            flow.setBackground(null);
            share.setImageDrawable(getContext().getDrawable(R.drawable.ic_share));
            edit.setImageDrawable(getContext().getDrawable(R.drawable.ic_create));
            flow.setImageDrawable(getContext().getDrawable(R.drawable.ic_run));
            share.setLayoutParams(buttons);
            edit.setLayoutParams(buttons);
            flow.setLayoutParams(buttons);
            bottomButtons.addView(edit);
            bottomButtons.addView(share);
            bottomButtons.addView(flow);
            bottomButtons.setPadding(5, 5, 5, 5);
            bottomButtons.setBackground(generateCoaster(configuration.getValue(Configuration.textColor, 0xff000000)));
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
                        onEdit.onEdit(configuration.getValue(Configuration.name, null));
                }
            });
            bottomButtons.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size + bottomButtons.getPaddingTop() + bottomButtons.getPaddingBottom()));
            left.setPadding(20, 0, 0, 0);
            right.setPadding(0, 0, 20, 0);
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

        public void setOnEdit(OnEdit onEdit) {
            this.onEdit = onEdit;
        }

        private void initStageDBluetooth() {
            TextView deviceName = new TextView(getContext());
            TextView deviceMac = new TextView(getContext());
            deviceName.setGravity(Gravity.CENTER);
            deviceName.setTextSize(20);
            deviceMac.setGravity(Gravity.CENTER);
            deviceMac.setTextSize(20);
            deviceName.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            deviceMac.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            String deviceNameText = "Device Name: " + configuration.getValue(Configuration.deviceName, "No Device");
            deviceName.setText(deviceNameText);
            String deviceMacText = "Device MAC: " + configuration.getValue(Configuration.deviceAddress, "No Mac");
            deviceMac.setText(deviceMacText);
            left.addView(deviceName);
            left.addView(deviceMac);
            TextView data = new TextView(getContext());
            data.setGravity(Gravity.CENTER);
            data.setTextSize(25);
            data.setTextColor(configuration.getValue(Configuration.textColor, 0xff000000));
            String dataText = "Data: \n" + configuration.getValue(Configuration.data, "No Data");
            data.setText(dataText);
            right.addView(data);
        }

        private void initStageDInternet() {
        }

        private Drawable generateCoaster(int color) {
            GradientDrawable gd = (GradientDrawable) getContext().getDrawable(R.drawable.rounded_rect);
            if (gd != null) {
                gd.setColor(color);
            }
            return gd;
        }

        public interface OnEdit {
            void onEdit(String conf);
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
    }

    static class BluetoothSession extends AsyncTask<String, Status, Status> {
        private Action action;
        private OnSessionEnd onSessionEnd;

        public BluetoothSession(Action action, OnSessionEnd onSessionEnd) {
            this.action = action;
            this.onSessionEnd = onSessionEnd;
        }

        @Override
        protected Main.Status doInBackground(String... configurations) {
            Main.Status returnStatus = new Main.Status(Main.Status.STATUS_STARTING);
            BluetoothAdapter blueAdapter;
            BluetoothManager manager = (BluetoothManager) action.c.getSystemService(Context.BLUETOOTH_SERVICE);
            SharedPreferences sp = action.c.getSharedPreferences(action.c.getPackageName(), Context.MODE_PRIVATE);
            Configuration configuration = new Configuration(sp.getString(action.config, "{}"));
            if (manager != null) {
                blueAdapter = manager.getAdapter();
                if (blueAdapter.isEnabled()) {
                    blueAdapter.cancelDiscovery();
                    if (!configuration.getValue(Configuration.deviceName, "").equals("") && !configuration.getValue(Configuration.deviceAddress, "").equals("")) {
                        final BluetoothDevice device = blueAdapter.getRemoteDevice(configuration.getValue(Configuration.deviceAddress, ""));
                        UUID uuid = device.getUuids()[0].getUuid();
                        try {
                            final BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                            returnStatus.status = Main.Status.STATUS_IN_PROGRESS;
                            try {
                                socket.connect();
                                while (!socket.isConnected())
                                    returnStatus.status = Main.Status.STATUS_IN_PROGRESS;
                                if (socket.isConnected()) {
                                    socket.getOutputStream().write(configuration.getValue(Configuration.data, "").getBytes());
                                    try {
                                        socket.getOutputStream().flush();
                                        socket.getOutputStream().close();
                                        socket.close();
                                    } catch (IOException ignored) {
                                    }
                                    returnStatus.status = Main.Status.STATUS_SUCCEDED;
                                } else {
                                    returnStatus.status = Main.Status.STATUS_FAILED;
                                }
                            } catch (IOException e) {
                                returnStatus.status = Main.Status.STATUS_FAILED;
                            }
                        } catch (IOException e) {
                            returnStatus.status = Main.Status.STATUS_FAILED;
                        }
                    }
                } else {
                    returnStatus.status = Main.Status.STATUS_FAILED;
                }
            }
            return returnStatus;
        }

        @Override
        protected void onPostExecute(Main.Status status) {
            super.onPostExecute(status);
            if (onSessionEnd != null) onSessionEnd.onSessionEnd(status);
        }

        public interface OnSessionEnd {
            void onSessionEnd(Main.Status result);
        }
    }
}