import com.bluetoothdeck.app.Reports;
import java.util.*;
public class ReportsTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        check(Arrays.equals(Reports.keyboard(2,4),new byte[]{2,0,4,0,0,0,0,0}));
        check(Arrays.equals(Reports.mouse(255,-300,300,1),new byte[]{7,-127,127,1}));
        check(Arrays.equals(Reports.gamepad(0x8001,-128,128,0,0,-1,300),new byte[]{1,-128,-127,127,0,0,0,-1}));
        // Parse HID short items and verify each report's input/output bit count.
        int size=0,count=0,id=0,collections=0; int[] input=new int[4],output=new int[4];
        byte[] d=Reports.DESCRIPTOR;
        for(int i=0;i<d.length;){
            int prefix=d[i++]&255;int n=prefix&3;if(n==3)n=4;int value=0;
            check(i+n<=d.length);for(int j=0;j<n;j++)value|=(d[i++]&255)<<(8*j);
            switch(prefix&0xfc){
                case 0x74:size=value;break;case 0x94:count=value;break;case 0x84:id=value;check(id>=1&&id<=3);break;
                case 0x80:input[id]+=size*count;break;case 0x90:output[id]+=size*count;break;
                case 0xa0:collections++;break;case 0xc0:collections--;check(collections>=0);break;
            }
        }
        check(collections==0);check(Arrays.equals(input,new int[]{0,64,32,64}));check(output[1]==8);
        System.out.println("PASS: report encodings, clamping, descriptor lengths and collection balance");
    }
}
