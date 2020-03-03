**Spring IoC**
============

2020年3月3日:没有实现构造器注入, 估计要实现需要大改逻辑, 

本文由word自动转换而来

**依赖注入与控制反转(IoC)**
============

是什么:
-------

Spring 最认同的技术是控制反转的依赖注入（DI）模式。

**控制反转（IoC, Inversion of
Control）是一个通用的概念，它可以用许多不同的方式去表达，依赖注入仅仅是控制反转的一个具体的例子。**

当编写一个复杂的 Java 应用程序时，应用程序类应该尽可能的独立于其他的 Java
类来增加这些类可重用可能性，当进行单元测试时，可以使它们独立于其他类进行测试。依赖注入（或者有时被称为配线）有助于将这些类粘合在一起，并且在同一时间让它们保持独立。

字段与属性:
-----------

>   <https://blog.csdn.net/chenchunlin526/article/details/71424844>

**字段(field)**，通常是在类中定义的类成员变量，例如：
```java
public class A{
	private String s = "123";
}
```

称:A类有一个字段s 。

字段一般用来承载数据，所以为了安全性，一般定义为私有的。

字段和常量描述了类的数据（域），当这些数据的某些部分不允许外界访问时，一般将其设置为private类型。

既然是私有，那外界怎么访问呢?

当然是通过Java的get/set方法, 这些特殊的方法就被称为**属性**

**属性(property)**只**局限于类中方法的声明**，**并不与类中其他成员相关**，属于JavaBean的范畴。

例如：
```java
void setA(String s){}
String getA(){}
```

当一个类中拥有这样一对方法时，可以说，

这个类中拥有一个**可读写**的a属性(注意是小写a)。

如果去掉了set的方法，则是可读属性，反之亦然。

使用方法:
=========

XML配置:
--------

在XML中写明注入方法:

*注意:*

id:被注入的变量的名字

class:被注入的变量的类型
```XML
<?xml version="1.0" encoding="UTF-8" ?>  
<beans\>  
	<bean id="cardDAO" class="CardDAO"\>  
	<bean id="chatDAO" class="ChatDAO"\>  
	<bean id="userDAO" class="UserDAO"\>  
	<bean id="userService" class="UserService"\>  
<beans\>
```

添加注解:
---------

在类内的字段上或setter上写明注解:
```java
public class UserService {  

    public UserDAO userDAO;  
    public ChatDAO chatDAO;  

    //字段注入  
    \@Resource  
    public CardDAO cardDAO;  

    \@Resource(name = "userDAO")  
    public void setUserDAO(UserDAO userDAO) {  
    	this.userDAO = userDAO;  
    }  

    //无参数  
    \@Resource  
    public void setChatDAO(ChatDAO chatDAO) {  
    	this.chatDAO = chatDAO;  
    }  

    public void show() {  
        userDAO.show();  
        chatDAO.show();  
        cardDAO.show();  
    }  
}
```

运行:
-----

在main中运行:

首先传入xml文件,获得上下文Context

之后从Context中获得实例即可使用

```java
public class Main {  
	public static void main(String[] args) {  
        ClassPathXMLApplicationContext context = new ClassPathXMLApplicationContext(  
        "context.xml");  
        UserService userService = (UserService) context.getBean("userService");  
        userService.show();  
    }  
}
```

辅助类:
=======

注解类:
-------


```java
//保留到运行时  
\@Retention(RetentionPolicy.*RUNTIME*)  
//注解适用的地方  
\@Target({ElementType.*FIELD*, ElementType.*METHOD*})  
public \@interface Resource {  
//注解的name属性  
public String name() default "";  
}
```

储存单条XML信息:
----------------

使用类XMLBean来储存从XML读取来的数据, 有两个字段:

1. id:String

2. className:String

```java
public class XMLBean {  
    private String id;  
    private String className;
    ……
}
```

ClassPathXMLApplicationContext:
===============================

最核心的类是ClassPathXMLApplicationContext.

其执行过程为:

1. 在运行构造函数时, 读取XML文件, 实例化对象, 并将所有的对象保存在一个Map中

2. 执行getBean()方法, 从Map中获得所需的对象.


字段
----

这个类有两个字段:
```java
List<XMLBean> xmlBeans = new ArrayList<>();  
Map<String, Object> idInstanceMap = new HashMap<>();
```
其中:

1. xmlBeans:List这个list储存单条从XML中读取来的数据

2. idInstanceMap:Map 这个Map储存从XML读取出来的每一个对象实例


构造方法:
---------
```java
public ClassPathXMLApplicationContext(String fileName) {  
    this.readXML(fileName);  
    //实例化bean  
    this.instantiateBean();  
    this.annotationInject();  
}
```
构造方法做了一下几个事:

1. readXML():将XML中的字符串写到XMLBeans字段中

2. instantiateBean():将XMLBeans.className实例化, 并放到idInstanceMap中

3. annotationInject():将idInstanceMap.values中被实例化的对象分配给等待他们的引用变量中


接下来分别介绍每个方法

readXML()
---------

执行过程:

