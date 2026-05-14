# 基本知识
- 当定义一个工具类时：可以private这个类，防止别人创建这个类的对象，私有化构造方法，防止被实例化，
- userAgent是浏览器/APP自带的身份自我介绍字符串，可以用来用来记录客户端访问信息


## 枚举类：
- 固定不变的一个选项列表
- 给固定，有限，不会改变的类型起别名
- 与String的对比：
  1. 容易写错单词
  2. 无法限制范围，别人传什么都能传进来，导致代码崩溃
  3. 别人不知道传什么
- 枚举只能选定义好的值，会自动弹出来提示
- 不能随便改变，是事先定义好的


## Record类

- 如果只是想单纯装一组数据，不需要逻辑
- 用于接口返回的DTO的封装
- 接收前端参数
- 临时组装中间数据
- 配置类
- 日志审计，客户端信息
- 最大特点：一旦new出来就不可修改

- 极简，只读，用来封装数据的实体类
- 纯数据载体，自带以下功能：
    1. 不用写 getter、构造器、toString：
    2. 自动生成私有 final 字段
    3. 自动生成全参构造器
    4. 自动生成 getter（方法名就是字段名，不用 getXXX）
    5. 自动生成 equals () /hashCode ()
    6. 自动生成 toString ()
    7. 不能被修改（只读）

## 工具类

- 工具类标准写法就是：
  class 用 final
  构造方法私有
  所有方法都是 static
- final class的作用：禁止被继承扩展，重写

- 在工具类,不需要实例化，不可变类，不希望被扩展的核心业务类可以用这个

## 安全篇
```java
private final StringRedisTemplate redisTemplate;

    public RedisVerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
```

- 这段代码使用构造器注入比注解注入更安全
- 可以用Final修饰保证线程安全+不可变
- 测试难度低


安全的随机数生成器
```java
  private static final SecureRandom RANDOM = new SecureRandom();
```

- 它是 Spring 提供的工具方法，用来判断一个字符串：不为 null、不为空、不全是空格。
- 
## Redis篇

获取Redis的Hash操作器
```java
  HashOperations<String, String, String> ops = redisTemplate.opsForHash();
```

获取hash表key下的所有field和value
```java
Map<String, String> data = ops.entries(key);
```
设置过期时间
```java
redisTemplate.expire(key, ttl);
```
把一个字符串转换成整数的方法
```java
Integer.parseInt(value);
```


将一个数据转换成String类型
```java
String.valueOf(updatedAttempts)
```

- Duration 是 Java 8 提供的时间工具类，专门用来表示「一段时间的长度」（比如 5 分钟、30 秒、1 小时）
```java
redisTemplate.expire(key, Duration.ofMinutes(30));
```

设置一个key的默认为1，interval后自动过期
```java
stringRedisTemplate.opsForValue().set(key, "1", interval);
```

对Redis中的key的value加1，并返回增加后的值
```java
Long count = stringRedisTemplate.opsForValue().increment(key);
```


