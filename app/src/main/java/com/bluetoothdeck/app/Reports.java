package com.bluetoothdeck.app;

/** Report IDs and payloads must match the composite HID descriptor. */
public final class Reports {
    private Reports() {}
    public static final byte[] DESCRIPTOR = hex(
        // Keyboard: modifier, reserved, six usages. LED output report is accepted.
        "05 01 09 06 A1 01 85 01 05 07 19 E0 29 E7 15 00 25 01 75 01 95 08 81 02 " +
        "95 01 75 08 81 01 95 05 75 01 05 08 19 01 29 05 91 02 95 01 75 03 91 01 " +
        "95 06 75 08 15 00 25 65 05 07 19 00 29 65 81 00 C0 " +
        // Mouse: 3 buttons, signed relative X/Y and wheel.
        "05 01 09 02 A1 01 85 02 09 01 A1 00 05 09 19 01 29 03 15 00 25 01 " +
        "95 03 75 01 81 02 95 01 75 05 81 01 05 01 09 30 09 31 09 38 " +
        "15 81 25 7F 75 08 95 03 81 06 C0 C0 " +
        // Gamepad: 16 buttons plus four signed axes, two unsigned triggers.
        "05 01 09 05 A1 01 85 03 05 09 19 01 29 10 15 00 25 01 75 01 95 10 81 02 " +
        "05 01 09 30 09 31 09 33 09 34 15 81 25 7F 75 08 95 04 81 02 " +
        "09 32 09 35 15 00 26 FF 00 75 08 95 02 81 02 C0");
    private static byte[] hex(String s) {
        String[] values = s.split(" "); byte[] out = new byte[values.length];
        for (int i=0;i<out.length;i++) out[i]=(byte)Integer.parseInt(values[i],16);
        return out;
    }
    public static byte[] keyboard(int modifier, int usage) {
        return new byte[]{(byte)modifier,0,(byte)usage,0,0,0,0,0};
    }
    public static byte[] mouse(int buttons,int x,int y,int wheel) {
        return new byte[]{(byte)(buttons&7),axis(x),axis(y),axis(wheel)};
    }
    public static byte[] gamepad(int buttons,int x,int y,int rx,int ry,int lt,int rt) {
        return new byte[]{(byte)buttons,(byte)(buttons>>8),axis(x),axis(y),axis(rx),axis(ry),
            (byte)Math.max(0,Math.min(255,lt)),(byte)Math.max(0,Math.min(255,rt))};
    }
    private static byte axis(int n) { return (byte)Math.max(-127,Math.min(127,n)); }
}
