package com.example.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    UUID uid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 其他设备 都是看出厂的地址


    ListView lv;

    //适配器
    BluetoothAdapter adapter;

    List<String> mList = new ArrayList<>();
    List<BluetoothDevice> devices = new ArrayList<>();

    ArrayAdapter<String> showAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = BluetoothAdapter.getDefaultAdapter();

        lv = (ListView) findViewById(R.id.lv);
        showAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mList);
        lv.setAdapter(showAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //去连接
                try {
                    BluetoothSocket socket = devices.get(i).createInsecureRfcommSocketToServiceRecord(uid);
                    if (socket == null) {
                        Toast.makeText(getBaseContext(), "连接失败，对方没有打开服务器", Toast.LENGTH_SHORT).show();
                    } else {
                        //不要掉了这一步
                        socket.connect();
                        Client client = new Client(socket);
                        //发送的就是 指令 协议
                        client.sendMessage("来之客户端的消息");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    public void open(View v) {
        if (adapter.isEnabled()) {
            adapter.disable();
        } else {
            //可以打开 但是不建议大家使用
            //不经过用户通过直接打开了
            //adapter.enable();
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    public void vis(View v) {
        //120秒
        startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600));
    }

    //慢慢扫
    public void scan(View v) {
        if (adapter.isEnabled()) {
            adapter.startDiscovery();
        }
    }

    public void start(View v) {
        //开启服务器
        // TCP  ip,port   mac地址(ip)  port（UUID）
        // UUID.randomUUID();
        new Server().start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mList.add(TextUtils.isEmpty(device.getName()) ? "匿名" : device.getName());
                devices.add(device);
                showAdapter.notifyDataSetChanged();
            }
        }
    };


    public class Server extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                BluetoothServerSocket socket = adapter.listenUsingInsecureRfcommWithServiceRecord("phone", uid);
                //堵塞
                BluetoothSocket client = socket.accept();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lv.setBackgroundColor(Color.BLUE);
                    }
                });
                //1对1，已经连上，没有必要再开服务器了
                new Client(client).start();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Client extends Thread {
        BluetoothSocket socket;

        public Client(BluetoothSocket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            try {
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            super.run();
            //堵塞
            final byte[] buf = new byte[1024];
            try {
                InputStream is = socket.getInputStream();
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    //一直在这里读取
                    final int finalLen = len;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, new String(buf, 0, finalLen), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
