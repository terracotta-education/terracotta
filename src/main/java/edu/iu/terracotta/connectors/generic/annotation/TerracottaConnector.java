package edu.iu.terracotta.connectors.generic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TerracottaConnector {

    LmsConnector value();

}
