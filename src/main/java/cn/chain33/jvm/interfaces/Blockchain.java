package cn.chain33.jvm.interfaces;

public interface Blockchain {
    //获取随机字符串
    public  String getRandomString();
    // 获取当前交易发送者地址
    public  String getFrom();
    // 获取当前区块高度
    public long getCurrentHeight();
    // 获取当前区块hash
    public String getCurrentBlockHash();
    // 获取当前交易hash
    public String getTxHash();
    // 获取随机数
    public int getRandomNumber();
    // 加载合约数据
    public Object loadData();
    // 保存执行后的数据
    public void  saveData();
}
