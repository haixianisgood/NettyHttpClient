package annotation.mapping;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
/**
 * 请求路径映射，被标记的接口类，都会以该路径为父路径
 */
public @interface RequestMapping {
    String value() default "";
}
