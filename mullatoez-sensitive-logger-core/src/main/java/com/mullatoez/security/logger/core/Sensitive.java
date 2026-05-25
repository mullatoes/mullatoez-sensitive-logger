package com.mullatoez.security.logger.core;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT
})
public @interface Sensitive {

    SensitiveMode mode() default SensitiveMode.DEFAULT;
}