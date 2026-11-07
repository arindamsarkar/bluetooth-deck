package com.bluetoothdeck.app;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.hardware.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import java.util.*;

@SuppressWarnings("MissingPermission")
public class MainActivity extends Activity implements SensorEventListener {
    private HidConnection connection;
    private TextView status;
    private LinearLayout root, body;
    private Vibrator vibrator;
    private SensorManager sensors;
    private Sensor rotationSensor;
    private final Steering steering=new Steering();
    private boolean gyroEnabled, sensorActive, gamepadMode;
    private int gyroX;
    private long lastGyroSend;
    private Switch gyroSwitch;
    private TextView gyroStatus;
    private final float[] quaternion=new float[4];
    private boolean haptics=true, shift=false, ctrl=false;
    private int strength=130, buttons, lx,ly,rx,ry,lt,rt;
    
    private static final int BLUE=Color.rgb(104,171,255), BG=Color.rgb(15,20,31);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
        if(sensors!=null && sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!=null) {
            rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            if(rotationSensor==null) rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(20,20,20,12); root.setBackgroundColor(BG);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(20+bars.left,16+bars.top,20+bars.right,12+bars.bottom);
            } else root.setPadding(20,insets.getSystemWindowInsetTop()+16,20,insets.getSystemWindowInsetBottom()+12);
            return insets;
        });
        text(root,"BLUETOOTH DECK",25); status=text(root,"Grant Bluetooth access to begin",14);
        LinearLayout links=row(root);
        button(links,"Setup",this::setup); button(links,"Pair PC",this::pair); button(links,"Connect",this::chooseHost);
        button(links,"Disconnect",()->{ if(connection!=null)connection.disconnect(); });
        LinearLayout tabs=row(root);
        button(tabs,"Keyboard",()->show("Keyboard")); button(tabs,"Mouse",()->show("Mouse")); button(tabs,"Gamepad",()->show("Gamepad"));
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        body=new LinearLayout(this); body.setOrientation(1); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Switch feedback=new Switch(this); feedback.setText("Touch haptics (local vibration)"); feedback.setTextColor(Color.WHITE); feedback.setChecked(true);
        feedback.setOnCheckedChangeListener((v,checked)->haptics=checked); root.addView(feedback);
        SeekBar intensity=new SeekBar(this); intensity.setMax(254); intensity.setProgress(129);
        intensity.setContentDescription("Haptic intensity");
        intensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){strength=p+1;}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){buzz(45);}
        }); root.addView(intensity);
        show("Gamepad"); setup();
    }
    private TextView text(LinearLayout parent,String value,int size) {
        TextView t=new TextView(this); t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(4,6,4,6);parent.addView(t);return t;
    }
    private LinearLayout row(LinearLayout parent) { LinearLayout r=new LinearLayout(this);r.setOrientation(0); parent.addView(r);return r; }
    private Button button(LinearLayout parent,String title,Runnable action) {
        Button b=new Button(this); b.setText(title); b.setTextSize(12); b.setAllCaps(false);
        parent.addView(b,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); b.setOnClickListener(v->action.run());return b;
    }
    private void say(String s){ status.setText(s); }
    private boolean permissions() {
        return Build.VERSION.SDK_INT<31 || (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)==PackageManager.PERMISSION_GRANTED);
    }
    private void setup() {
        if(!permissions()) { requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE},1); return; }
        if(connection==null)connection=new HidConnection(this,this::say);
        if(connection.adapter()==null){say("Bluetooth unavailable");return;}
        if(!connection.adapter().isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return;}
        connection.start();
    }
    @Override public void onRequestPermissionsResult(int request,String[] p,int[] results){super.onRequestPermissionsResult(request,p,results);if(permissions())setup();else say("Bluetooth access denied. Enable Nearby devices in app settings.");}
    private void pair() {
        if(!permissions() || connection==null){setup();return;}
        if(!connection.adapter().isEnabled()){setup();return;}
        Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120);startActivity(i);
        say("On PC: Bluetooth settings → Add device → this phone. Confirm the pairing code.");
    }
    private void chooseHost() {
        if(!permissions() || connection==null){setup();return;}
        Set<BluetoothDevice> paired=connection.adapter().getBondedDevices();
        if(paired.isEmpty()){say("Pair with your PC first");return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(paired);String[] names=new String[devices.size()];
        for(int i=0;i<names.length;i++)names[i]=devices.get(i).getName()+"\n"+devices.get(i).getAddress();
        new AlertDialog.Builder(this).setTitle("Choose your paired PC").setItems(names,(d,n)->connection.connect(devices.get(n))).show();
    }
    private void buzz(int ms) {
        if(haptics && vibrator!=null && vibrator.hasVibrator())
            vibrator.vibrate(VibrationEffect.createOneShot(ms,vibrator.hasAmplitudeControl()?strength:VibrationEffect.DEFAULT_AMPLITUDE));
    }
    private void send(int id,byte[] report){if(connection!=null)connection.send(id,report);}
    private void reset() {
        
        buttons=lx=ly=rx=ry=lt=rt=0;shift=ctrl=false;
        if(connection!=null)connection.release();
    }
    private void show(String mode) {
        stopGyro(); gyroEnabled=false; gyroSwitch=null; gyroStatus=null;
        gamepadMode=mode.equals("Gamepad"); reset();body.removeAllViews();
        if(mode.equals("Gamepad")) {
            text(body,"GENERIC HID GAMEPAD • Xbox mode requires a future PC companion",12);
            text(body,"Landscape recommended • map controls in your game",12);
            gyroSwitch=new Switch(this); gyroSwitch.setText("Gyro steering OFF"); gyroSwitch.setTextColor(Color.WHITE);
            gyroSwitch.setEnabled(rotationSensor!=null); body.addView(gyroSwitch);
            gyroStatus=text(body,rotationSensor==null?"Gyro unavailable on this phone":"OFF — left joystick controls steering",12);
            gyroSwitch.setOnCheckedChangeListener((v,enabled)->{
                gyroEnabled=enabled; lx=0;
                if(enabled) startGyro(); else stopGyro();
                gyroSwitch.setText(gyroEnabled?"Gyro steering ON":"Gyro steering OFF");
                if(!gyroEnabled) gyroStatus.setText("OFF — left joystick controls steering");
                pad();
            });
            LinearLayout gyroOptions=row(body);
            button(gyroOptions,"Calibrate",()->{ if(gyroEnabled){steering.reset();gyroX=0;pad();gyroStatus.setText("Hold phone centered…");} });
            TextView lockLabel=text(body,"Steering sensitivity: full lock at 35°",12);
            SeekBar lock=new SeekBar(this);lock.setMax(60);lock.setProgress(20);lock.setContentDescription("Full steering lock angle");body.addView(lock);
            lock.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar bar,int value,boolean user){steering.setFullLock(value+15);lockLabel.setText("Steering sensitivity: full lock at "+(value+15)+"°");}
                public void onStartTrackingTouch(SeekBar bar){} public void onStopTrackingTouch(SeekBar bar){}
            });
            steering.setFullLock(35);
            LinearLayout shoulders=row(body);
            hold(shoulders,"L1",4); trigger(shoulders,"L2",true);trigger(shoulders,"R2",false);hold(shoulders,"R1",5);
            LinearLayout sticks=row(body);
            sticks.addView(new Stick(false),new LinearLayout.LayoutParams(0,dp(150),1));
            sticks.addView(new Stick(true),new LinearLayout.LayoutParams(0,dp(150),1));
            LinearLayout face=row(body); hold(face,"A",0);hold(face,"B",1);hold(face,"X",2);hold(face,"Y",3);
            LinearLayout dpad=row(body);hold(dpad,"↑",10);hold(dpad,"↓",11);hold(dpad,"←",12);hold(dpad,"→",13);
            LinearLayout menu=row(body);hold(menu,"Select",6);hold(menu,"Start",7);hold(menu,"L3",8);hold(menu,"R3",9);
        } else if(mode.equals("Mouse")) {
            text(body,"Slide to move • tap to click • two fingers to scroll",14);
            View pad=new View(this);pad.setMinimumHeight(dp(180));pad.setBackgroundColor(Color.rgb(30,43,62));
            body.addView(pad,new LinearLayout.LayoutParams(-1,0,1));
            pad.setOnTouchListener(new View.OnTouchListener(){
                float x,y,startX,startY;long began;boolean scrolling;
                public boolean onTouch(View v,MotionEvent e){
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch(e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:x=startX=e.getX();y=startY=e.getY();began=SystemClock.uptimeMillis();scrolling=false;break;
                        case MotionEvent.ACTION_POINTER_DOWN:scrolling=true;y=e.getY();break;
                        case MotionEvent.ACTION_MOVE:
                            if(e.getPointerCount()>1){int wheel=(int)((y-e.getY())/12);if(wheel!=0){send(2,Reports.mouse(0,0,0,wheel));y=e.getY();}}
                            else {send(2,Reports.mouse(0,(int)(e.getX()-x),(int)(e.getY()-y),0));x=e.getX();y=e.getY();}break;
                        case MotionEvent.ACTION_UP:
                            if(!scrolling && SystemClock.uptimeMillis()-began<220 && Math.hypot(e.getX()-startX,e.getY()-startY)<dp(10)){clickMouse(1);v.performClick();}break;
                        case MotionEvent.ACTION_CANCEL:send(2,new byte[4]);break;
                    }return true;
                }
            });
            LinearLayout clicks=row(body);button(clicks,"Left click",()->clickMouse(1));button(clicks,"Right click",()->clickMouse(2));
            button(clicks,"Scroll ↑",()->send(2,Reports.mouse(0,0,0,1)));button(clicks,"Scroll ↓",()->send(2,Reports.mouse(0,0,0,-1)));
        } else {
            text(body,"US keyboard layout • tap keys • Shift / Ctrl toggle",14);
            LinearLayout mods=row(body);
            Button sh=button(mods,"Shift OFF",()->{});sh.setOnClickListener(v->{shift=!shift;sh.setText(shift?"Shift ON":"Shift OFF");});
            Button ct=button(mods,"Ctrl OFF",()->{});ct.setOnClickListener(v->{ctrl=!ctrl;ct.setText(ctrl?"Ctrl ON":"Ctrl OFF");});
            String[] rows={"1234567890","qwertyuiop","asdfghjkl","zxcvbnm"};
            for(String keys:rows){LinearLayout r=row(body);for(char c:keys.toCharArray())button(r,String.valueOf(c),()->key(usage(c)));}
            LinearLayout nav=row(body);button(nav,"Esc",()->key(41));button(nav,"Tab",()->key(43));button(nav,"⌫",()->key(42));button(nav,"Enter",()->key(40));
            LinearLayout arrows=row(body);button(arrows,"←",()->key(80));button(arrows,"↑",()->key(82));button(arrows,"↓",()->key(81));button(arrows,"→",()->key(79));button(arrows,"Space",()->key(44));
        }
    }
    private int usage(char c){return c>='a'&&c<='z'?4+c-'a':c=='0'?39:30+c-'1';}
    private void key(int usage){send(1,Reports.keyboard((shift?2:0)|(ctrl?1:0),usage));send(1,new byte[8]);buzz(12);}
    private void clickMouse(int b){send(2,Reports.mouse(b,0,0,0));send(2,new byte[4]);buzz(15);}
    private void pad(){send(3,Reports.gamepad(buttons,gyroEnabled?gyroX:lx,ly,rx,ry,lt,rt));}
    private void hold(LinearLayout parent,String label,int bit){
        Button b=button(parent,label,()->{});b.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){buttons|=1<<bit;pad();buzz(22);v.setPressed(true);return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL){buttons&=~(1<<bit);pad();v.setPressed(false);if(e.getActionMasked()==MotionEvent.ACTION_UP)v.performClick();return true;}return true;
        });
    }
    private void trigger(LinearLayout parent,String label,boolean left){
        Button b=button(parent,label,()->{});b.setOnTouchListener((v,e)->{
            int value=(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL)?0:255;
            if(left)lt=value;else rt=value;pad();v.setPressed(value>0);if(e.getActionMasked()==MotionEvent.ACTION_DOWN)buzz(35);return true;
        });
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private class Stick extends View {
        final boolean right;final Paint p=new Paint(3);float px,py;int pointer=-1;
        Stick(boolean right){super(MainActivity.this);this.right=right;setContentDescription(right?"Right joystick":"Left joystick");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float radius=Math.min(getWidth(),getHeight())*.39f,cx=getWidth()/2f,cy=getHeight()/2f;
            p.setColor(Color.rgb(36,52,75));c.drawCircle(cx,cy,radius,p);p.setColor(BLUE);c.drawCircle(cx+px*radius,cy+py*radius,radius*.32f,p);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){pointer=e.getPointerId(0);getParent().requestDisallowInterceptTouchEvent(true);buzz(12);}
            if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_UP || (action==MotionEvent.ACTION_POINTER_UP && e.getPointerId(e.getActionIndex())==pointer)){
                pointer=-1;px=py=0;
            }else {int index=e.findPointerIndex(pointer);if(index<0)return true;
                float radius=Math.max(1,Math.min(getWidth(),getHeight())*.39f);px=(e.getX(index)-getWidth()/2f)/radius;py=(e.getY(index)-getHeight()/2f)/radius;
                float length=(float)Math.hypot(px,py);if(length>1){px/=length;py/=length;}if(length<.12f){px=py=0;}
            }
            if(right){rx=(int)(px*127);ry=(int)(py*127);}else{lx=gyroEnabled?0:(int)(px*127);ly=(int)(py*127);}pad();invalidate();return true;
        }
    }
    private void startGyro() {
        steering.reset();gyroX=0;lastGyroSend=0;
        sensorActive=sensors!=null && rotationSensor!=null && sensors.registerListener(this,rotationSensor,20_000);
        if(!sensorActive) {
            gyroEnabled=false;
            if(gyroSwitch!=null) gyroSwitch.setChecked(false);
            say("Gyro could not start on this phone");
        } else if(gyroStatus!=null) gyroStatus.setText("Hold phone centered — calibrating…");
    }
    private void stopGyro() {
        if(sensors!=null) sensors.unregisterListener(this);
        sensorActive=false;gyroX=0;steering.reset();
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if(!sensorActive || !gyroEnabled || !gamepadMode) return;
        // Limit sensor processing/report output even if another app requests faster sampling.
        if(lastGyroSend!=0 && event.timestamp-lastGyroSend<20_000_000L) return;
        lastGyroSend=event.timestamp;
        SensorManager.getQuaternionFromVector(quaternion,event.values);
        gyroX=steering.update(quaternion[1],quaternion[2],quaternion[3],quaternion[0]);
        if(gyroStatus!=null) gyroStatus.setText("Tilt like a steering wheel • steering "+gyroX+" / 127");
        pad();
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) {}
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if(gyroEnabled){steering.reset();gyroX=0;pad();}
    }
    @Override protected void onResume(){super.onResume();if(gyroEnabled && gamepadMode)startGyro();if(connection!=null && permissions() && connection.adapter()!=null && connection.adapter().isEnabled())connection.start();}
    @Override protected void onPause(){stopGyro();reset();if(vibrator!=null)vibrator.cancel();super.onPause();}
    @Override protected void onDestroy(){if(connection!=null && permissions())connection.close();super.onDestroy();}
}
