import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//保留到运行时
@Retention(RetentionPolicy.RUNTIME)
//注解适用的地方
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Resource {
    //注解的name属性
    public String id() default "";
}
