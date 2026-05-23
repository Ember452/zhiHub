前端运行：npm run dev

# 基本知识
-去掉字符串首尾所有空白，strip()更安全
cs = cs.trim();
- 当定义一个工具类时：可以private这个类，防止别人创建这个类的对象，私有化构造方法，防止被实例化，

- userAgent是浏览器/APP自带的身份自我介绍字符串，可以用来用来记录客户端访问信息

- @PatchMapping:局部更新，不用传整个对象

- @AuthenticationPrincipal:因为在config里面开启了jwt解析，可以直接用这个拿到

- 这个还可以直接拿到前端传进来的User等对象

- @AuthenticationPrincipal Jwt jwt：拿到jwt对象

- Spring构造函数执行时机
  - Spring 启动 → 扫描 @Service 注解 → 发现 CounterServiceImpl
    → 创建实例 → 自动调用构造函数 → 注入依赖参数 → 完成初始化

- 如果需要把一个List<String> str转换为Object类型,可以用str.toArray()

- @Scheduled(fixedDelay = 1000L)用于设置一个自动执行的定时任务

- 由于Map本身不能遍历，把Map里面的一条条键值对打包成一个集合就可以遍历了
  
  ```java
  Map.Entry<Object, Object> e : entries.entrySet()
  ```

- String.valueOf()把任何值转化成字符串

- 如果一个值不固定的话可以先转字符串再转Long
  
  ```java
  delta = Long.parseLong(String.valueOf(e.getValue()));
  ```

- 将一个数转换成int类型
  
  ```java
  idx = Integer.parseInt(field);
  ```
  
- 把一个 List 截取「从第 0 个开始，到 safeSize 个位置」的子集合，只保留前 safeSize 条数据，多余的全部砍掉。
```java
 rows = rows.subList(0, safeSize);
```


- java底层最快的数组拷贝方法
- 把 Redis 读出来的原始字节数组 raw，拷贝到目标缓冲区 buf 里，拷贝长度是 len。
- 
```java
System.arraycopy(raw, 0, buf, 0, len)
```

### Spring容器生命周期篇
1. 如果需要定义一个常驻后台的监听服务，可以继承SmartListcycle类
2. 这个SmartListcycle是Spring容器的生命周期管理器
3. 可以让一个类在Spring启动时自动运行，Spring关闭时自动停止
4. 可以override里面的start，end方法
```java
CanalKafkaBridge implements SmartLifecycle
```


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

### Record类和普通class的选择

#### 选择

Record 的规则只有一条：
创建之后，里面的三个值（status、attempts、maxAttempts）永远不能变！

这种一次创建，不在修改的可以用Record

```java
return new VerificationCheckResult(TOO_MANY_ATTEMPTS, 3,5);
```

这种需要反复修改的必须是class

```java
LoginLog log = new LoginLog();  // 1. 先 new 空对象
log.setUserId(1001);           // 2. 改
log.setIp("1.2.3.4");          // 3. 改
log.setStatus("SUCCESS");      // 4. 改
```

#### 优缺点

- Record：代码简洁，线程安全没有并发问题，适合做返回值，DTO
  
        。不能被继承，不能有无参构造，适合用在接口返回结果，数据查询返回，不可变配置，一次性数据载体

- Class + @Data:可以随意改，支持无参构造，但有被修改的风险，依赖lombok
- 和数据库表对应的实体类，必须用class 
## 工具类

//创建一个空的字符串拼接容器，用来高效拼接字符串
StringBuilder buf = new StringBuilder();

- 工具类标准写法就是：
  class 用 final
  构造方法私有
  所有方法都是 static

- final class的作用：禁止被继承扩展，重写

- 在工具类,不需要实例化，不可变类，不希望被扩展的核心业务类可以用这个

- 把前端传过来的字符串类型的 postId，转换成后端能用的数字类型（Long）

- Long.parseLong()把字符串类型数字转换成java长整型
  
  ```java
  postId = Long.parseLong(request.postId());
  ```

- 将一个java对象转换文json字符串

- writeValueAsString :把任意java对象转换为json

- 因为kafka不支持直接传输java对象

- 这里用到了ObjectMapper：一个工具类，用来java对象和json字符串的转换

- private final ObjectMapper objectMapper;
  
  ```java
  String payload = objectMapper.writeValueAsString(event);
  ```

- java8中函数式调用，更简洁
```java
// 读取第 idx 段的计数（1 基坐标），大端拼接为 long
        // 0xFFL转成无符号数，接收一个idx，返回一个long
        IntFunction<Long> read = idx -> {
            if (idx < 1 || idx > seg) return 0L;
            int off = (idx - 1) * 4;
            long n = 0;
            for (int i = 0; i < 4; i++) {
                n = (n << 8) | (buf[off + i] & 0xFFL);
            }
            return n;
        };
        //函数式变量：调用apply来传参数，获取第一段和第二段的32位字节数据
        //这样内聚写，更干净
        long sdsFollowings = read.apply(1);
        long sdsFollowers = read.apply(2);
```

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

