package annotation.method;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Post {
    String value() default "";
    boolean multipart() default false;
}
