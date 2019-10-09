package org.lorislab.quarkus.jel.jpa.service;

public interface EntityService {

    default Class getEntityClass() {
        return null;
    }

    default String getEntityName() {
        return null;
    }
}
