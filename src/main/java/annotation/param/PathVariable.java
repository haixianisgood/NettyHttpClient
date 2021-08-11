package annotation.param;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE_PARAMETER, })
/**
 * 路径参数
 */
public @interface PathVariable {
    String value() default "";
}
