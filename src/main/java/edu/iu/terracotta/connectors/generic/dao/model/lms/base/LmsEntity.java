package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

public interface LmsEntity<T> {

    T from();
    Class<?> getType();

}