### Optional

- 是一个容器，里面要么有值要么为空，有就返回，没有就抛异常，避免忘写判断判断导致空指针异常

- 查询用户，返回一个“安全盒子”Optional<User>
- 判断盒子里【没有用户】→ 直接抛异常（业务要求）
- 经过上面判断，这里**一定有值**，所以可以安全 get()
  
  ```java
  Optional<User> userOptional = findUserByIdentifier(request.identifierType(), identifier);
        if (userOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        User user = userOptional.get();
  ```

### Objects.equals(a, b)

- Objects.equals(a, b) = 安全判断两个对象是否相等，永远不会报空指针！]()
### singleFlight
这是本地 JVM 内存锁（单机锁），专门用来做合并重复请求、防止缓存击穿、防止回源雪崩。
// 作用：同一个 key，只让一个线程去回源查库，其他线程等待。
// 自动去重、合并请求
// 性能极高.无Redis网络IO，无锁竞争损耗，高并发神器
Object lock = singleFlight.computeIfAbsent(idsKey, k -> new Object());
## Canal篇
Canal Server 端存储位点，而不是在你的应用代码中。具体来说：
Canal Server 持久化位点
Canal Server 会将每个 destination（实例）的消费位点存储在自身的数据目录中
默认位置：conf/example/meta.dat（以你的配置 destination: example 为例）
这个文件记录了最后成功 ack 的 binlog position

Message = Canal 一次性打包发给你的「一批数据 + 控制信息」
它不是业务消息，是 Canal 通信协议里的数据载体。

```java
 Message message = connector.getWithoutAck(batchSize);
```
  
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

- 按照put的顺序输出，保证顺序不变
  
  ```java
  Map<String, Long> result = new LinkedHashMap<>();
  ```

- Redis执行lua脚本时传参必须是List，基于社区规定，集群安全，脚本规范，哪怕只有一个数据
  

- Spring Data Redis 中最底层的 RedisCallback 用法
- 如果用普通的方法，要结果Spring的序列化，反序列化，有性能损耗
- 在基数等高并发，超高频下不适用
```java
// 1. 执行 Redis 原生命令，返回字节数组
byte[] raw = redis.execute(
    // 2. RedisCallback 回调：直接操作底层 Redis 连接
    (RedisCallback<byte[]>) c -> 
        // 3. 获取字符串命令 → 执行 GET 命令
        c.stringCommands().get(
            // 4. key 转 UTF-8 字节数组
            key.getBytes(StandardCharsets.UTF_8)
        )
);
```

- members 获取一个集合中全部符合的内容
```java
Set<String> cur = redis.opsForSet().members("feed:public:index:" + eid + ":" + hourSlot);
```

- multiGet是批量获取，一次IO拿所有内容---只能批量取值，不能批量执行命令
- 
```java
List<String> itemJsons = redis.opsForValue().multiGet(itemKeys);
```
  ## JWT篇

```java
//生成JWT
private final JwtEncoder jwtEncoder;
//解析JWT
private final JwtDecoder jwtDecoder;
private final AuthProperties properties;
//获取当前时间
private final Clock clock = Clock.systemUTC();
```

Instant = Java 标准的 UTC 时间戳对象，代表时间线上一个绝对时刻，专门用来做 JWT 过期、时间戳、跨时区时间计算。
不可变，线程安全java8+新API

```java
Instant issuedAt = Instant.now(clock);
```

- 构建JWT载核，所有存到Token里面的信息都在这里
  
  ```java
  JwtClaimsSet claims = JwtClaimsSet.builder()
  ```

- 判断左边对象是不是右边类型，如果是，强转为这个类型
  
  ```java
  if (claim instanceof Number number) {
    return number.longValue();
  }
  ```

### Claims

- JWT数据包，里面存放了用户ID，权限等信息，
- 调用get方法拿key=userId对应的value
- jwt.getClaims() 获取 JWT 里存放数据的容器
  
  ```java
  Object claim = jwt.getClaims().get(CLAIM_USER_ID);
  ```

### JWT定义作用

JWT 对象 = 解码后的 token 本身，它就是一个装信息的 “数据包”。
JWT 对象 = 把一串加密的 token 字符串，解析成 Java 能看懂、能取值的对象。
这个里面存放着所有Token里的数据

## 日志篇

 audit包：审计日志包
把登录日志记录到数据库表中

## 登录注册篇

