package cn.chain33.jvm.dapp.guess;

import cn.chain33.jvm.interfaces.Storage;
import com.fuzamei.chain33.Account;
import com.fuzamei.chain33.Blockchain;
import com.fuzamei.chain33.LocalDB;
import com.fuzamei.chain33.StateDB;
import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.Map;

public class Guess implements Storage {
    private static final Guess INSTANCE = new Guess();

    public static final Guess getInstance(Integer... args) {
        if (args.length == 0) {
            return INSTANCE.loadData();
        } else {
            return INSTANCE.loadData(args[0]);
        }
    }

    public static final String LastRound = "LastRound";
    public static final long TicketPrice = 100000000;
    private long startHeight;
    private long endHeight;
    //记录当轮游戏信息
    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> data;
    private String admin;
    //状态 0开始
    private Boolean isClosed;
    //中奖数字
    private Integer luckyNum;
    //当前游戏轮次
    private Integer round;
    //奖池
    private long bonusPool;
    //遗留奖金
    private long legacyBonus;


    public Guess loadData() {
        byte[] bytes = LocalDB.getFromLocal(LastRound.getBytes());
        if (bytes != null) {
            byte[] data = StateDB.getFromState(bytes.toString().getBytes());
            if (data != null) {
                Gson gson = new Gson();
                Guess guess = gson.fromJson(data.toString(), new Guess().getClass());
                if (guess.isClosed) {
                    Guess nextRound = new Guess();
                    nextRound.admin = guess.admin;
                    nextRound.startHeight = Blockchain.getCurrentHeight();
                    nextRound.round = guess.round + 1;
                    nextRound.data = new LinkedHashMap<Integer, LinkedHashMap<String, Integer>>();
                    nextRound.bonusPool = guess.legacyBonus;
                    return nextRound;
                }
                return guess;
            } else {
                Blockchain.stopTransWithErrInfo("not found last round game info! round:" + bytes.toString());
                throw new IllegalStateException("not found last round game info! round:" + bytes.toString());
            }
        }
        return null;
    }

    /**
     * 查询接口加载数据专用
     *
     * @param round
     * @return
     */
    public Guess loadData(Integer round) {
        byte[] data = StateDB.getFromState(round.toString().getBytes());
        if (data != null) {
            Gson gson = new Gson();
            Guess guess = gson.fromJson(data.toString(), new Guess().getClass());
            return guess;
        }
        Blockchain.stopTransWithErrInfo("not found last round game info! round:" + round);
        throw new IllegalStateException("not found last round game info! round:" + round);
    }

    public boolean saveData() {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(this);
        //TODO 数据库操作没有事务处理机制，默认就是执行成功吧
        //状态数据放在stateDB中
        StateDB.setState(this.round.toString().getBytes(), jsonStr.getBytes());
        //LastRound,索引信息 放在localDB中
        Boolean flag = LocalDB.setLocal(LastRound.getBytes(), this.round.toString().getBytes());
        return flag;
    }

    //只需启动一次
    public boolean startGame() {
        Guess guess = loadData();
        if (guess == null) {
            Guess newGuess = new Guess();
            newGuess.admin = Blockchain.getFrom();
            newGuess.startHeight = Blockchain.getCurrentHeight();
            newGuess.round = 1;
            newGuess.data = new LinkedHashMap<Integer, LinkedHashMap<String, Integer>>();
            return newGuess.saveData();
        }

        return true;
    }

