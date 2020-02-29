import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ObjectStreamException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ClassPathXMLApplicationContext {

    List<BeanDefine> beanList = new ArrayList<>();
    Map<String, Object> sigletions = new HashMap<>();

    public ClassPathXMLApplicationContext(String fileName) {
        this.readXML(fileName);
        //实例化bean
        this.instancesBean();
        this.annotationInject();
    }

    /*读取配置文件并放到list中*/
    @SuppressWarnings("unchecked")
    public void readXML(String fileName) {
        SAXReader saxReader = new SAXReader();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Document document = saxReader.read(classLoader.getResourceAsStream(fileName));
            Element XMLBeans = document.getRootElement();
            for (Iterator<Element> XMLBeanList = XMLBeans.elementIterator(); XMLBeanList.hasNext(); ) {
                Element element = XMLBeanList.next();
                BeanDefine bean = new BeanDefine(element.attributeValue("id"), element.attributeValue("class"));
                beanList.add(bean);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /*实例化Bean*/
    public void instancesBean() {
        for (BeanDefine bean : beanList) {
            try {
                sigletions.put(bean.getId(), Class.forName(bean.getClassName()).getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                System.out.println("实例化时失败");
                e.printStackTrace();
            }
        }
    }

    /*注解处理器*/
    public void annotationInject() {
        for (String beanName : sigletions.keySet()) {
            Object bean = sigletions.get(beanName);
            if (bean != null) {
                //处理setter注解
                this.propertyAnnotation(bean);
                //处理field注解
                this.fieldAnnotation(bean);
            }
        }
    }

    //处理在set方法处加入的注解
    public void propertyAnnotation(Object bean) {
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                //获取所有set方法
                Method setter = propertyDescriptor.getWriteMethod();
                if (setter != null && setter.isAnnotationPresent(Resource.class)) {
                    //判断set方法是否定义了注解
                    Resource resource = setter.getAnnotation(Resource.class);
                    String name = "";
                    Object value = null;
                    resource.name();
                    if (!"".equals(resource.name())) {
                        //使用了参数, 获得参数
                        name = resource.name();
                        value = sigletions.get(name);
                    } else {
                        //如果没有使用参数,则根据类型进行匹配
                        for (String key : sigletions.keySet()) {
                            //判断当前属性所属的类型是否在配置文件中存在
                            if (propertyDescriptor.getPropertyType().isAssignableFrom(sigletions.get(key).getClass())) {
                                value = sigletions.get(key);
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

    public void fieldAnnotation(Object bean) {
        try {
            //获取其全部的字段描述
            Field[] fields = bean.getClass().getFields();
            for (Field field : fields) {
                if (field != null && field.isAnnotationPresent(Resource.class)) {
                    Resource resource = field.getAnnotation(Resource.class);
                    String name = "";
                    Object value = null;
                    if (!"".equals(resource.name())) {
                        //使用了参数
                        name = resource.name();
                        value = sigletions.get(name);
                    } else {
                        //未使用参数, 使用类型匹配
                        for (String key : sigletions.keySet()) {
                            //判断当前属性是否在配置文件中存在
                            if (field.getType().isAssignableFrom(sigletions.get(key).getClass())) {
                                value = sigletions.get(key);
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
        return sigletions.get(beanId);
    }





}
