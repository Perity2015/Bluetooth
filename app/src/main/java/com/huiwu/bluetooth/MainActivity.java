package com.huiwu.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_configInfo;
    private Button btn_info;
    private Button btn_list;
    private Button btn_config;
    private TextView text_response;

    private final int REQUEST_DEVICE = 0x01;

    private static final int UART_PROFILE_CONNECTED = 20;

    private static final int UART_PROFILE_DISCONNECTED = 21;

    private final String TAG = MainActivity.class.getCanonicalName();

    private int mState = UART_PROFILE_DISCONNECTED;
    private BluetoothService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private int ble_state = 0;

    private final int ble_config_info = 1;

    private final int ble_info = 2;

    private final int ble_config = 3;

    private byte[] configBytes;

    private int configBytesSize = 0;

    private boolean write_success = true;

    int sequence_id = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btn_configInfo = (Button) findViewById(R.id.btn_configInfo);
        btn_info = (Button) findViewById(R.id.btn_info);
        btn_list = (Button) findViewById(R.id.btn_list);
        btn_config = (Button) findViewById(R.id.btn_config);
        text_response = (TextView) findViewById(R.id.text_response);

        btn_configInfo.setOnClickListener(this);
        btn_info.setOnClickListener(this);
        btn_list.setOnClickListener(this);
        btn_config.setOnClickListener(this);

        service_init();
    }

    private byte[] defaultConfigBytes1() {
        byte[] bytes = {
                -85, 0, 0, 47, -31, 70, 0, 1,
                2, 0, 1, 0, 42, -1, -1, -127,
        };
        return bytes;
    }

    private byte[] defaultConfigBytes() throws UnsupportedEncodingException {
        sequence_id += 1;

        byte[] configValues_1 = {-1, -1, -127,
                20, 0, 80, 40,
                0x00, 0x01,
                0x00, 0x01
        };

        byte[] configValues = new byte[configValues_1.length + 6];
        System.arraycopy(configValues_1, 0, configValues, 0, configValues_1.length);
        Calendar calendar = Calendar.getInstance();
        configValues[configValues_1.length] = (byte) (calendar.get(Calendar.YEAR) % 100);
        configValues[configValues_1.length + 1] = (byte) (calendar.get(Calendar.MONTH) + 1);
        configValues[configValues_1.length + 2] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        configValues[configValues_1.length + 3] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        configValues[configValues_1.length + 4] = (byte) calendar.get(Calendar.MINUTE);
        configValues[configValues_1.length + 5] = (byte) calendar.get(Calendar.SECOND);

//        configValues[configValues_1.length] = 16;
//        configValues[configValues_1.length + 1] = 4;
//        configValues[configValues_1.length + 2] = 27;
//        configValues[configValues_1.length + 3] = 10;
//        configValues[configValues_1.length + 4] = 0;
//        configValues[configValues_1.length + 5] = 0;

        byte[] UTF = "test".getBytes("GBK");
        byte[] UTF_1 = new byte[UTF.length + 1];
        UTF_1[0] = (byte) UTF.length;
        System.arraycopy(UTF, 0, UTF_1, 1, UTF.length);

        byte[] company = "慧物".getBytes("GBK");
        byte[] company_1 = new byte[company.length + 1];
        company_1[0] = (byte) company.length;
        System.arraycopy(company, 0, company_1, 1, company.length);

        byte[] goods = "疫苗".getBytes("GBK");
        byte[] goods_1 = new byte[goods.length + 1];
        goods_1[0] = (byte) goods.length;
        System.arraycopy(goods, 0, goods_1, 1, goods.length);

        byte[] place = "上海".getBytes("GBK");
        byte[] place_1 = new byte[place.length + 1];
        place_1[0] = (byte) place.length;
        System.arraycopy(place, 0, place_1, 1, place.length);

        byte[] back = "备注".getBytes("GBK");
        byte[] back_1 = new byte[back.length + 1];
        back_1[0] = (byte) back.length;
        System.arraycopy(back, 0, back_1, 1, back.length);

        byte[] keyValues = new byte[UTF_1.length + company_1.length + goods_1.length + place_1.length + back_1.length + configValues.length];
        System.arraycopy(configValues, 0, keyValues, 0, configValues.length);

        System.arraycopy(UTF_1, 0, keyValues, configValues.length, UTF_1.length);

        System.arraycopy(company_1, 0, keyValues, configValues.length + UTF_1.length, company_1.length);

        System.arraycopy(goods_1, 0, keyValues, configValues.length + UTF_1.length + company_1.length, goods_1.length);

        System.arraycopy(place_1, 0, keyValues, configValues.length + UTF_1.length + company_1.length + goods_1.length, place_1.length);

        System.arraycopy(back_1, 0, keyValues, configValues.length + UTF_1.length + company_1.length + goods_1.length + place_1.length, back_1.length);

        byte[] L2 = new byte[5 + keyValues.length];
        L2[0] = 0x02;
        L2[1] = 0x00;
        L2[2] = 0x01;
        L2[3] = (byte) (keyValues.length / 256);
        L2[4] = (byte) (0xFF & (byte) keyValues.length);
        System.arraycopy(keyValues, 0, L2, 5, keyValues.length);

        byte[] L1 = new byte[8 + L2.length];
        L1[0] = (byte) 0xAB;
        L1[1] = 0x00;
        L1[2] = (byte) (L2.length / 256);
        L1[3] = (byte) (0xFF & (byte) L2.length);

        int i = CRC.crcTable(L2);
        L1[4] = (byte) (i / 256);
        L1[5] = (byte) (0xFF & (byte) i);

        L1[6] = (byte) (sequence_id / 256);
        L1[7] = (byte) (0xFF & (byte) sequence_id);

        System.arraycopy(L2, 0, L1, 8, L2.length);
        return L1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.add("清空内容");
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                text_response.setText("");
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_list:
                if (btn_list.getText().equals("连接设备")) {

                    //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                    Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(newIntent, REQUEST_DEVICE);
                } else {
                    //Disconnect button pressed
                    if (mDevice != null) {
                        mService.disconnect();

                    }
                }
                break;
            case R.id.btn_configInfo:
                ble_state = ble_config_info;
                readInfo(ble_state);
                break;
            case R.id.btn_info:
                ble_state = ble_info;
                readInfo(ble_state);
                break;
            case R.id.btn_config:
                try {
                    configBytes = defaultConfigBytes();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                configBytesSize = 0;
                Log.d(TAG, Arrays.toString(configBytes));
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        while (configBytesSize < configBytes.length) {
                            if (write_success) {
                                write_success = false;
                                int num = configBytes.length - configBytesSize;
                                if (num > 20) {
                                    byte[] bytes = new byte[20];
                                    System.arraycopy(configBytes, configBytesSize, bytes, 0, 20);
                                    Log.d(TAG, Arrays.toString(bytes));

                                    mService.writeRXCharacteristic(bytes);
                                } else {
                                    byte[] bytes = new byte[num];
                                    System.arraycopy(configBytes, configBytesSize, bytes, 0, num);
                                    Log.d(TAG, Arrays.toString(bytes));

                                    mService.writeRXCharacteristic(bytes);
                                }
                                configBytesSize += 20;

                            }
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
                break;
        }
    }


    private void readInfo(int ble_state) {
        sequence_id += 1;

        byte[] bytes = new byte[18];
        bytes[0] = -85;
        bytes[1] = 0;
        bytes[2] = 0;
        bytes[3] = 5;
        bytes[6] = (byte) (sequence_id / 256);
        bytes[7] = (byte) (0xFF & (byte) sequence_id);

        byte[] arrayOfByte = new byte[5];
        arrayOfByte[0] = 1;
        arrayOfByte[1] = 0;
        if (ble_state == ble_config_info) {
            arrayOfByte[2] = 3;
        } else {
            arrayOfByte[2] = 2;
        }
        arrayOfByte[3] = 0;
        arrayOfByte[4] = 0;

        int i = CRC.crcTable(arrayOfByte);
        Log.d(TAG, String.valueOf(i));
        bytes[4] = (byte) (i / 256);
        bytes[5] = (byte) (0xFF & (byte) i);

        bytes[8] = arrayOfByte[0];
        bytes[9] = arrayOfByte[1];
        bytes[10] = arrayOfByte[2];
        bytes[11] = arrayOfByte[3];
        bytes[12] = arrayOfByte[4];

        mService.writeRXCharacteristic(bytes);
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((BluetoothService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, action);

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(BluetoothService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btn_list.setText("断开连接");
                        btn_info.setEnabled(true);
                        btn_configInfo.setEnabled(true);
                        btn_config.setEnabled(true);
                        mState = UART_PROFILE_CONNECTED;

                    }
                });
            }

            //*********************//
            if (action.equals(BluetoothService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btn_list.setText("连接设备");
                        btn_info.setEnabled(false);
                        btn_configInfo.setEnabled(false);
                        btn_config.setEnabled(false);
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();
//                        mService.enableTXNotification(false);
                    }
                });
            }

            if (action.equals(BluetoothService.ACTION_GATT_WRITE_SUCCESSED)) {
                write_success = true;
            }


            //*********************//
            if (action.equals(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification(true);

            }
            //*********************//
            if (action.equals(BluetoothService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);


                runOnUiThread(new Runnable() {
                    public void run() {
                        if (true) {
                            String text = Arrays.toString(txValue) + "__" + txValue.length;
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            text_response.append("\n[" + currentDateTimeString + "] : \n" + text);
                        }

                        try {
                            if (txValue[0] == (byte) 0xAB) {
                                String text = Arrays.toString(txValue) + "__" + txValue.length;
                                String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                text_response.append("\n[" + currentDateTimeString + "] : \n" + text);

                                sequence_id = CRC.Convert2bytesHexFormatToInt(new byte[]{txValue[6], txValue[7]});

                                if (ble_state == ble_config_info && txValue[1] == 0) {
                                    byte[] tempBytes = {txValue[2], txValue[3]};
                                    configBytesSize = CRC.Convert2bytesHexFormatToInt(tempBytes);
                                    configBytes = new byte[configBytesSize];

                                    Log.d(TAG, String.valueOf(configBytesSize));
                                }
                                return;
                            }

                            if (ble_state == ble_config_info) {
                                System.arraycopy(txValue, 0, configBytes, configBytes.length - configBytesSize, txValue.length);
                                configBytesSize = configBytesSize - txValue.length;
                                if (configBytesSize <= 0) {
                                    String text = Arrays.toString(configBytes);
                                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                    text_response.append("\n[" + currentDateTimeString + "] : \n" + text);
                                    parseConfigBytes(configBytes);
                                }
                            } else if (ble_state == ble_info) {
                                parseInfo(txValue);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(BluetoothService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };

    private void parseConfigBytes(byte[] configBytes) {
        String text = "记录数量：" + CRC.Convert2bytesHexFormatToInt(new byte[]{configBytes[7], configBytes[8]});
        text += "\n电池电量：" + configBytes[9];
        text += "\n设备编号：" + bytesToHexString(new byte[]{configBytes[10], configBytes[11], configBytes[12], configBytes[13]});
        text += "\n开始时间：" + configBytes[14] + "-" + configBytes[15] + "-" + configBytes[16] + " " + configBytes[17] + ":" + configBytes[18] + ":" + configBytes[19];
        text += "\n温度上限：" + configBytes[20];
        text += "\n温度下限：" + configBytes[21];
        text += "\n湿度上限：" + configBytes[22];
        text += "\n湿度下限：" + configBytes[23];
        text += "\n记录间隔：" + CRC.Convert2bytesHexFormatToInt(new byte[]{configBytes[24], configBytes[25]});
        text += "\n延迟时间：" + CRC.Convert2bytesHexFormatToInt(new byte[]{configBytes[26], configBytes[27]});
        text += "\n配置时间：" + configBytes[28] + "-" + configBytes[29] + "-" + configBytes[30] + " " + configBytes[31] + ":" + configBytes[32] + ":" + configBytes[33];

        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        text_response.append("\n[" + currentDateTimeString + "] : \n" + text);
    }

    private void parseInfo(byte[] infoBytes) {
        int length = infoBytes.length / 4;
        double[] info = new double[length * 2];
        for (int i = 0; i < length; i++) {
            info[2 * i] = (double) CRC.Convert2bytesHexFormatToInt(new byte[]{infoBytes[i * 4], infoBytes[4 * i + 1]}) / 100;
            info[2 * i + 1] = (double) CRC.Convert2bytesHexFormatToInt(new byte[]{infoBytes[i * 4 + 2], infoBytes[4 * i + 3]}) / 100;
        }

        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
//        text_response.append("\n[" + currentDateTimeString + "] : \n" + Arrays.toString(infoBytes));
        text_response.append("\n[" + currentDateTimeString + "] : \n" + Arrays.toString(info));
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, BluetoothService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothService.DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(BluetoothService.ACTION_GATT_WRITE_SUCCESSED);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getExtras().getString(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    mService.connect(deviceAddress);
                }
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
//            new AlertDialog.Builder(this)
//                    .setIcon(android.R.drawable.ic_dialog_alert)
//                    .setTitle(R.string.popup_title)
//                    .setMessage(R.string.popup_message)
//                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            finish();
//                        }
//                    })
//                    .setNegativeButton(R.string.popup_no, null)
//                    .show();
        }
    }

    public String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);

        for (int i = 0; i < bArray.length; ++i) {
            String sTemp = Integer.toHexString(255 & bArray[i]);
            if (sTemp.length() < 2) {
                sb.append(0);
            }

            sb.append(sTemp.toUpperCase());
        }

        return sb.toString();
    }

    public byte[] hexStringToBytes(String str) {
        int length = str.length() / 2;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = Byte.parseByte(str.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
