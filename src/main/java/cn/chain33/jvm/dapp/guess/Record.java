package cn.chain33.jvm.dapp.guess;

import java.util.LinkedHashMap;

import com.fuzamei.chain33.Blockchain;
import com.google.gson.Gson;
import com.fuzamei.chain33.LocalDB;
import cn.chain33.jvm.interfaces.Storage;

public class Record implements Storage  {
    private String address;
    //(round->(guessNumber->ticketNumber))
    private LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>> guessRecord;
    //(round->bonus)
    private LinkedHashMap<Integer,Long> prizeRecord;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LinkedHashMap<Integer, LinkedHashMap<Integer, Integer>> getGuessRecord() {
        return guessRecord;
    }

    public void setGuessRecord(LinkedHashMap<Integer, LinkedHashMap<Integer, Integer>> guessRecord) {
        this.guessRecord = guessRecord;
    }

    public LinkedHashMap<Integer, Long> getPrizeRecord() {
        return prizeRecord;
    }

    public void setPrizeRecord(LinkedHashMap<Integer, Long> prizeRecord) {
        this.prizeRecord = prizeRecord;
    }

    public Record loadData(){
      byte[] values = LocalDB.getFromLocal(Blockchain.getFrom().getBytes());
      if (values==null){
          Record record =new Record();
          record.setAddress(Blockchain.getFrom());
          record.guessRecord = new LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>>();
          record.prizeRecord = new LinkedHashMap<Integer,Long>();
          return  record;
      }
      Gson gson = new Gson();
      Record record = gson.fromJson(values.toString(),new Record().getClass());
      return record;
  }
    public Record loadData(String from){
        byte[] values = LocalDB.getFromLocal(from.getBytes());
        if (values==null){
            Record record =new Record();
            record.setAddress(from);
            record.guessRecord = new LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>>();
            record.prizeRecord = new LinkedHashMap<Integer,Long>();
            return  record;
        }
        Gson gson = new Gson();
        Record record = gson.fromJson(values.toString(),new Record().getClass());
        return record;
    }

  public boolean saveData(){
      Gson gson = new Gson();
      String jsonStr=gson.toJson(this);
      return LocalDB.setLocal(this.address.getBytes(),jsonStr.getBytes());
  }
}
