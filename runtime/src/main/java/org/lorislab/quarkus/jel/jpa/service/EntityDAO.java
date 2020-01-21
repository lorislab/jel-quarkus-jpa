package org.lorislab.quarkus.jel.jpa.service;

public interface EntityDAO {

    default Class getEntityClass() {
        return null;
    }

    default String getEntityName() {
        return null;
    }
}
