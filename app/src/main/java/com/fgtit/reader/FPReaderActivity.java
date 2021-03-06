package com.fgtit.reader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fgtit.data.Conversions;
import com.fgtit.data.wsq;
import com.fgtit.fpcore.FPMatch;
import com.fgtit.printer.DataUtils;
import com.fgtit.utils.FPDatabase;
import com.fgtit.utils.FingerprintResponse;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Copyright (c) 2019 Farmerline LTD. All rights reserved.
 * Created by Clement Osei Tano K on 27/11/2019.
 */
public class FPReaderActivity extends AppCompatActivity {
    // Debugging
    //directory for saving the fingerprint images
    private String sDirectory = "";
    private static final String TAG = "FPReaderActivity";

    //default image size
    public static final int IMG_WIDTH = 256;
    public static final int IMG_HEIGHT = 288;
    public static final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;
    public static final int WSQBUFSIZE = 200000;

    //other image size
    public static final int IMG200 = 200;
    public static final int IMG288 = 288;
    public static final int IMG360 = 360;

    //definition of commands
    private final static byte CMD_PASSWORD = 0x01;    //Password
    private final static byte CMD_ENROLID = 0x02;        //Enroll in Device
    private final static byte CMD_VERIFY = 0x03;        //Verify in Device
    private final static byte CMD_IDENTIFY = 0x04;    //Identify in Device
    private final static byte CMD_DELETEID = 0x05;    //Delete in Device
    private final static byte CMD_CLEARID = 0x06;        //Clear in Device

    private final static byte CMD_ENROLHOST = 0x07;    //Enroll to Host
    private final static byte CMD_CAPTUREHOST = 0x08;    //Caputre to Host
    private final static byte CMD_MATCH = 0x09;        //Match
    private final static byte CMD_GETIMAGE = 0x30;      //GETIMAGE
    private final static byte CMD_GETCHAR = 0x31;       //GETDATA


    private final static byte CMD_WRITEFPCARD = 0x0A;    //Write Card Data
    private final static byte CMD_READFPCARD = 0x0B;    //Read Card Data
    private final static byte CMD_CARDSN = 0x0E;        //Read Card Sn
    private final static byte CMD_GETSN = 0x10;

    private final static byte CMD_FPCARDMATCH = 0x13;   //

    private final static byte CMD_WRITEDATACARD = 0x14;    //Write Card Data
    private final static byte CMD_READDATACARD = 0x15;     //Read Card Data

    private final static byte CMD_PRINTCMD = 0x20;        //Printer Print
    private final static byte CMD_GETBAT = 0x21;
    private final static byte CMD_UPCARDSN = 0x43;
    private final static byte CMD_GET_VERSION = 0x22;        //Version

    private byte mDeviceCmd = 0x00;
    private boolean mIsWork = false;
    private byte[] mCmdData = new byte[10240];
    private int mCmdSize = 0;

    private Timer mTimerTimeout = null;
    private TimerTask mTaskTimeout = null;
    private Handler mHandlerTimeout;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private ListView mConversationView;
    private ImageView fingerprintImage;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayList<String> mConversationArray;
    private FPReaderAdapter readerAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothReaderService mChatService = null;

    //definition of variables which used for storing the fingerprint template
    public byte[] mRefData = new byte[512]; //enrolled FP template data
    public int mRefSize = 0;
    public byte[] mMatData = new byte[512];  // match FP template data
    public int mMatSize = 0;

    public byte[] mCardSn = new byte[7];
    public byte[] mCardData = new byte[4096];
    public int mCardSize = 0;

    public byte[] mBat = new byte[2];  // data of battery status
    public byte[] mUpImage = new byte[73728]; // image data
    public int mUpImageSize = 0;
    public int mUpImageCount = 0;


    public byte[] mRefCoord = new byte[512];
    public byte[] mMatCoord = new byte[512];

    public byte[] mIsoData = new byte[378];
    private byte lowHighByte;
    private Toolbar mToolbar;
    private TextView textSize, btState;
    private int imgSize;

    private int userId; // User ID number
    private FPDatabase fpDatabase; //SQLite database object

    //dynamic setting of the permission for writing the data into phone memory
    private int REQUEST_PERMISSION_CODE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // arguments
    private int verificationCount = 0;
    private int requiredEnrolment = 3;
    private boolean captureImages = true;
    private boolean enrolFinger = false;
    private int action;
    private String fingers;

    private HashMap<String, String> commandMessages = new HashMap<>();
    private int fingerArrayPosition = 0;
    private ArrayList<String> fingersArray = new ArrayList<>();

    // the id of the user we are enrolling
    private String fpRespondentId = null;
    private String fingerResponseId = null;
    private String currentFinger;
    private String currentJPGFile;
    private String currentWSQFile;
    private String currentRAWFile;
    private boolean forResult = false;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up the window layout
        setContentView(R.layout.activity_fp_reader);

