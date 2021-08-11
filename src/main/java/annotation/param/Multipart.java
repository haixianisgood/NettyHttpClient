package annotation.param;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
/**
 * 把被注解的文件写入到content中，用于注解文件“File”
 */
public @interface Multipart {
    String value() default "";
}