1. 首先读取XML文件,
2. 对于每一行XML:
   1. 生成一个XMLBean, 将这行XML中的id字段和class字段分别放到XMLBean对象的对应字段中
   2. 把XMLBean加入到XMLBeans列表中

核心代码:
```java
Document document = saxReader.read(classLoader.getResourceAsStream(fileName));  
Element rootElement = document.getRootElement();  
for (Iterator<Element> iterator = rootElement.elementIterator();
     iterator.hasNext(); ) {  
    Element element = iterator.next();  
    XMLBean xmlBean = new XMLBean(element.attributeValue("id"),
                                  element.attributeValue("class"));  
    xmlBeans.add(xmlBean);  
}
```
instantiateBean()
-----------------

执行过程:

1. 对于XMLBeans列表中的每一个XMLBean:
   1. 获取其字符串格式的名字, 使用Class类通过反射将其实例化
   2. 将被实例化的对象装入以被注入变量名字(id)为key的idInstanceMap中

核心代码:
```java
for (XMLBean xmlBean : xmlBeans) {  
	idInstanceMap.put(xmlBean.getId(),
	Class.forName(xmlBean.getClassName()).getDeclaredConstructor().newInstance());  
}
```
**注意:**

这里的实例化是直接使用了Class类的实例化函数newInstance, 此时调用了构造器,
如果此时构造器也需要其他的对象作为参数, 则会实例化失败

annotationInject()
------------------

执行过程:

1. 对于idInstanceMap中的每一个映射:
   1. 获取该映射中的value,也就是实例
   2. 检查这个实例内部的所有Setter方法有没有被\@Resource注解,
      如果有,则从idInstanceMap中查找需要被注入的实例, 并注入
   3. 检查这个实例内部所有的字段有没有被\@Resource注解, 如果有,
      则从idInstanceMap中查找需要被注入的实例, 并注入

以上的(1.2)和(1.3)分别通过injectSetterAnnotation()方法和injectFieldAnnotation()方法实现,
会在下面分别表述

injectSetterAnnotation(Object)
------------------------------

这个方法用于检查传入的Object中的所有Setter方法有没有被\@Resource注解, 如果有,
则从idInstanceMap中查找需要被注入的实例, 并注入

执行过程:

1. 获得传入对象的类的所有属性(property)

2. 对于每一个属性:
   1. 检查该属性是否是可写属性(即存在Set方法), 如果是:
   2. 检查该方法是否被\@Resource注解, 如果有:
   3. 检查该注解是否使用了参数(即id)
      1. 如果有:根据id从idInstanceMap中查找对象
      2. 如果没有:遍历整个idInstanceMap, 找到第一个与被注入字段类型相同的对象
   4. 通过反射, 为Setter方法传入上一步获得的对象, 从而实现注入

核心代码:
```java
//获得类的所有属性  
PropertyDescriptor[] propertyDescriptors =
Introspector.*getBeanInfo*(bean.getClass()).getPropertyDescriptors();  
for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {  
    //获取属性的写方法(如果有)  
    Method setter = propertyDescriptor.getWriteMethod();  
    //如果存在写方法, 并定义了注解  
    if (setter != null && setter.isAnnotationPresent(Resource.class)) {  
        Resource resource = setter.getAnnotation(Resource.class);  
        String id = "";  
        Object value = null;  
        if (!"".equals(resource.id())) {  
            //使用了参数, 获得参数  
            id = resource.id();  
            value = idInstanceMap.get(id);  
        } else {  
            //如果没有使用参数,则根据类型进行匹配  
            for (String key : idInstanceMap.keySet()) {  
                //判断当前属性所属的类型是否在配置文件中存在 
                if(propertyDescriptor.getPropertyType().isAssignableFrom(idInstanceMap.get(key).getClass()))
                {  
                    value = idInstanceMap.get(key);  
                    break;  
                }  
            }  
        }  
        setter.setAccessible(true);  
        setter.invoke(bean, value);  
    }  
}
```
injectFieldAnnotation(Object)
-----------------------------

该方法与injectSetterAnnotation()方法类似,
检查这个实例内部所有的字段有没有被\@Resource注解, 如果有,
则从idInstanceMap中查找需要被注入的实例, 并注入

这个函数由于只在字段上进行操作, 因此不需要获得属性

执行过程:

1. 获得传入对象的类的所有字段(field)

2. 对于每一个字段:
   1. 检查该方法是否被\@Resource注解, 如果有:
   2. 检查该注解是否使用了参数(即id)
   3. 如果有:根据id从idInstanceMap中查找对象
   4. 如果没有:遍历整个idInstanceMap, 找到第一个与被注入字段类型相同的对象
   5. 通过反射, 为Setter方法传入上一步获得的对象, 从而实现注入

getBean(String)
---------------

ClassPathXMLApplicationContext类执行完构造函数后,
XML文件中所有的对象已经被注入好, 且全被保存到了idInstanceMap字段中,
此时调用getBean(), 就是返回这个Map中的对象

实现代码:
```java
public Object getBean(String beanId) {  
return idInstanceMap.get(beanId);  
}
```