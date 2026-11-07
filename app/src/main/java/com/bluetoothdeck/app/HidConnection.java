package com.bluetoothdeck.app;

import android.bluetooth.*;
import android.content.Context;
import java.util.function.Consumer;

@SuppressWarnings("MissingPermission")
public final class HidConnection implements BluetoothProfile.ServiceListener {
    private final Context context;
    private final Consumer<String> status;
    private final BluetoothAdapter adapter;
    private BluetoothHidDevice hid;
    private BluetoothDevice host;
    private boolean registered, closed;
    private final byte[][] last = {null,new byte[8],new byte[4],new byte[8]};
    public HidConnection(Context context, Consumer<String> status) {
        this.context=context; this.status=status;
        BluetoothManager manager=context.getSystemService(BluetoothManager.class);
        adapter=manager==null?null:manager.getAdapter();
    }
    public BluetoothAdapter adapter() { return adapter; }
    public void start() {
        if (adapter==null) { status.accept("This phone has no Bluetooth adapter"); return; }
        if (hid!=null || closed) return;
        if (!adapter.getProfileProxy(context,this,BluetoothProfile.HID_DEVICE))
            status.accept("Bluetooth HID profile unavailable on this phone");
    }
    @Override public void onServiceConnected(int profile,BluetoothProfile proxy) {
        if (closed) { adapter.closeProfileProxy(profile,proxy); return; }
        hid=(BluetoothHidDevice)proxy;
        boolean accepted=hid.registerApp(new BluetoothHidDeviceAppSdpSettings(
            "Bluetooth Deck","Keyboard, mouse and gamepad","Bluetooth Deck",
            (byte)0xC0,Reports.DESCRIPTOR),null,null,context.getMainExecutor(),new BluetoothHidDevice.Callback() {
                @Override public void onAppStatusChanged(BluetoothDevice device,boolean ready) {
                    registered=ready;
                    if (!ready) host=null;
                    status.accept(ready?"Ready — pair your PC, then choose Connect":"HID registration stopped. Keep this app open.");
                }
                @Override public void onConnectionStateChanged(BluetoothDevice device,int state) {
                    if (state==BluetoothProfile.STATE_CONNECTED) {
                        host=device; release(); status.accept("Connected: "+device.getName());
                    } else if (state==BluetoothProfile.STATE_DISCONNECTED) {
                        host=null; status.accept("Disconnected — choose Connect to retry");
                    } else status.accept("Connecting…");
                }
                @Override public void onGetReport(BluetoothDevice device,byte type,byte id,int size) {
                    if(type==BluetoothHidDevice.REPORT_TYPE_INPUT && id>=1 && id<=3)
                        hid.replyReport(device,type,id,last[id]);
                    else hid.reportError(device,BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                }
                @Override public void onSetReport(BluetoothDevice device,byte type,byte id,byte[] data) {
                    // Keyboard LEDs are acknowledged; this descriptor does not advertise rumble.
                    hid.reportError(device,(type==BluetoothHidDevice.REPORT_TYPE_OUTPUT && id==1)
                        ?BluetoothHidDevice.ERROR_RSP_SUCCESS:BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                }
            });
        if (!accepted) status.accept("HID registration failed. Close other Bluetooth keyboard apps and retry.");
    }
    @Override public void onServiceDisconnected(int profile) {
        hid=null; host=null; registered=false; status.accept("Bluetooth service unavailable");
    }
    public void connect(BluetoothDevice device) {
        if(hid==null || !registered) { status.accept("HID not ready. Keep app open and retry."); return; }
        if(host!=null && !host.equals(device)) { status.accept("Disconnect the current PC first"); return; }
        if(!hid.connect(device)) status.accept("Connection rejected. Pair with the PC and retry.");
    }
    public void send(int id,byte[] data) {
        // Relative movement is transient and must not be replayed on GET_REPORT.
        last[id]=id==2?Reports.mouse(data[0],0,0,0):data.clone();
        if(hid!=null && host!=null && !hid.sendReport(host,id,data)) status.accept("Input could not be sent");
    }
    public void release() { send(1,new byte[8]); send(2,new byte[4]); send(3,new byte[8]); }
    public void disconnect() { release(); if(hid!=null && host!=null) hid.disconnect(host); }
    public void close() {
        release(); closed=true;
        if(hid!=null) { hid.unregisterApp(); adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid); }
        hid=null; host=null;
    }
}
