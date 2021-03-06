package annotation.param;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE_PARAMETER})
/**
 * HTTP请求参数
 */
public @interface RequestParam {
    String value() default "";
}
