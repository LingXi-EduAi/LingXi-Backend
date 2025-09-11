# 灵犀教育后端服务 (LingXi-EduAI Backend)

## 项目介绍

灵犀教育后端服务是一个基于Spring Boot开发的教育管理系统后端API，为教师和学生提供完整的教学管理功能。系统集成了AI智能教学辅助功能，支持教案生成、智能出题、学情分析等核心教育场景。

## 技术架构

### 核心技术栈
- **框架**: Spring Boot 2.x
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **持久层**: MyBatis
- **安全认证**: 基于Token的身份验证
- **文档**: MyBatis XML映射
- **构建工具**: Maven

### 系统架构
```
├── 控制层 (Controller)     # 接收HTTP请求，参数验证
├── 服务层 (Service)        # 业务逻辑处理
├── 数据访问层 (Mapper)     # 数据库操作
├── 实体层 (POJO)          # 数据实体对象
├── 工具层 (Util)          # 通用工具类
├── 配置层 (Config)        # 系统配置
└── 拦截器 (Interceptor)   # 请求拦截处理
```

## 核心功能模块

### 用户管理模块
- 用户注册、登录、注销
- 用户信息管理
- 角色权限控制（教师/学生）
- Token认证机制

### 教学管理模块
- 班级创建与管理
- 课程信息管理
- 学生分组管理
- 教学资源上传

### 作业系统模块
- 作业发布与管理
- 作业提交与批改
- 成绩统计分析
- 学习进度跟踪

### AI集成模块
- 智能教案生成
- 自动出题系统
- 学情智能分析
- 个性化推荐

## 项目结构

```
src/main/java/com/lxe/lx/
├── annotation/          # 自定义注解
├── config/             # 配置类
│   ├── AuthorizationInterceptor.java  # 权限拦截器
│   ├── WebMvcConfig.java             # MVC配置
│   └── WebMvcTokenConfig.java        # Token配置
├── controller/         # 控制器
│   ├── TokenController.java          # 登录认证
│   ├── CustomerController.java       # 用户管理
│   ├── LXClassController.java        # 班级管理
│   ├── HomeworkController.java       # 作业管理
│   └── ...
├── service/           # 服务层
│   ├── impl/          # 服务实现
│   └── ...
├── mapper/            # 数据访问层
├── pojo/              # 实体类
├── util/              # 工具类
└── LxApplication.java # 启动类

src/main/resources/
├── application.properties           # 主配置文件
├── application-dev.properties      # 开发环境配置
├── application-prod.properties     # 生产环境配置
└── mybatis/                       # MyBatis映射文件
```

## 环境要求

- Java 8+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

## 快速开始

### 1. 环境准备

确保已安装并启动以下服务：
- MySQL数据库服务
- Redis缓存服务

### 2. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE lx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入数据表结构和初始数据
mysql -u root -p lx < db/lx_full.sql
```

### 3. 配置文件设置

根据实际环境修改配置文件：

**application-dev.properties**
```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/lx?...
spring.datasource.username=root
spring.datasource.password=your_password

# Redis配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=your_redis_password
```

### 4. 启动应用

```bash
# 使用Maven启动
mvn spring-boot:run

# 或者编译后启动
mvn clean package
java -jar target/LingXi-EduAI-1.0.jar
```

### 5. 验证服务

访问 `http://localhost:5678` 确认服务正常启动

## API接口文档

### 认证接口
- `POST /token/login` - 用户登录
- `POST /token/logout` - 用户注销

### 用户管理
- `POST /customer/detail` - 获取用户详情
- `POST /customer/register` - 用户注册
- `POST /customer/update` - 更新用户信息

### 班级管理
- `POST /LXClass/create` - 创建班级
- `POST /LXClass/list` - 班级列表
- `POST /LXClass/join` - 加入班级

### 作业系统
- `POST /homework/create` - 创建作业
- `POST /homework/submit` - 提交作业
- `POST /homework/grade` - 批改作业

## 开发规范

### 代码规范
- 使用驼峰命名法
- 类名首字母大写
- 方法和变量名首字母小写
- 常量全大写，下划线分隔

### 接口规范
- 统一使用POST请求
- 返回格式统一使用ResultConstant
- 错误码规范化处理
- 请求参数验证

### 数据库规范
- 表名使用下划线命名
- 字段名使用下划线命名  
- 主键统一使用id
- 创建时间字段：create_time
- 更新时间字段：update_time

## 部署说明

### Docker部署

```bash
# 构建镜像
docker build -t lingxi-backend .

# 运行容器
docker run -d -p 5678:5678 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your_db_host \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  lingxi-backend
```

### Docker Compose部署

```bash
# 启动完整服务栈
docker-compose up -d
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接参数是否正确
   - 确认网络连通性

2. **Redis连接失败**
   - 检查Redis服务状态
   - 验证Redis配置参数
   - 检查防火墙设置

3. **Token验证失败**
   - 检查Redis中Token是否存在
   - 验证Token格式是否正确
   - 确认Token是否过期

## 参与贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 版本历史

- v1.0.0 - 初始版本发布
- v1.1.0 - 添加AI功能集成
- v1.2.0 - 优化性能和用户体验

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

如有问题或建议，请通过以下方式联系：
- 项目Issues: [GitHub Issues](https://github.com/your-repo/issues)
- 邮箱: support@lingxi-edu.com

## 致谢

感谢所有为本项目做出贡献的开发者和用户！
