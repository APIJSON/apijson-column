# apijson-column  [![](https://jitpack.io/v/APIJSON/apijson-column.svg)](https://jitpack.io/#APIJSON/apijson-column)
腾讯 [APIJSON](https://github.com/Tencent/APIJSON) 字段插件，支持 字段名映射 和 !key 反选字段，可通过 Maven, Gradle 等远程依赖。<br />
A column plugin for Tencent [APIJSON](https://github.com/Tencent/APIJSON), support Invert Selection and Mappding for columns.

### Maven
#### 1. 在 pom.xml 中添加 JitPack 仓库
#### 1. Add the JitPack repository to pom.xml
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
<br />

#### 2. 在 pom.xml 中添加 apijson-column 依赖
#### 2. Add the apijson-column dependency to pom.xml
```xml
	<dependency>
	    <groupId>com.github.APIJSON</groupId>
	    <artifactId>apijson-column</artifactId>
	    <version>LATEST</version>
	</dependency>
```

<br />
<br />
<br />

### Gradle
#### 1. 在项目根目录 build.gradle 中最后添加 JitPack 仓库
#### 1. Add the JitPack repository in your root build.gradle at the end of repositories
```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
<br />

#### 2. 在项目某个 module 目录(例如 `app`) build.gradle 中添加 apijson-column 依赖
#### 2. Add the apijson-column dependency in one of your modules(such as `app`)
```gradle
	dependencies {
	        implementation 'com.github.APIJSON:apijson-column:latest'
	}
```

### 初始化
### Initialization
见 [ColumnUtil](/blob/main/src/main/java/apijson/column/ColumnUtil.java) 的注释及 [APIJSONBoot](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot) 的 [DemoSQLConfig](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot/src/main/java/apijson/demo/DemoSQLConfig.java) 和 [DemoSQLEexcutor](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot/src/main/java/apijson/demo/DemoSQLEexcutor.java) <br />

See document in [ColumnUtil](/blob/main/src/main/java/apijson/column/ColumnUtil.java) and [DemoSQLConfig](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot/src/main/java/apijson/demo/DemoSQLConfig.java), [DemoSQLEexcutor](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot/src/main/java/apijson/demo/DemoSQLEexcutor.java) in [APIJSONBoot](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot)


### 使用
### Usage

#### 1.反选字段
#### 1. Column Inverse
"@column": "!columnKey"  // 返回排除 columnKey 后的全部其它字段
"@column": "!columnKey"  // return all columns except for columnKey
```js
{
    "User": {  // id,sex,name,tag,head,contactIdList,pictureList,date
        "id": 82001,
        "@column": "!contactIdList"  // -> id,sex,name,tag,head,pictureList,date
    }
}
```

#### 2.字段名映射
#### 2. Column Mapping
"@column": "mappedKey"  // 隐藏了数据库的对应真实字段名
"@column": "mappedKey"  // the real column name is hidden
```js
{
    "User": {  // id,sex,name,tag,head,contactIdList,pictureList,date
        "id": 82001,
        "@column": "gender"  // -> sex 
    }
}
```

<br /><br />
