package com.smartparking.amrbadr12.smartparkingadmin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter bluetoothAdapter;
    public static final String TAG="Main Activity";
    private StateChangedBroadcastReciever btChangedStateListener;
    private ArrayList<DeviceItem> btDevicesList;
    private ListView pairedListView;
    private DevicesListAdapter adapter;
    private EditText writeEditText;
    private Button writeButton;
    private TextView readStreamText;
    private StringBuilder messages;
    private Button disconnectButton;
    private ArrayList<BluetoothDevice>btDevices;
    private ReadMessagesBroadcastReciever readBroadcastReciever;
    private BluetoothConnectionService bluetoothConnectionService;
    private boolean isBtConnected;
    private BluetoothDeviceFoundBroadcastReciever bluetoothDeviceFoundBroadcastReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        disconnectButton=(Button)findViewById(R.id.disconnected_button);
        isBtConnected=false;
        messages=new StringBuilder();
        writeEditText=(EditText)findViewById(R.id.write_to_stream);
        writeButton=(Button)findViewById(R.id.write_button);
        readStreamText=(TextView)findViewById(R.id.read_stream_textview);
        btDevicesList=new ArrayList<DeviceItem>();
        adapter=new DevicesListAdapter(this,btDevicesList);
        btDevices=new ArrayList<BluetoothDevice>();
        bluetoothDeviceFoundBroadcastReciever=new BluetoothDeviceFoundBroadcastReciever();
        Button readButton = (Button) findViewById(R.id.read_button);
        pairedListView=(ListView)findViewById(R.id.paired_devices_list);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        btChangedStateListener =new StateChangedBroadcastReciever();
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothConnectionService.readFromInputStream();
            }
        });
            attachAdapterToListView();
            disconnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isBtConnected){
                        bluetoothConnectionService.disconnectDevice();
                        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(readBroadcastReciever);
                    }
                }
            });
            writeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isBtConnected){
                        messages.setLength(0);
                        String writtenText=writeEditText.getText().toString();
                        bluetoothConnectionService.writeToOutputStream(writtenText.getBytes());
                    }
                }
            });

    }

    @Override
    protected void onStop() {
        unregisterReceiver(btChangedStateListener);
        super.onStop();
    }

    private class ReadMessagesBroadcastReciever extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action= intent.getAction();
            if(action.equals("message")){
                String theMessage = intent.getStringExtra("theMessage");
                messages.append(theMessage);
                readStreamText.setText(messages.toString());
                //Log.i(TAG,"message recieved is:"+theMessage);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Main Activity",messages.toString());
        if(!bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.enable();
        }
            IntentFilter intentFilter=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(btChangedStateListener,intentFilter);

    }

    private void attachAdapterToListView(){
        if(bluetoothAdapter!=null){
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if(pairedDevices.size()>0 && bluetoothAdapter.isEnabled()){
                for(BluetoothDevice bt: pairedDevices){
                    //for already paired devices
                    btDevices.add(bt);
                    adapter.add(new DeviceItem(bt.getName(),bt.getAddress(),false));
                }
                pairedListView.setAdapter(adapter);
                pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                        //TODO add connect functionality
                        if(bluetoothAdapter.isEnabled()){
                            BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(adapter.getItem(position).getBluetoothAddress());
                            if(remoteDevice!=null) {
                                Log.i(TAG, remoteDevice.getName());
                            bluetoothConnectionService=new BluetoothConnectionService(MainActivity.this,remoteDevice,"00001101-0000-1000-8000-00805F9B34FB",bluetoothAdapter);
                            bluetoothConnectionService.connectToDevice();
                            readBroadcastReciever=new ReadMessagesBroadcastReciever();
                            isBtConnected=true;
                                LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(readBroadcastReciever,new IntentFilter("message"));
                            }
                        }
                    }
                });
            }
            else{
                Toast.makeText(MainActivity.this,"No paired Devices found or bluetooth is disabled!",Toast.LENGTH_LONG).show();
            }

        }
        else{
            Toast.makeText(MainActivity.this,"Your bluetooth is not enabled",Toast.LENGTH_SHORT).show();
        }
    }

    private class BluetoothDeviceFoundBroadcastReciever extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice btDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                DeviceItem device=new DeviceItem(btDevice.getName(),btDevice.getAddress(),false);
                adapter.add(device);
                btDevices.add(btDevice);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private class StateChangedBroadcastReciever extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG,"Bluetooth is on");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"Bluetooth is off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG,"Bluetooth is turning off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"Bluetooth is turning on");
                        break;
                }

            }
        }
    }

}