    public void playGame(Integer lucky, Integer ticketNum) {
        Guess guess = loadData();
        if (guess == null) {
            Blockchain.stopTransWithErrInfo("the game hasn't started yet!");
            throw new IllegalStateException("the game hasn't started yet!");
        }
        LinkedHashMap<String, Integer> recordMap = guess.data.get(lucky);
        long amount = ticketNum.longValue() * TicketPrice;
        String from = Blockchain.getFrom();
        if (Account.execTransfer(from, guess.admin, amount)) {
            if (Account.execFrozen(guess.admin, amount)) {
                guess.bonusPool += amount;
                Integer value = recordMap.get(from);
                if (value != null) {
                    value += ticketNum;
                    recordMap.put(from, value);
                } else {
                    recordMap.put(from, ticketNum);
                }
                guess.saveData();
                //TODO  这里能不能换成静态函数,加入对象？index
                Record prevRecord = Record.getInstance(from);
                LinkedHashMap<Integer, LinkedHashMap<Integer, Integer>> guessRecord = prevRecord.getGuessRecord();
                LinkedHashMap<Integer, Integer> records = guessRecord.get(guess.round);
                if (records == null) {
                    LinkedHashMap<Integer, Integer> newRecords = new LinkedHashMap<Integer, Integer>();
                    newRecords.put(lucky, ticketNum);
                    guessRecord.put(guess.round, newRecords);
                    prevRecord.setGuessRecord(guessRecord);
                    prevRecord.saveData();
                } else {
                    Integer count = records.get(lucky);
                    if (count != null) {
                        count += ticketNum;
                        records.put(lucky, count);
                    } else {
                        records.put(lucky, ticketNum);
                    }
                    guessRecord.put(guess.round, records);
                    prevRecord.setGuessRecord(guessRecord);
                    prevRecord.saveData();
                }
                return;
            }
        }
        Blockchain.stopTransWithErrInfo("lack of balance! need amount:" + amount);
        throw new IllegalStateException("lack of balance! need amount:" + amount);
    }

    public void closeGame() {
        Guess guess = loadData();
        if (guess == null) {
            Blockchain.stopTransWithErrInfo("the game hasn't started yet!");
            throw new IllegalStateException("the game hasn't started yet!");
        }
        long blockHeight = Blockchain.getCurrentHeight();
        if (blockHeight - guess.startHeight <= 10) {
            Blockchain.stopTransWithErrInfo("you have to wait for 10 block height!");
            throw new IllegalStateException("you have to wait for 10 block height!");
        }
        // 0~9,取余
        Integer luckyNum = Integer.valueOf(Blockchain.getRandom().getBytes().length % 10);
        guess.luckyNum = luckyNum;
        LinkedHashMap<String, Integer> luckyMap = guess.data.get(luckyNum);
        if (luckyMap == null) {
            guess.legacyBonus = guess.bonusPool;
            guess.endHeight = blockHeight;
            guess.isClosed = true;
            guess.saveData();
            return;
        }
        long count = 0;
        for (Map.Entry<String, Integer> entry : luckyMap.entrySet()) {
            count += entry.getValue().longValue();
        }
        for (Map.Entry<String, Integer> entry : luckyMap.entrySet()) {
            // 60%用于平分， 35%滚动下一轮， 5%平台手续费
            long bonus = guess.bonusPool * 6 / 10 * entry.getValue().longValue() / count;

            if (Account.execActive(guess.admin, bonus)) {
                if (Account.execTransfer(guess.admin, entry.getKey(), bonus)) {
                    //TODO生成 index
                    Record prevRecord = Record.getInstance(entry.getKey());
                    LinkedHashMap<Integer, Long> bonusRecord = prevRecord.getPrizeRecord();
                    bonusRecord.put(guess.round, Long.valueOf(bonus));
                    prevRecord.setPrizeRecord(bonusRecord);
                    prevRecord.saveData();
                    continue;
                }
            }
            Blockchain.stopTransWithErrInfo("The frozen bonus is insufficient!");
            throw new IllegalStateException("The frozen bonus is insufficient!");
        }

        long fee = guess.bonusPool * 5 / 100;
        Account.execActive(guess.admin, fee);
        guess.legacyBonus = guess.bonusPool * 35 / 100;
        guess.endHeight = blockHeight;
        guess.isClosed = true;
        guess.saveData();

    }

    //TODO 调用方式待定

    /**
     * 入口函数，启动args[0]表示调用方法，后面携带参数
     *
     * @param args
     */
    public static void main(String[] args) {
        Guess guess = new Guess();
        switch (args[0]) {
            case "startGame":
                guess.startGame();
                break;
            case "playGame":
                if (args.length != 3) {
                    Blockchain.stopTransWithErrInfo("insufficient paramenters!");
                    return;
                }
                guess.playGame(Integer.valueOf(args[1]), Integer.valueOf(args[2]));
                break;
            case "closeGame":
                guess.closeGame();
                break;
            default:
                throw new IllegalStateException("Unknown funcName: " + args[0]);
        }
    }

}
