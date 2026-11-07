package com.bluetoothdeck.app;

/** Calibrated screen-normal twist from Android rotation-vector quaternions, no Android dependencies. */
public final class Steering {
    private double bx,by,bz,bw;
    private boolean calibrated;
    private double filtered;
    private int fullLock=35;
    public void reset() { calibrated=false; filtered=0; }
    public boolean isCalibrated() { return calibrated; }
    public void setFullLock(int degrees) { fullLock=Math.max(15,Math.min(75,degrees)); }
    public int update(double x,double y,double z,double w) {
        double norm=Math.sqrt(x*x+y*y+z*z+w*w);
        if(!Double.isFinite(norm) || norm<1e-8) { reset(); return 0; }
        x/=norm;y/=norm;z/=norm;w/=norm;
        if(!calibrated) { bx=x;by=y;bz=z;bw=w;calibrated=true;return 0; }
        // conjugate(baseline) * current, then twist about baseline device Z.
        double relativeZ=bw*z-bx*y+by*x-bz*w;
        double relativeW=bw*w+bx*x+by*y+bz*z;
        if(Math.hypot(relativeZ,relativeW)<1e-8) { filtered=0;return 0; }
        double degrees=Math.toDegrees(2*Math.atan2(relativeZ,relativeW));
        degrees=((degrees+540)%360)-180;
        double target=mapDegrees(degrees,fullLock);
        filtered += .3*(target-filtered);
        // Snap small residuals to neutral after the wheel returns to its dead zone.
        if(target==0 && Math.abs(filtered)<1) filtered=0;
        return (int)Math.round(filtered);
    }
    public static int mapDegrees(double degrees,int fullLock) {
        if(!Double.isFinite(degrees)) return 0;
        double magnitude=Math.abs(degrees);
        if(magnitude<=2) return 0;
        double scale=Math.max(15,Math.min(75,fullLock))-2;
        // Clockwise wheel turn -> positive HID X (right).
        return (int)Math.round(-Math.signum(degrees)*Math.min(1,(magnitude-2)/scale)*127);
    }
}