        //checking the permission
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                        REQUEST_PERMISSION_CODE);
            }
        }

        Intent intent = getIntent();

        requiredEnrolment = intent.getIntExtra(Constants.VERIFICATION_COUNT_KEY, 1);
        captureImages = intent.getBooleanExtra(Constants.CAPTURE_IMAGES_KEY, true);
        enrolFinger = intent.getBooleanExtra(Constants.ENROL_FINGER_KEY, false);
        action = intent.getIntExtra(Constants.FP_ACTION, Constants.Actions.ENROL_FINGERPRINT);
        forResult = intent.getBooleanExtra("is_external", false);
        fingers = intent.getStringExtra(Constants.FINGERS_KEY);
        fpRespondentId = intent.getStringExtra(Constants.CUSTOM_RESPONDENT_ID);
        if (TextUtils.isEmpty(fingers))
            fingers = Constants.FINGERS.LEFT_HAND_THUMB;//Constants.FINGERS.ALL_FINGERS;

        commandMessages.put("0", "Scan Right Thumb");
        commandMessages.put("1", "Scan Right Index Finger");
        commandMessages.put("2", "Scan Right Middle Finger");
        commandMessages.put("3", "Scan Right Ring Finger");
        commandMessages.put("4", "Scan Right Pinky Finger");
        commandMessages.put("5", "Scan Left Thumb");
        commandMessages.put("6", "Scan Left Index Finger");
        commandMessages.put("7", "Scan Left Middle Finger");
        commandMessages.put("8", "Scan Left Ring Finger");
        commandMessages.put("9", "Scan Left Pinky Finger");
        commandMessages.put("10", "Scanner ready. Place finger on the scanner and lift after the beep");

        fingersArray.addAll(Arrays.asList(fingers.split("-")));

        CreateDirectory();

        textSize = findViewById(R.id.textSize);
        btState = findViewById(R.id.bt_state);
        //toolbar
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle("Capture Fingerprint");
        mToolbar.setTitleTextColor(getResources().getColor(R.color.fp_write));
        btState.setText(R.string.title_not_connected);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Drawable upArrow = getResources().getDrawable(R.drawable.ic_back);
        upArrow.setColorFilter(getResources().getColor(R.color.fp_write), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        mToolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.fp_write), PorterDuff.Mode.SRC_ATOP);

        mToolbar.setOnMenuItemClickListener(onMenuItemClick);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //initialize the match function of the fingerprint
        int feedback = FPMatch.getInstance().InitMatch(1, "");
        if (feedback == 0) {
            //Toast.makeText(this, "Init Match ok", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(this, "Init Match failed", Toast.LENGTH_SHORT).show();
        }

        //initialize the SQLite
        userId = 1;
        fpDatabase = new FPDatabase(this);
        fpDatabase.open();

        // show devices list and select one
        // Launch the DeviceListActivity to see devices and do scan
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();
        if (preferences.getString("selected_bt_device", "").isEmpty()) {
            Intent serverIntent = new Intent(FPReaderActivity.this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }else {
            connectDevice(preferences.getString("selected_bt_device", ""));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        setResult(RESULT_CANCELED, null);
        finish();
        return super.onOptionsItemSelected(item);
    }

    // The Handler that gets information back from the BluetoothChatService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothReaderService.STATE_CONNECTED:
                            btState.setText("Connected to: " + mConnectedDeviceName);
                            mConversationArray.clear();
                            readerAdapter = new FPReaderAdapter(FPReaderActivity.this, mConversationArray);
                            mConversationView.setAdapter(readerAdapter);
                            // begin action
                            switch (action) {
                                case Constants.Actions.ENROL_FINGERPRINT:
                                case Constants.Actions.VERIFY_FINGERPRINT:
                                if (enrolFinger) {
                                    //save the respondent into database
                                    if (TextUtils.isEmpty(fpRespondentId)) {
                                        fpRespondentId = UUID.randomUUID().toString();
                                    }

                                    fpDatabase.insertRespondent(fpRespondentId, captureImages, requiredEnrolment);

                                    //Log.e("DEBUG_RESPONDENT", "FP Respondent ID: " + fpRespondentId);

                                    // start enrolling
                                    SendCommand(CMD_ENROLHOST, null, 0);
                                } else {
                                    SendCommand(CMD_CAPTUREHOST, null, 0);
                                }
                                break;
                                case Constants.Actions.READ_CARD:
                                    SendCommand(CMD_READFPCARD, null, 0);
                                    break;
                                case Constants.Actions.WRITE_CARD:
                                    SendCommand(CMD_WRITEFPCARD, null, 0);
                                    break;
                            }
                            break;
                        case BluetoothReaderService.STATE_CONNECTING:
                            btState.setText(R.string.title_connecting);
                            break;
                        case BluetoothReaderService.STATE_LISTEN:
                        case BluetoothReaderService.STATE_NONE:
                            btState.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    Log.e("DEBUG_DEVICE_MSG", msg.toString());
                    byte[] readBuf = (byte[]) msg.obj;
                    Log.e("DEBUG_DEVICE_MSG", print(readBuf));
                    if (readBuf.length > 0) {
                        if (readBuf[0] == (byte) 0x1b) {
                            AddStatusListHex(readBuf, msg.arg1);
                        } else {
                            ReceiveCommand(readBuf, msg.arg1);
                        }
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public synchronized void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothReaderService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void AddStatusList(String text) {
        mConversationArray.add(text);
        readerAdapter = new FPReaderAdapter(this, mConversationArray);
        mConversationView.setAdapter(readerAdapter);
    }

    private void AddStatusListHex(byte[] data, int size) {
        String text = "";
        for (int i = 0; i < size; i++) {
            text = text + " " + Integer.toHexString(data[i] & 0xFF).toUpperCase() + "  ";
        }
        mConversationArray.add(text);
        readerAdapter = new FPReaderAdapter(this, mConversationArray);
        mConversationView.setAdapter(readerAdapter);
    }

    public static String print(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String print(byte[] bytes, int size) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int i = 0; i < size; i++) {
            byte b = bytes[i];
            sb.append(String.format("%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String printASCII(byte[] bytes, int size) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int i = 0; i < size; i++) {
            byte b = bytes[i];
            sb.append(String.format("%s", (char) b));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * method of copying the byte[] data with specific length
     *
     * @param dstbuf    byte[] for storing the copied data with specific length
     * @param dstoffset the starting point for storing
     * @param srcbuf    the source byte[] used for copying.
     * @param srcoffset the starting point for copying
     * @param size      the length required to copy
     */
    private void memcpy(byte[] dstbuf, int dstoffset, byte[] srcbuf, int srcoffset, int size) {
        for (int i = 0; i < size; i++) {
            dstbuf[dstoffset + i] = srcbuf[srcoffset + i];
        }
    }

    /**
     * calculate the check sum of the byte[]
     *
     * @param buffer byte[] required for calculating
     * @param size   the size of the byte[]
     * @return the calculated check sum
     */
    private int calcCheckSum(byte[] buffer, int size) {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum = sum + buffer[i];
        }
        return (sum & 0x00ff);// the lower byte of the value
    }

    private byte[] changeByte(int data) {
        byte b4 = (byte) ((data) >> 24);
        byte b3 = (byte) (((data) << 8) >> 24);
        byte b2 = (byte) (((data) << 16) >> 24);
        byte b1 = (byte) (((data) << 24) >> 24);
        byte[] bytes = {b1, b2, b3, b4};
        return bytes;
    }

    /**
     * generate the image data into Bitmap format
     *
     * @param width  width of the image
     * @param height height of the image
     * @param data   image data
     * @return bitmap image data
     */
    private byte[] toBmpByte(int width, int height, byte[] data) {
        byte[] buffer = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            int bfType = 0x424d;
            int bfSize = 54 + 1024 + width * height;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            int bfOffBits = 54 + 1024;

            dos.writeShort(bfType);
            dos.write(changeByte(bfSize), 0, 4);
            dos.write(changeByte(bfReserved1), 0, 2);
            dos.write(changeByte(bfReserved2), 0, 2);
            dos.write(changeByte(bfOffBits), 0, 4);

            int biSize = 40;
            int biWidth = width;
            int biHeight = height;
            int biPlanes = 1;
            int biBitcount = 8;
            int biCompression = 0;
            int biSizeImage = width * height;
            int biXPelsPerMeter = 0;
            int biYPelsPerMeter = 0;
            int biClrUsed = 256;
            int biClrImportant = 0;

            dos.write(changeByte(biSize), 0, 4);
            dos.write(changeByte(biWidth), 0, 4);
            dos.write(changeByte(biHeight), 0, 4);
            dos.write(changeByte(biPlanes), 0, 2);
            dos.write(changeByte(biBitcount), 0, 2);
            dos.write(changeByte(biCompression), 0, 4);
            dos.write(changeByte(biSizeImage), 0, 4);
            dos.write(changeByte(biXPelsPerMeter), 0, 4);
            dos.write(changeByte(biYPelsPerMeter), 0, 4);
            dos.write(changeByte(biClrUsed), 0, 4);
            dos.write(changeByte(biClrImportant), 0, 4);

            byte[] palatte = new byte[1024];
            for (int i = 0; i < 256; i++) {
                palatte[i * 4] = (byte) i;
                palatte[i * 4 + 1] = (byte) i;
                palatte[i * 4 + 2] = (byte) i;
                palatte[i * 4 + 3] = 0;
            }
            dos.write(palatte);

            dos.write(data);
            dos.flush();
            buffer = baos.toByteArray();
            dos.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * generate the fingerprint image
     *
     * @param data   image data
     * @param width  width of the image
     * @param height height of the image
     * @param offset default setting as 0
     * @return bitmap image data
     */
    public byte[] getFingerprintImage(byte[] data, int width, int height, int offset) {
        if (data == null) {
            return null;
        }
        byte[] imageData = new byte[width * height];
//        Log.e("DEBUG_DATA", "Offset even: "+(0xf0));
//        Log.e("DEBUG_DATA", "Offset odd: "+(4 & 0xf0));
        for (int i = 0; i < (width * height / 2); i++) {
            imageData[i * 2] = (byte) (data[i + offset] & 0xf0);
            imageData[i * 2 + 1] = (byte) (data[i + offset] << 4 & 0xf0);
//            Log.e("DEBUG_DATA", "Actual byte: "+(data[i + offset]));
//            Log.e("DEBUG_DATA", "Actual byte and f0: "+(data[i + offset] & 0xf0));
//            Log.e("DEBUG_DATA", "Offset byte ls4 and f0: "+(data[i + offset] << 4 & 0xf0));
        }
        byte[] bmpData = toBmpByte(width, height, imageData);
        return bmpData;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
    }

    /**
     * configure for the UI components
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArray = new ArrayList<>();
        readerAdapter = new FPReaderAdapter(this, mConversationArray);
        mConversationView = findViewById(R.id.in);
        mConversationView.setAdapter(readerAdapter);

        fingerprintImage = findViewById(R.id.imageView1);

        final Button enrollFPButton = findViewById(R.id.id_enroll_fp);
        enrollFPButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendCommand(CMD_ENROLHOST, null, 0);
            }
        });

        final Button captureFPButton = findViewById(R.id.id_capture_fp);
        captureFPButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendCommand(CMD_CAPTUREHOST, null, 0);
            }
        });

        final Button searchFBButton = findViewById(R.id.id_search_fp);
        searchFBButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Match In System
                int score = FPMatch.getInstance().MatchFingerData(mRefData, mMatData);
                AddStatusList("Match Score:" + score);
            }
        });

        final Button getSerialNumberButton = findViewById(R.id.id_get_sn);
        getSerialNumberButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendCommand(CMD_GETSN, null, 0);
            }
        });

        final Button getBatteryLevelButton = findViewById(R.id.id_get_bat);
        getBatteryLevelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendCommand(CMD_GETBAT, null, 0);
            }
        });


        final Button getImage200Button = findViewById(R.id.id_get_fp_img_200);
        getImage200Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imgSize = IMG200;
                mUpImageSize = 0;
                SendCommand(CMD_GETIMAGE, null, 0);
            }
        });

        final Button getImage288Button = findViewById(R.id.id_get_fp_img_288);
        getImage288Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imgSize = IMG288;
                mUpImageSize = 0;
                SendCommand(CMD_GETIMAGE, null, 0);
            }
        });

        final Button getImage360Button = findViewById(R.id.id_get_fp_img_360);
        getImage360Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imgSize = IMG360;
                mUpImageSize = 0;
                SendCommand(CMD_GETIMAGE, null, 0);
            }
        });

        final Button clearDBButton = findViewById(R.id.id_clear_db);
        clearDBButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                userId = 1;
                fpDatabase.deleteAllUsers();
                fpDatabase.execSQL("update sqlite_sequence set seq=0 where name='User'");
                AddStatusList("Clear DB ok!");
            }
        });


        final Button getDataButton = findViewById(R.id.id_get_data);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendCommand(CMD_GETCHAR, null, 0);
            }
        });

        final Button getDeviceVersionButton = findViewById(R.id.id_get_version);
        getDeviceVersionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendCommand(CMD_GET_VERSION, null, 0);
            }
        });

        mChatService = new BluetoothReaderService(this, mHandler);    // Initialize the BluetoothChatService to perform bluetooth connections
        mOutStringBuffer = new StringBuffer();                    // Initialize the buffer for outgoing messages
    }

    /**
     * stat the timer for counting
     */
    public void TimeOutStart() {
        if (mTimerTimeout != null) {
            return;
        }
        mTimerTimeout = new Timer();
        mHandlerTimeout = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TimeOutStop();
                if (mIsWork) {
                    mIsWork = false;
                    //AddStatusList("Time Out");
                }
                super.handleMessage(msg);
            }
        };
        mTaskTimeout = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                mHandlerTimeout.sendMessage(message);
            }
        };
        mTimerTimeout.schedule(mTaskTimeout, 10000, 10000);
    }

    /**
     * stop the timer
     */
    public void TimeOutStop() {
        if (mTimerTimeout != null) {
            mTimerTimeout.cancel();
            mTimerTimeout = null;
            mTaskTimeout.cancel();
            mTaskTimeout = null;
        }
    }

    /**
     * Generate the command package sending via bluetooth
     *
     * @param cmdid command code for different function achieve.
     * @param data  the required data need to send to the device
     * @param size  the size of the byte[] data
     */
    private void SendCommand(byte cmdid, byte[] data, int size) {
        if (mIsWork) return;

        int sendsize = 9 + size;
        byte[] sendbuf = new byte[sendsize];
        sendbuf[0] = 'F';
        sendbuf[1] = 'T';
        sendbuf[2] = 0;
        sendbuf[3] = 0;
        sendbuf[4] = cmdid;
        sendbuf[5] = (byte) (size);
        sendbuf[6] = (byte) (size >> 8);

        // append all the bytes in the data variable to the command position 7 and above
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                sendbuf[7 + i] = data[i];
            }
        }

        // get a checksum of the command generated so far (pass the command and it's size)
        int sum = calcCheckSum(sendbuf, (7 + size));

        // append the checksum to the command (can we assume the device gets it with command[command.length -2]?)
        sendbuf[7 + size] = (byte) (sum);

        // zero the lower byte of the checksum
        sendbuf[8 + size] = (byte) (sum >> 8);

        //Log.e("DEBUG_COMMAND", print(sendbuf));

        mIsWork = true;
        TimeOutStart();
        mDeviceCmd = cmdid;
        mCmdSize = 0;
        mChatService.write(sendbuf);

        Log.e("DEBUG_COMMAND_ID", "Command ID: " + mDeviceCmd);

        switch (sendbuf[4]) {
            case CMD_PASSWORD:
                break;
            case CMD_ENROLID:
                AddStatusList("Enrol ID ...");
                break;
            case CMD_VERIFY:
                AddStatusList("Verify ID ...");
                break;
            case CMD_IDENTIFY:
                AddStatusList("Search ID ...");
                break;
            case CMD_DELETEID:
                AddStatusList("Delete ID ...");
                break;
            case CMD_CLEARID:
                AddStatusList("Clear ...");
                break;
            case CMD_ENROLHOST: {
                currentFinger = fingersArray.get(fingerArrayPosition);
                AddStatusList(commandMessages.get(currentFinger));
            }
            break;
            case CMD_CAPTUREHOST:
                AddStatusList("Capture Template ...");
                break;
            case CMD_MATCH:
                AddStatusList("Match Template ...");
                break;
            case CMD_WRITEFPCARD:
            case CMD_WRITEDATACARD:
                AddStatusList("Write Card ...");
                break;
            case CMD_READFPCARD:
            case CMD_READDATACARD:
                AddStatusList("Read Card ...");
                break;
            case CMD_FPCARDMATCH:
                AddStatusList("FingerprintCard Match ...");
                break;
            case CMD_CARDSN:
                AddStatusList("Read Card SN ...");
                break;
            case CMD_GETSN:
                AddStatusList("Get Device SN ...");
                break;
            case CMD_GETBAT:
                AddStatusList("Get Battery Value ...");
                break;
            case CMD_GETIMAGE:
                mUpImageSize = 0;
                //AddStatusList("Get Fingerprint Image ...");
                break;
            case CMD_GETCHAR:
                AddStatusList("Get Fingerprint Data ...");
                break;
            case CMD_GET_VERSION:
                AddStatusList("Get Version ...");
                break;
        }
    }

    /**
     * Received the response from the device
     *
     * @param databuf  the data package response from the device (1024 bytes of data)
     * @param datasize the size of the data package (the length of useful data)
     */
    private void ReceiveCommand(byte[] databuf, int datasize) {
        //Log.e("DEBUG_DATA", print(databuf, datasize));
        Log.e("DEBUG_DATA", "Data stream length: " + databuf.length + " Data size: " + datasize);
        if (mDeviceCmd == CMD_GETIMAGE) { //receiving the image data from the device
            if (imgSize == IMG200) {   //image size with 152*200
                memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                if (mUpImageSize >= 15200) {
                    saveRawData();

                    byte[] bmpdata = getFingerprintImage(mUpImage, 152, 200, 0/*18*/);
                    textSize.setText("152 * 200");
                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    saveJPGimage(image);
                    Log.d(TAG, "bmpdata.length:" + bmpdata.length);
                    fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mUpImageCount = mUpImageCount + 1;
                    mIsWork = false;
                    //AddStatusList("Display Image");
                }
            } else if (imgSize == IMG288) {   //image size with 256*288
                memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                if (mUpImageSize >= 36864) {
                    saveRawData();

                    // the array for the raw fingerprint data; this gives us data of size 73728 bytes
                    byte[] bmpdata = getFingerprintImage(mUpImage, 256, 288, 0/*18*/);
                    textSize.setText("256 * 288");

                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    saveJPGimage(image);

                    byte[] inpdata = new byte[73728];
                    int inpsize = 73728;
                    System.arraycopy(bmpdata, 1078, inpdata, 0, inpsize);
                    SaveWsqFile(inpdata, inpsize);

                    Log.d(TAG, "bmpdata.length:" + bmpdata.length);
                    fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mUpImageCount = mUpImageCount + 1;
                    mIsWork = false;
                    //AddStatusList("Display Image");

                    saveFingerResponse(verificationCount - 1);
                    if (captureImages)
                        checkRerun(false);
                }
            } else if (imgSize == IMG360) {   //image size with 256*360
                memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                //AddStatusList("Image Len="+Integer.toString(mUpImageSize)+"--"+Integer.toString(mUpImageCount));
                if (mUpImageSize >= 46080) {
                    saveRawData();

                    byte[] bmpdata = getFingerprintImage(mUpImage, 256, 360, 0/*18*/);
                    textSize.setText("256 * 360");
                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    saveJPGimage(image);

                    byte[] inpdata = new byte[92160];
                    int inpsize = 92160;
                    System.arraycopy(bmpdata, 1078, inpdata, 0, inpsize);
                    SaveWsqFile(inpdata, inpsize);

                    Log.d(TAG, "bmpdata.length:" + bmpdata.length);
                    fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mUpImageCount = mUpImageCount + 1;
                    mIsWork = false;
                    //AddStatusList("Display Image");

                    saveFingerResponse(verificationCount - 1);
                    if (captureImages)
                        checkRerun(false);
                }
            }
        } else { //other data received from the device
            // append the databuf received into mCmdData.
            memcpy(mCmdData, mCmdSize, databuf, 0, datasize);
            mCmdSize = mCmdSize + datasize;
            int totalsize = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) + 9;
            if (mCmdSize >= totalsize) {
                mCmdSize = 0;
                mIsWork = false;
                TimeOutStop();

                //parsing the mCmdData
                if ((mCmdData[0] == 'F') && (mCmdData[1] == 'T')) {
                    switch (mCmdData[4]) {
                        case CMD_PASSWORD: {
                        }
                        break;
                        case CMD_ENROLID: {
                            if (mCmdData[7] == 1) {
                                //int id=mCmdData[8]+(mCmdData[9]<<8);
                                int id = mCmdData[8] + (byte) ((mCmdData[9] << 8) & 0xFF00);
                                AddStatusList("Enrol Succeed:" + id);
                                Log.d(TAG, String.valueOf(id));
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_VERIFY: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Verify Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_IDENTIFY: {
                            if (mCmdData[7] == 1) {
                                int id = mCmdData[8] + (byte) ((mCmdData[9] << 8) & 0xFF00);
                                //int id=mCmdData[8]+(mCmdData[9]<<8);
                                AddStatusList("Search Result:" + id);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_DELETEID: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Delete Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_CLEARID: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Clear Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_ENROLHOST: {// insert a fingerprint into the local database
                            enrolFinger();
                        }
                        break;
                        case CMD_CAPTUREHOST: {// compare a fingerprint to
                            verifyFinger();
                        }
                        break;
                        case CMD_MATCH: {
                            int score = mCmdData[8] + ((mCmdData[9] << 8) & 0xFF00);
                            if (mCmdData[7] == 1)
                                AddStatusList("Match Succeed:" + score);
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_WRITEFPCARD: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Write Fingerprint Card Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_READFPCARD: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00);
                            if (size > 0) {
                                memcpy(mCardData, 0, mCmdData, 8, size);
                                mCardSize = size;
                                AddStatusList("Read Fingerprint Card Succeed");
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_FPCARDMATCH: {
                            if (mCmdData[7] == 1) {
                                AddStatusList("Fingerprint Match Succeed");
                                int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                byte[] tmpbuf = new byte[size];
                                memcpy(tmpbuf, 0, mCmdData, 8, size);
                                AddStatusList("Len=" + size);
                                AddStatusListHex(tmpbuf, size);
                                String txt = new String(tmpbuf);
                                AddStatusList(txt);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_UPCARDSN:
                        case CMD_CARDSN: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xF0) - 1;
                            if (size > 0) {
                                memcpy(mCardSn, 0, mCmdData, 8, size);
                                AddStatusList("Read Card SN Succeed:" + Integer.toHexString(mCardSn[0] & 0xFF) + Integer.toHexString(mCardSn[1] & 0xFF) + Integer.toHexString(mCardSn[2] & 0xFF) + Integer.toHexString(mCardSn[3] & 0xFF) + Integer.toHexString(mCardSn[4] & 0xFF) + Integer.toHexString(mCardSn[5] & 0xFF) + Integer.toHexString(mCardSn[6] & 0xFF));
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_WRITEDATACARD: {
                            if (mCmdData[7] == 1) {
                                AddStatusList("Write Card Data Succeed");
                            } else {
                                AddStatusList("Search Fail");
                            }
                        }
                        break;
                        case CMD_READDATACARD: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00);
                            if (size > 0) {
                                memcpy(mCardData, 0, mCmdData, 8, size);
                                Log.d(TAG, DataUtils.bytesToStr(mCardData));
                                mCardSize = size;
                                AddStatusListHex(mCardData, size);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_GETSN: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                byte[] snb = new byte[32];
                                memcpy(snb, 0, mCmdData, 8, size);
                                String sn = null;
                                try {
                                    sn = new String(snb, 0, size, "UNICODE");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                AddStatusList("SN:" + sn);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_PRINTCMD: {
                            if (mCmdData[7] == 1) {
                                AddStatusList("Print OK");
                            } else {
                                AddStatusList("Search Fail");
                            }
                        }
                        break;
                        case CMD_GETBAT: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (size > 0) {
                                memcpy(mBat, 0, mCmdData, 8, size);
                                double batVal = mBat[0] / 10.0;
                                double batPercent = ((batVal - 3.45) / 0.75) * 100;
                                DecimalFormat decimalFormat = new DecimalFormat("0.00");
                                String batPercentage = decimalFormat.format(batPercent) + " %";
                                AddStatusList("Battery Percentage:" + batPercentage);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_GETCHAR: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                memcpy(mMatData, 0, mCmdData, 8, size);
                                mMatSize = size;
                                AddStatusList("Len=" + mMatSize);
                                AddStatusList("Get Data Succeed");
                                AddStatusListHex(mMatData, mMatSize);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case CMD_GET_VERSION: {
                            int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                memcpy(mMatData, 0, mCmdData, 8, size);
                                AddStatusList("Version:" + bytesToAscii(mMatData));
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * A method to verify a fingerprint
     */
    private void verifyFinger() {
        int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
        if (mCmdData[7] == 1) {
            memcpy(mMatData, 0, mCmdData, 8, size);
            mMatSize = size;

            // get the users in our database
            ArrayList<FingerprintResponse> fingerprintResponses = fpDatabase.getFingerprintResponses();

            // a flag for the matching
            boolean matchFlag = false;

            if (fingerprintResponses != null) {
                if (fingerprintResponses.size() > 0) {
                    for (FingerprintResponse responnse : fingerprintResponses) {
                        byte[] enrol1 = responnse.getFpData();
                        String respondent = responnse.getFpRespondentId();
                        int ret = FPMatch.getInstance().MatchFingerData(enrol1, mMatData);
                        if (ret >= 70) {
                            AddStatusList("Got a match");
                            matchFlag = true;

                            if (forResult) {
                                Intent data = new Intent();
                                data.putExtra(Constants.RESPONDENT_RESULT, respondent);
                                setResult(RESULT_OK, data);
                                finish();
                            }
                            break;
                        }
                    }
                }
                else {
                    reScan(true);
                }
            }
            if (!matchFlag) {
                AddStatusList("No match found!");

                Intent data = new Intent();
                data.putExtra(Constants.RESPONDENT_RESULT, "");
                setResult(RESULT_OK, data);
                finish();
            }

            //ISO Format
            String bsiso = Conversions.getInstance().IsoChangeCoord(mMatData, 1);
            //SaveTextToFile(bsiso,"/sdcard/iso2.txt");

            File file = new File("/sdcard/fingerprint2.dat");
            try {
                file.createNewFile();

                FileOutputStream out = new FileOutputStream(file);
                out.write(mRefData);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            AddStatusList("Search Fail");
    }

    /**
     * A method to add a fingerprint into our local database
     */
    private void enrolFinger() {
        int size = mCmdData[5] + ((mCmdData[6] << 8) & 0xFF00) - 1;
        if (mCmdData[7] == 1) {
            if (!TextUtils.isEmpty(fpRespondentId))
                fingerResponseId = UUID.randomUUID().toString();

            // update the verification count
            verificationCount = verificationCount + 1;
            memcpy(mRefData, 0, mCmdData, 8, size);
            mRefSize = size;
            fpDatabase.insertUser(mRefData);
            userId += 1;

            if (!captureImages) {
                checkRerun(false);

                // save fingerprint for respondent
                saveFingerResponse(verificationCount - 1);
            } else {
                captureImage();
                Log.e("DEBUG_FLOW", "Returned");
            }
        } else {
            checkRerun(true);
        }
    }

    /**
     * A method to save the finger recorded
     *
     * @param vc the verification index
     */
    private void saveFingerResponse(int vc) {
        if (enrolFinger && !TextUtils.isEmpty(fpRespondentId)) {
            fpDatabase.createFingerprintResponse(fpRespondentId, currentFinger, mRefData, fingerResponseId, vc, currentJPGFile, currentWSQFile, currentRAWFile);
            currentJPGFile = null;
            currentWSQFile = null;
            currentRAWFile = null;
        }
    }

    private void captureImage() {
        imgSize = IMG288;
        mUpImageSize = 0;
        SendCommand(CMD_GETIMAGE, null, 0);
    }

    /**
     * A method to check whether the there should be a rerun of the fingerprint reader.
     * It adds a delay of 800ms
     *
     * @param b whether the previous run failed or not
     */
    private void checkRerun(final boolean b) {
        new CountDownTimer(800, 200) {
            /**
             * Callback fired on regular interval.
             *
             * @param millisUntilFinished The amount of time until finished.
             */
            @Override
            public void onTick(long millisUntilFinished) {

            }

            /**
             * Callback fired when the time is up.
             */
            @Override
            public void onFinish() {
                if (!b) {
                    if (verificationCount < requiredEnrolment) {
                        //AddStatusList("Scan finger again");
                        SendCommand(CMD_ENROLHOST, null, 0);
                    } else if (verificationCount == requiredEnrolment) {
                        verificationCount = 0;
                        if (fingerArrayPosition == fingersArray.size() - 1) {
                            fingerArrayPosition = 0;
                            AddStatusList("Enrolment complete");

                            if (forResult) {
                                Intent data = new Intent();
                                data.putExtra(Constants.RESPONDENT_RESULT, fpRespondentId);
                                setResult(RESULT_OK, data);
                                finish();
                            }
                        } else {
                            fingerArrayPosition = fingerArrayPosition + 1;
                            SendCommand(CMD_ENROLHOST, null, 0);
                        }
                    }
                } else {
                    if (verificationCount < requiredEnrolment) {
                        AddStatusList("Failed: Scan finger again");
                        SendCommand(CMD_ENROLHOST, null, 0);
                    } else if (verificationCount == requiredEnrolment) {
                        AddStatusList("Failed: Enrolment complete");
                        verificationCount = 0;
                    }
                }
            }
        }.start();
    }

    /**
     * A method to check whether the there should be a rerun of the fingerprint reader.
     * It adds a delay of 800ms
     *
     * @param b whether the previous run failed or not
     */
    private void reScan(final boolean b) {
        new CountDownTimer(800, 200) {
            /**
             * Callback fired on regular interval.
             *
             * @param millisUntilFinished The amount of time until finished.
             */
            @Override
            public void onTick(long millisUntilFinished) {

            }

            /**
             * Callback fired when the time is up.
             */
            @Override
            public void onFinish() {
                if (!b) {
                    if (verificationCount < requiredEnrolment) {
                        //AddStatusList("Scan finger again");
                        SendCommand(CMD_CAPTUREHOST, null, 0);
                    } else if (verificationCount == requiredEnrolment) {
                        verificationCount = 0;
                        if (fingerArrayPosition == fingersArray.size() - 1) {
                            fingerArrayPosition = 0;
                            AddStatusList("Found a match");
                        } else {
                            fingerArrayPosition = fingerArrayPosition + 1;
                            SendCommand(CMD_CAPTUREHOST, null, 0);
                        }
                    }
                } else {
                    if (verificationCount < requiredEnrolment) {
                        AddStatusList("Failed: Scan finger again");
                        SendCommand(CMD_CAPTUREHOST, null, 0);
                    } else if (verificationCount == requiredEnrolment) {
                        AddStatusList("Could not find any matches");
                        verificationCount = 0;
                    }
                }
            }
        }.start();
    }

    private void connectDevice(String address){
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        if (mChatService == null) setupChat();
        mChatService.connect(device);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    connectDevice(address);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // setting for the tool bar menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Create directory folder for storing the images
     */
    public void CreateDirectory() {
//        sDirectory = Environment.getExternalStorageDirectory() + "/Fingerprint Images/";
//        File destDir = new File(sDirectory);
//        if (!destDir.exists()) {
//            destDir.mkdirs();
//        }
    }

    /**
     * method for saving the fingerprint image as JPG
     *
     * @param bitmap bitmap image
     */
    public void saveJPGimage(Bitmap bitmap) {
        String dir = sDirectory;
        // create the image file name
        String imageFileName = !TextUtils.isEmpty(fpRespondentId) ? fpRespondentId + "_" +
                fingerResponseId + ".jpg" : (System.currentTimeMillis()) + ".jpg";

        try {
            // create the actual file to save
            File file = new File(dir, imageFileName);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            // set the JPG field for the enrollment
            if (!TextUtils.isEmpty(fpRespondentId) && enrolFinger && captureImages && file.exists())
                currentJPGFile = file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * method of saving the image into WSQ format
     *
     * @param rawdata raw image data.
     * @param rawsize size of the raw image data.
     */
    public void SaveWsqFile(byte[] rawdata, int rawsize) {
        byte[] outdata = new byte[rawsize];
        int[] outsize = new int[1];

        if (rawsize == 73728) {
            wsq.getInstance().RawToWsq(rawdata, rawsize, 256, 288, outdata, outsize, 2.833755f);
        } else if (rawsize == 92160) {
            wsq.getInstance().RawToWsq(rawdata, rawsize, 256, 360, outdata, outsize, 2.833755f);
        }

        try {
            String dir = sDirectory;
            // create the image file name
            String imageFileName = !TextUtils.isEmpty(fpRespondentId) ? fpRespondentId + "_" +
                    fingerResponseId + ".wsq" : (System.currentTimeMillis()) + ".wsq";

            File file = new File(dir, imageFileName);
            RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
            long fileLength = randomFile.length();
            randomFile.seek(fileLength);
            randomFile.write(outdata, 0, outsize[0]);
            randomFile.close();

            // set the WSQ field for the enrollment
            if (!TextUtils.isEmpty(fpRespondentId) && enrolFinger && captureImages && file.exists())
                currentWSQFile = file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveRawData() {
        String dir = sDirectory;
        // create the image file name
        String imageFileName = !TextUtils.isEmpty(fpRespondentId) ? fpRespondentId + "_" +
                fingerResponseId + ".raw" : (System.currentTimeMillis()) + ".raw";

        File file = new File(dir, imageFileName);

        try {
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            out.write(mUpImage);
            out.close();

            // set the RAW field for the enrollment
            if (!TextUtils.isEmpty(fpRespondentId) && enrolFinger && captureImages && file.exists())
                currentRAWFile = file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.scan) {// Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(FPReaderActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            } else if (itemId == R.id.discoverable) {// Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            return true;
        }
    };

    public String bytesToAscii(byte[] bytes, int offset, int dateLen) {
        if ((bytes == null) || (bytes.length == 0) || (offset < 0) || (dateLen <= 0)) {
            return null;
        }
        if ((offset >= bytes.length) || (bytes.length - offset < dateLen)) {
            return null;
        }

        String asciiStr = null;
        byte[] data = new byte[dateLen];
        System.arraycopy(bytes, offset, data, 0, dateLen);
        try {
            asciiStr = new String(data, "ISO8859-1");
        } catch (UnsupportedEncodingException e) {
        }
        return asciiStr;
    }

    //display the first 6 bytes of data.
    public String bytesToAscii(byte[] bytes) {
        return bytesToAscii(bytes, 0, 6);
    }
}
