package annotation.param;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE_PARAMETER})
/**
 * 请求实体，把对象序列化后再请求报文中。
 */
public @interface RequestBody {
    String value() default "";
}
