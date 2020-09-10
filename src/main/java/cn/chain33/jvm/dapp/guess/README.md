# java 开发的guess游戏合约

## 说明

   - [Guess.java]实现了tx接口,提供startGame(),palyGame(),closeGame()等方法
   
   - [Record.java]实现查询接口,提供查询个人中奖信息,个人投注信息查询
   
   - [Storage数据存储加载接口]Guess和Record类都各自实现了Storage接口
     
      1.loadData() 从区块链中加载数据
   
      2.saveData() 把当前数据保存到链上
   
 

