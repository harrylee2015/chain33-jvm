package cn.chain33.jvm.userlib;

import junit.framework.TestCase;
import junit.framework.Assert;
import cn.chain33.jvm.userlib.Chain33Map;


public class Chain33MapTest extends TestCase {
//    Chain33Map<String,Integer> chain33Map = new Chain33Map<String,Integer>();
    private static int entryCount = 48;
    public void putTest() {
        Chain33Map<Integer, String> map = new Chain33Map<>();
        int count = 5;
        for (int i = 0; i < count; i++) {
            int key = createCollisionKey(i);
            Assert.assertNull(map.put(key, String.valueOf(i)));
            Assert.assertEquals(i + 1, map.size());
            Assert.assertEquals(String.valueOf(i), map.get(key));
        }

        //rewrite
        for (int i = 0; i < count; i++) {
            int key = createCollisionKey(i);
            Assert.assertEquals(String.valueOf(i), map.put(key, String.valueOf(key)));
            Assert.assertEquals(count, map.size());
            Assert.assertEquals(String.valueOf(key), map.get(key));
        }
    }
    private int createCollisionKey(int val) {
        return val * 16;
    }
}
