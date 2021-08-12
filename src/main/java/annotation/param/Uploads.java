package annotation.param;

import java.lang.annotation.*;

/**
 * 把被注解的文件写入到content中，用于注解文件“File”
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Uploads {
    String[] value() default {};
}
