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
- Objects.equals(a, b) = 安全判断两个对象是否相等，永远不会报空指针！
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

- 问题一：JWT本身是存储在客户端的，服务端不存，用户登出或管理员踢人的话，这个Token还能使用造成巨大安全漏洞
- 解决方法：做一个Redis白名单，把RefreshToken写到Redis中，只有在这个里面的Token才是有效的
- 以后用户或者管理员在刷新操作时需要去Redis中查询（会不会影响性能：不会，内存数据库，微秒级操作）
- 如果Redis中没有这个，说明这个RefreshToken失效了

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



































