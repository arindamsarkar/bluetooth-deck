import com.bluetoothdeck.app.Steering;
public class SteeringTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static int twist(Steering s,double degrees){double half=Math.toRadians(degrees)/2;return s.update(0,0,Math.sin(half),Math.cos(half));}
    public static void main(String[] args){
        check(Steering.mapDegrees(0,35)==0,"neutral");
        check(Steering.mapDegrees(2,35)==0 && Steering.mapDegrees(-2,35)==0,"dead zone");
        check(Steering.mapDegrees(-35,35)==127 && Steering.mapDegrees(35,35)==-127,"direction and full lock");
        check(Steering.mapDegrees(-90,35)==127,"clamp");
        check(Math.abs(Steering.mapDegrees(15,20))>Math.abs(Steering.mapDegrees(15,60)),"sensitivity");
        Steering s=new Steering();check(twist(s,20)==0 && s.isCalibrated(),"initial auto calibration");
        int axis=0;for(int i=0;i<35;i++)axis=twist(s,-15);check(axis==127,"relative calibration");
        for(int i=0;i<35;i++)axis=twist(s,20);check(axis==0,"return to neutral");
        s.reset();check(!s.isCalibrated() && twist(s,-55)==0,"recalibration");
        s.reset();twist(s,179);for(int i=0;i<35;i++)axis=twist(s,-179);check(axis==0,"angle wrap");
        s.reset();s.update(0,0,0,1);check(s.update(0,0,0,-1)==0,"quaternion sign invariance");
        s.reset();check(s.update(Double.NaN,0,0,1)==0 && !s.isCalibrated(),"bad sensor input");
        // A rotated baseline (e.g. landscape/natural landscape tablet) has the same device-Z steering.
        s.reset();double b=Math.sqrt(.5);s.update(0,0,b,b);
        for(int i=0;i<35;i++)axis=twist(s,55);check(axis==127,"landscape baseline");
        // Arbitrarily pitched baseline; baseline * z-twist must still steer correctly.
        s.reset();s.update(b,0,0,b);double h=Math.toRadians(-35)/2;
        for(int i=0;i<35;i++)axis=s.update(b*Math.cos(h),-b*Math.sin(h),b*Math.sin(h),b*Math.cos(h));
        check(axis==127,"pitched baseline");
        System.out.println("PASS: gyro steering calibration, direction, sensitivity, dead zone, smoothing, angle wrap and invalid input");
    }
}
