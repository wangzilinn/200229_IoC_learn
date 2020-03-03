import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ClassPathXMLApplicationContext {

    List<XMLBean> xmlBeans = new ArrayList<>();
    Map<String, Object> idInstanceMap = new HashMap<>();

    public ClassPathXMLApplicationContext(String fileName) {
        this.readXML(fileName);
        //实例化bean
        this.instantiateBean();
        this.annotationInject();
    }

    /*读取配置文件并放到list中*/
    @SuppressWarnings("unchecked")
    public void readXML(String fileName) {
        SAXReader saxReader = new SAXReader();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Document document = saxReader.read(classLoader.getResourceAsStream(fileName));
            Element rootElement = document.getRootElement();
            for (Iterator<Element> iterator = rootElement.elementIterator(); iterator.hasNext(); ) {
                Element element = iterator.next();
                XMLBean xmlBean = new XMLBean(element.attributeValue("id"), element.attributeValue("class"));
                xmlBeans.add(xmlBean);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /*实例化Bean*/
    public void instantiateBean() {
        for (XMLBean xmlBean : xmlBeans) {
            try {
                idInstanceMap.put(xmlBean.getId(), Class.forName(xmlBean.getClassName()).getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                System.out.println("实例化时失败");
                e.printStackTrace();
            }
        }
    }

    /*注解处理器*/
    public void annotationInject() {
        for (String beanName : idInstanceMap.keySet()) {
            Object bean = idInstanceMap.get(beanName);
            if (bean != null) {
                //处理setter注解
                this.injectSetterAnnotation(bean);
                //处理field注解
                this.injectFieldAnnotation(bean);
            }
        }
    }

    //处理在set方法处加入的注解
    public void injectSetterAnnotation(Object bean) {
        try {
            //获得类的所有属性
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                //获取属性的写方法(如果有)
                Method setter = propertyDescriptor.getWriteMethod();
                //如果存在写方法, 并定义了注解
                if (setter != null && setter.isAnnotationPresent(Resource.class)) {
                    Resource resource = setter.getAnnotation(Resource.class);
                    Object value = null;
                    if (!"".equals(resource.id())) {
                        //使用了参数, 获得参数
                        String id = resource.id();
                        value = idInstanceMap.get(id);
                    } else {
                        //如果没有使用参数,则根据类型进行匹配
                        for (String key : idInstanceMap.keySet()) {
                            //判断当前属性所属的类型是否在配置文件中存在
                            if (propertyDescriptor.getPropertyType().isAssignableFrom(idInstanceMap.get(key).getClass())) {
                                value = idInstanceMap.get(key);
                                break;
                            }
                        }
                    }
                    setter.setAccessible(true);
                    setter.invoke(bean, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void injectFieldAnnotation(Object bean) {
        try {
            //获得类的所有字段
            Field[] fields = bean.getClass().getFields();
            for (Field field : fields) {
                if (field != null && field.isAnnotationPresent(Resource.class)) {
                    Resource resource = field.getAnnotation(Resource.class);
                    Object value = null;
                    if (!"".equals(resource.id())) {
                        //使用了参数
                        String id = resource.id();
                        value = idInstanceMap.get(id);
                    } else {
                        //未使用参数, 使用类型匹配
                        for (String key : idInstanceMap.keySet()) {
                            //判断当前属性是否在配置文件中存在
                            if (field.getType().isAssignableFrom(idInstanceMap.get(key).getClass())) {
                                value = idInstanceMap.get(key);
                                break;
                            }
                        }
                    }
                    field.setAccessible(true);
                    field.set(bean, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object getBean(String beanId) {
        return idInstanceMap.get(beanId);
    }





}
