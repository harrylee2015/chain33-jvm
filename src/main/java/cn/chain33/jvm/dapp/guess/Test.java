package cn.chain33.jvm.dapp.guess;

import com.google.gson.Gson;

import java.util.LinkedHashMap;

public class Test {
    private LinkedHashMap<Integer, Integer> data=new LinkedHashMap<Integer,Integer>();

    public boolean saveData() {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(this);
        System.out.println(jsonStr);
        return true;
    }

    public static void main(String[] args){
         Test test = new Test();
         test.data.put(1,1);
         test.data.put(2,2);
         test.data.put(2000,2000);
         test.saveData();
    }
}


