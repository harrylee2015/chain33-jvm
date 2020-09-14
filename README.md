# chain33-jvm 

说明：这是一个在chain33中使用java进行智能合约开发的实验性项目

## 可用的java原生类型工具说明
  *  随机性质的函数禁用,如rand,time等包都不能用。
  
  * map存储使用LinkedHashMap,保证顺序写，顺序读。
     ```
    import java.util.LinkedHashMap;
    import java.util.Map;
    ```
  * 
## 关于序列化和反序列化
  
  * 要保证结果一致性，不能出现字段位置出现偏差，存储时状态hash在不同的节点上会不一致。
  
  * 建议使用第三方包Gson进行序列化和反序列，当然也可以自己按自己的规则实现，存储字节可能更小。
    ```
    import com.google.gson.Gson;
    ```
## 交易执行流程

   1. 交易过来,先解析交易,获取合约名,调用方法及参数
   2. exector模块启动jvm,加载合约相应的jar包,调用合约中相应的方法
   
   - [x]如何调用jar包中相应的方法，是通过main函数还是其他容器方式？
   
   

## 查询请求执行流程
   1. rpc请求过来,解析请求,获取合约名,获取查询方法及参数
   2. exector模块启动jvm,加载合约相应的jar包,调用合约中相应的查询方法

## 合约示例
- [java开发的guess猜数字游戏合约](src/main/java/cn/chain33/jvm/dapp/guess)