HttpServletRequest = 前端发给后端的【一次 HTTP 请求的所有信息包裹】
包含请求头（useragent,token,等）
请求参数
请求信息
客户端信息
可以用get的方法获取HttpServletRequest的信息

```java
HttpServletRequest httpRequest;
```

告诉前端成功了，但没消息给你
ResponseEntity是Spring封装好的完整响应包

```java
return ResponseEntity.noContent().build();
```

.trim可以把字符串前后的空格全部删掉，或者转换大小写

```java
private String normalizeIdentifier(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> identifier.trim();
            case EMAIL -> identifier.trim().toLowerCase(Locale.ROOT);
        };
    }
```

判断一个字符串是否包含字母，数字.chars()把字符串拆成字符编码流
isLetter：判断是否有字母
Character:isDigit：判断是否有数字

```java
String trimmed = "abc123";
boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);
boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);
```

- 查用户 → 如果查不到，直接抛异常 → 查到了就直接赋值给 user
- orElseThrow(),有就返回，没有就抛异常
  
  ```java
  User user = findUserById(userId).orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
  ```

- 把用户输入的明文密码，用 Spring Security 的 BCrypt 算法加密成哈希密码，然后存到用户对象里，准备保存到数据库。
  
  ```java
  user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
  ```

### 映射令牌到响应对象

- 把TokenPair中中的部分字段拿到TokenResponse中，供前端使用
  
  ```java
  private TokenResponse mapToken(TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(), tokenPair.accessTokenExpiresAt(), tokenPair.refreshToken(), tokenPair.refreshTokenExpiresAt());
    }
  ```

## 数据与安全篇

DTO 映射（实体转响应对象）

- 把数据库里的 User 对象，转换成前端安全、干净、需要的格式再返回，绝对不能直接返回数据库实体。
- 防止前端收到所有信息，包括密码等
- 相当与把user表中部分字段拿来使用

## OSS篇

- 后端生成上传链接（putUrl），就是给前端一个 “临时专用上传通道”，前端拿着它，直接把文件上传到阿里云 / 腾讯云 / 亚马逊云的存储服务器，不用传给后端服务器。

### 什么时候用 uploadAvatar（后端上传）什么时候用 presign（前端直传）

1. 用 uploadAvatar 后端接收上传
   
   - 适用场景
     小文件：头像、头像缩略图、签名、小图片（几十 KB~ 几 MB）
     上传量少、并发低：个人头像、个人资料图
   - 必须后端做强校验 
     校验图片尺寸、分辨率、裁剪
     压缩图片、加水印
     严格过滤违规图、恶意文件
     前端技术弱，不想写上传逻辑
     内部后台、管理端上传
   - 流程
     前端传文件 → 后端接收 → 后端直传 OSS → 返回地址缺点：占用后端服务器带宽、大文件容易超时

2. 用 presign 预签名前端直传
   
   - 适用场景
     大文件：文章配图、长图、视频、附件、批量图片
     用户量大、高并发：社区发帖、动态配图、用户大量上传
     追求速度、减轻服务器压力
     移动端、小程序、APP 大量上传场景
     不需要后端实时处理图片，只存原图即可
   
   - 流程
     前端拿参数找后端拿临时上传链接 → 前端直接 PUT 传到 OSS → 上传完通知后端存数据库优点：不占后端带宽、上传快、支持断点续传、体验好
     
     3. 最简选择口诀
        头像、小图、要压缩裁剪 → 后端上传 uploadAvatar
        发帖配图、大图、多图、大流量 → 预签名直传 presign
        三、优缺点对比
        后端上传
        ✅ 校验强、逻辑集中、好管控❌ 耗服务器流量、大文件易超时
        前端直传
        ✅ 速度快、服务器零压力、支持大文件❌ 前端要写上传逻辑，基础校验放前端
        四、项目标准搭配（企业常用）
        用户头像：统一用 uploadAvatar 后端上传，统一裁剪压缩
        发布文章 / 动态图片：统一用 presign 预签名直传

## 注解篇

1. ResponseEntity<>:是一个标准的响应封装，可以设置状态码，响应头
2. @Valid：开启自动校验
3. @RequestBody：把前端json转换成java对象
4. @AuthenticationPrincipal:SpringSecurity专用注解，自动把当前用户的令牌注入进来
   
   ```java
   public ResponseEntity<Map<String, Object>> like(@Valid @RequestBody ActionRequest req,
                                                    @AuthenticationPrincipal Jwt jwt)
   ```

5. @Qualifier("feedPublicCache")告诉 Spring：我要注入名字叫 feedPublicCache 的那个 Bean，别装错了！
```java
@Qualifier("feedPublicCache") Cache<String, FeedPageResponse> feedPublicCache
```

6. @PostConstruct：在Spring项目启动后立即执行一次
## kafka篇

- Acknowledgment = Kafka 消息的 “确认签收条”只有你调用 ack.acknowledge()，Kafka 才认为这条消息真正消费成功。
- 

## Lua脚本篇

这里为什么选用lua脚本：

- Lua 脚本是一整段发给 Redis

- Redis 单线程执行，不会被打断

- 读 → 改 → 写 一步到位

- 高并发下，一定会出现数据错乱！ 比如两个点赞同时来：
  
  - A 读到 10
  - B 读到 10
  - A 写 11
  - B 写 11
  
  结果应该是 12，实际变成 11！—— 数据丢了这就是 **并发冲突 / 线程安全问题**。

INCR_FIELD_LUA结构：是**一段固定长度的二进制数据**：

[ 点赞4字节 ][ 评论4字节 ][ 收藏4字节 ][ 浏览4字节 ]...

- 每个计数占 4 个字节
- 一串二进制存所有指标
- 不用 Hash，用原生 String 存计数
- 极快、极小、极高并发

点赞：------->在二进制的4-8字节-------->取出二进制------>跳到4字节------->读取旧值------->加、减-------写回二进制-------->保存

好处：

1. 速度快

2. 极小，100个计数只占400字节

3. 原子安全Lua 脚本保证并发不冲突

4. 固定结构：按位置找数，速度最快

5. 高并发点赞不会覆盖

## Spring跨域篇

- CorsFilter ：一个全局过滤器：在请求进入Controller之前，自动处理跨域逻辑
  
  - 是后端给前端开的一个跨域通行证，告诉浏览器允许这个前端访问

- CorsConfiguration：Spring专门用来封装跨域规则的配置类，所有跨域规则都在这个里面配置

- 配置跨域规则:只允许/profile/下的所有接口访问

- ```java
  UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  // 仅对 Profile 相关接口开启跨域
  ```java 
  source.registerCorsConfiguration("/api/v1/profile/**", config);
  ```

## 遇到的问题

- 问题一：JWT本身是存储在客户端的，服务端不存，用户登出或管理员踢人的话，这个Token还能使用造成巨大安全漏洞

- 解决方法：做一个Redis白名单，把RefreshToken写到Redis中，只有在这个里面的Token才是有效的

- 以后用户或者管理员在刷新操作时需要去Redis中查询（会不会影响性能：不会，内存数据库，微秒级操作）

- 如果Redis中没有这个，说明这个RefreshToken失效了

- 问题二：在CounterEventProducer中kafka消息发送是异步的，抛异常时，代码不知道，消息直接丢了，

- 解决方案：监听kafka发送结果

- 失败打印错误栈

- 不阻塞主流程

- 可接入告警：失败量高告警

- 问题三：定时任务 flush () 在刷数据的同时，Kafka 消费者还在往同一个聚合桶里写新数据。
  如果直接删除字段，会把刚写入的新增量覆盖 / 丢掉！
  必须用 Lua 原子扣减，不能直接覆盖！
- 如果在扣减后，发现为0要删除时又有一条点赞怎么做：
  - 采用lua脚本，保证扣减完删除之间不插入新数据，新数据再重新创建一个字段存储



- 问题四：在查询用户关系时，如果有大V，Redis和数据库扛不住流量
- 解决方案：创建两个本地内存缓存，给大V用户用


- 问题五：在消费kafka消息时，如果用户10分钟内取关后再关注，由于重复消息拦截无法成功



## ElasticSearch
- ES为了性能高，不是写入一条就能搜到的，写入的这个数据，先到内存缓冲区，每隔一秒自动刷新
- 这1秒内搜不到，
```java
IndexRequest<Map<String, Object>> req = IndexRequest.of(b -> b
                    .index(INDEX)
                    .id(String.valueOf(id))
                    .document(doc)
                    .refresh(Refresh.WaitFor)
            );
```
- 我不着急写，但你要等数据变成 “可搜索” 了，再给我返回结果。
- 你写入数据
  ES 等待数据刷新完成（几毫秒～几十毫秒）
  数据立刻可搜索
  然后再返回成功给你
1. Refresh.True （强制刷新）
     立刻刷新索引
     性能最差，高并发会把 ES 写崩
     不推荐
2. Refresh.WaitFor （你代码里用的这个）
   等待数据变成可搜索，但不主动强制刷新
   性能好、实时性高
   发布文章、编辑后立刻要搜到的场景用这个
3. Refresh.False（默认）
   不刷新，等 1 秒自动刷
   性能最好
   但写入后1 秒内搜不到


- functionScore 是 Elasticsearch 中一个非常强大的打分控制机制，它的作用是：在基础相关性得分之上，叠加业务规则的额外加分



- Elasticsearch 客户端的设计是：嵌套！嵌套！嵌套！