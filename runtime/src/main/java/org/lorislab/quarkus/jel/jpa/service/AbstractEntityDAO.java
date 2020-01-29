/*
 * Copyright 2019 lorislab.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lorislab.quarkus.jel.jpa.service;

import org.hibernate.exception.ConstraintViolationException;
import org.lorislab.quarkus.jel.jpa.exception.ConstraintDAOException;
import org.lorislab.quarkus.jel.jpa.exception.DAOException;
import org.lorislab.quarkus.jel.jpa.model.Persistent;
import org.lorislab.quarkus.jel.jpa.model.Persistent_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.*;

/**
 * The abstract EAO service class using an entity type.
 *
 * @param <T> the entity class.
 */
@Transactional(value = Transactional.TxType.NOT_SUPPORTED, rollbackOn = DAOException.class)
public abstract class AbstractEntityDAO<T extends Persistent> implements EntityDAO {

    private static final Logger log = LoggerFactory.getLogger(AbstractEntityDAO.class);

    /**
     * The property hint is javax.persistence.fetchgraph.
     * <p>
     * This hint will treat all the specified attributes in the Entity Graph as
     * FetchType.EAGER. Attributes that are not specified are treated as
     * FetchType.LAZY.
     */
    private static final String HINT_LOAD_GRAPH = "javax.persistence.loadgraph";

    /**
     * The entity manager.
     */
    @Inject
    public EntityManager em;

    /**
     * The entity class.
     */
    protected Class<T> entityClass;

    /**
     * The entity name.
     */
    protected String entityName;

    /**
     * Initialize the entity service bean.
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        String serviceClass = getClass().getName();
        entityClass = getEntityClass();
        entityName = getEntityName();
        log.debug("Initialize the entity service {} for entity {}/{}", serviceClass, entityClass, entityName);
    }

    /**
     * Gets all entities.
     *
     * @return the list of all entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = DAOException.class)
    protected List<T> find() {
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            cq.from(entityClass);
            TypedQuery<T> query = em.createQuery(cq);
            return query.getResultList();
        } catch (Exception e) {
            throw new DAOException(EntityServiceErrors.FIND_ALL_ENTITIES_FAILED, e);
        }
    }

    /**
     * Finds the list of object by GUIDs.
     *
     * @param guids the set of GUIDs.
     * @return the corresponding list of entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = DAOException.class)
    public List<T> find(List<String> guids) {
        if (guids != null && !guids.isEmpty()) {
            try {
                CriteriaQuery<T> cq = createCriteriaQuery();
                cq.where(cq.from(entityClass).get(Persistent_.GUID).in(guids));
                return em.createQuery(cq).getResultList();
            } catch (Exception e) {
                throw new DAOException(EntityServiceErrors.FAILED_TO_GET_ENTITY_BY_GUIDS, e, entityName);
            }
        }
        return Collections.emptyList();
    }


    /**
     * Finds all entities in the corresponding interval.
     *
     * @param from  the from index.
     * @param count the count index.
     * @return the corresponding list of the entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = DAOException.class)
    public List<T> find(Integer from, Integer count) {
        try {
            CriteriaQuery<T> cq = createCriteriaQuery();
            cq.from(entityClass);
            TypedQuery<T> query = em.createQuery(cq);
            if (from != null) {
                query.setFirstResult(from);
            }
            if (count != null) {
                if (from != null) {
                    query.setMaxResults(from + count);
                } else {
                    query.setMaxResults(count);
                }
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new DAOException(EntityServiceErrors.FAILED_TO_GET_ALL_ENTITIES, e, entityName, from, count);
        }
    }

    /**
     * Gets the entity by id.
     *
     * @param guid the entity GUID.
     * @return the entity corresponding to the GUID.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = DAOException.class)
    public T findBy(final String guid) {
        try {
            return em.find(entityClass, guid);
        } catch (Exception e) {
            throw new DAOException(EntityServiceErrors.FIND_ENTITY_BY_ID_FAILED, e);
        }
    }

    /**
     * Updates the entity.
     *
     * @param entity the entity.
     * @return the updated entity.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public T update(T entity) {
        return update(entity, false);
    }

    /**
     * Updates the entity.
     *
     * @param entity the entity.
     * @param flush flush flag
     * @return the updated entity.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public T update(T entity, boolean flush) {
        try {
            T result = em.merge(entity);
            if (flush) {
                em.flush();
            }
            return result;
        } catch (Exception e) {
            throw handleConstraint(e, EntityServiceErrors.MERGE_ENTITY_FAILED);
        }
    }

    /**
     * Updates the entities.
     *
     * @param entities the list of entities.
     * @return the list of updated entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public List<T> update(List<T> entities) {
        return update(entities, false);
    }

    /**
     * Updates the entities.
     *
     * @param entities the list of entities.
     * @param flush flush flag
     * @return the list of updated entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public List<T> update(List<T> entities, boolean flush) {
        if (entities != null) {
            try {
                final List<T> result = new ArrayList<>(entities.size());
                entities.forEach(e -> result.add(em.merge(e)));
                if (flush) {
                    em.flush();
                }
                return result;
            } catch (Exception e) {
                throw handleConstraint(e, EntityServiceErrors.MERGE_ENTITY_FAILED);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Creates the entity.
     *
     * @param entity the entity.
     * @return the created entity.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public T create(T entity) {
        return create(entity, false);
    }

    /**
     * Creates the entity.
     *
     * @param entity the entity.
     * @param flush flush flag
     * @return the created entity.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public T create(T entity, boolean flush) {
        try {
            em.persist(entity);
            if (flush) {
                em.flush();
            }
        } catch (Exception e) {
            throw handleConstraint(e, EntityServiceErrors.PERSIST_ENTITY_FAILED);
        }
        return entity;
    }

    /**
     * Creates the entities.
     *
     * @param entities the list of enties.
     * @return list of created entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public List<T> create(List<T> entities) {
        return create(entities, false);
    }

    /**
     * Creates the entities.
     *
     * @param entities the list of entities.
     * @param flush flush flag
     * @return list of created entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public List<T> create(List<T> entities, boolean flush) {
        if (entities != null) {
            try {
                entities.forEach(em::persist);
                if (flush) {
                    em.flush();
                }
            } catch (Exception e) {
                throw handleConstraint(e, EntityServiceErrors.PERSIST_ENTITY_FAILED);
            }
        }
        return entities;
    }

    /**
     * Performs persist followed by flush.
     *
     * @param entity the entity.
     */
    protected void refresh(T entity) {
        em.refresh(entity);
    }

    /**
     * Deletes the entity.
     *
     * @param entity the entity.
     * @return <code>true</code> if the entity was correctly deleted.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public boolean delete(T entity) {
        return delete(entity, false);
    }

    /**
     * Deletes the entity.
     *
     * @param entity the entity.
     * @param flush flush flag
     * @return <code>true</code> if the entity was correctly deleted.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public boolean delete(T entity, boolean flush) {
        try {
            if (entity != null) {
                em.remove(entity);
                if (flush) {
                    em.flush();
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw handleConstraint(e, EntityServiceErrors.DELETE_ENTITY_FAILED);
        }
    }

    /**
     * Performs delete operation on a list of entities. false is returned if one
     * object fails to be deleted.
     *
     * @param entities the list of entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public void delete(List<T> entities) {
        delete(entities, false);
    }

    /**
     * Performs delete operation on a list of entities. false is returned if one
     * object fails to be deleted.
     *
     * @param entities the list of entities.
     * @param flush flush flag
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public void delete(List<T> entities, boolean flush) {
        try {
            if (entities != null && !entities.isEmpty()) {
                entities.forEach(e -> em.remove(e));
                if (flush) {
                    em.flush();
                }
            }
        } catch (Exception e) {
            throw handleConstraint(e, EntityServiceErrors.DELETE_ENTITIES_FAILED);
        }
    }

    /**
     * Creates the named query.
     *
     * @param namedQuery the named query.
     * @param parameters the map of parameters.
     * @return the query.
     */
    protected Query createNamedQuery(String namedQuery, Map<String, Object> parameters) {
        Query query = em.createNamedQuery(namedQuery);
        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach(query::setParameter);
        }
        return query;
    }

    /**
     * Lock Entity in EntityManager.
     *
     * @param entity   the entity
     * @param lockMode the lock mode
     */
    protected void lock(T entity, LockModeType lockMode) {
        em.lock(entity, lockMode);
    }


    /**
     * Performs delete operation on a list of entities. false is returned if one
     * object fails to be deleted.
     *
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public void delete() {
        try {
            List<T> tmp = find();
            if (tmp != null && !tmp.isEmpty()) {
                delete(tmp);
            }
        } catch (Exception e) {
            throw new DAOException(EntityServiceErrors.FAILED_TO_DELETE_ALL, e, entityName);
        }
    }

    /**
     * Removes all entities. Check on existence is made.
     *
     * @return the number of deleted entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public int deleteQuery() {
        return deleteQuery(false);
    }

    /**
     * Removes all entities. Check on existence is made.
     *
     * @param flush flush flag
     * @return the number of deleted entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public int deleteQuery(boolean flush) {
        try {
            CriteriaQuery<T> cq = createCriteriaQuery();
            cq.from(entityClass);
            int result = em.createQuery(cq).executeUpdate();
            if (flush) {
                em.flush();
            }
            return result;
        } catch (Exception e) {
            throw handleConstraint(e, EntityServiceErrors.FAILED_TO_DELETE_ALL_QUERY);
        }
    }

    /**
     * Removes an entity by GUID. Check on existence is made.
     *
     * @param guid the GUID of the entity
     * @return true if removed.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public boolean deleteQuery(String guid) {
        return deleteQuery(guid, false);
    }

    /**
     * Removes an entity by GUID. Check on existence is made.
     *
     * @param guid the GUID of the entity
     * @param flush flush flag
     * @return true if removed.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public boolean deleteQuery(String guid, boolean flush) {
        if (guid != null) {
            try {
                CriteriaDelete<T> cq = createDeleteQuery();
                cq.where(
                        em.getCriteriaBuilder()
                                .equal(cq.from(entityClass).get(Persistent_.GUID), guid)
                );
                int count = em.createQuery(cq).executeUpdate();
                if (flush) {
                    em.flush();
                }
                return count == 1;
            } catch (Exception e) {
                throw handleConstraint(e, EntityServiceErrors.FAILED_TO_DELETE_BY_GUID_QUERY);
            }
        }
        return false;
    }

    /**
     * Removes entities by GUIDs. Check on existence is made.
     *
     * @param guids the set of GUIDs.
     * @return the number of deleted entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public int deleteQuery(List<String> guids) {
        return deleteQuery(guids, false);
    }

    /**
     * Removes entities by GUIDs. Check on existence is made.
     *
     * @param guids the set of GUIDs.
     * @param flush flush flag
     * @return the number of deleted entities.
     * @throws DAOException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = DAOException.class)
    public int deleteQuery(List<String> guids, boolean flush) {
        try {
            if (guids != null && !guids.isEmpty()) {
                CriteriaDelete<T> cq = createDeleteQuery();
                cq.where(cq.from(entityClass).get(Persistent_.GUID).in(guids));
                int result = em.createQuery(cq).executeUpdate();
                if (flush) {
                    em.flush();
                }
                return result;
            }
        } catch (Exception e) {
            throw handleConstraint(e, EntityServiceErrors.FAILED_TO_DELETE_ALL_BY_GUIDS_QUERY);
        }
        return 0;
    }

    /**
     * Loads all entities.
     *
     * @param entityGraph the entity graph.
     * @return the list loaded entities.
     * @throws DAOException if the method fails.
     */
    protected List<T> load(EntityGraph<?> entityGraph) {
        try {
            CriteriaQuery<T> cq = createCriteriaQuery();
            cq.from(entityClass);
            cq.distinct(true);
            TypedQuery<T> query = em.createQuery(cq);
            if (entityGraph != null) {
                query.setHint(HINT_LOAD_GRAPH, entityGraph);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new DAOException(EntityServiceErrors.FAILED_TO_LOAD_ALL_ENTITIES, e, entityName, entityGraph == null ? null : entityGraph.getName());
        }
    }

    /**
     * Loads all entities.
     *
     * @param guids       the set of GUIDs.
     * @param entityGraph the entity graph.
     * @return the list loaded entities.
     * @throws DAOException if the method fails.
     */
    protected List<T> load(List<String> guids, EntityGraph<?> entityGraph) {
        List<T> result = null;
        try {
            if (guids != null && !guids.isEmpty()) {
                CriteriaQuery<T> cq = createCriteriaQuery();
                cq.where(cq.from(entityClass).get(Persistent_.GUID).in(guids));
                TypedQuery<T> query = em.createQuery(cq);
                if (entityGraph != null) {
                    query.setHint(HINT_LOAD_GRAPH, entityGraph);
                }
                result = query.getResultList();
            }
        } catch (Exception e) {
            throw new DAOException(EntityServiceErrors.FAILED_TO_LOAD_GUIDS_ENTITIES, e, entityName, entityGraph == null ? null : entityGraph.getName());
        }
        return result;
    }

    /**
     * Loads the entity by GUID and entity graph name.
     *
     * @param guid        the GUID.
     * @param entityGraph the entity graph.
     * @return the entity.
     * @throws DAOException if the method fails.
     */
    protected T load(String guid, EntityGraph<?> entityGraph) {
        if (guid != null) {
            try {
                Map<String, Object> properties = new HashMap<>();
                if (entityGraph != null) {
                    properties.put(HINT_LOAD_GRAPH, entityGraph);
                }
                return em.find(entityClass, guid, properties);
            } catch (Exception e) {
                throw new DAOException(EntityServiceErrors.FAILED_TO_LOAD_ENTITY_BY_GUID, e, entityName, guid, entityGraph == null ? null : entityGraph.getName());
            }
        }
        return null;
    }

    /**
     * Handle the JPA constraint exception.
     *
     * @param ex  the exception.
     * @param key the error key.
     * @return the corresponding service exception.
     */
    protected DAOException handleConstraint(Exception ex, Enum<?> key) {
        if (ex instanceof ConstraintDAOException) {
            return (DAOException) ex;
        }
        if (ex.getCause() instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) ex.getCause();
            throw new ConstraintDAOException(cve.getConstraintName(), key, ex, entityName);
        }
        return new DAOException(key, ex, entityName);
    }

    protected CriteriaQuery<T> createCriteriaQuery() {
        return this.em.getCriteriaBuilder().createQuery(this.entityClass);
    }

    protected CriteriaDelete<T> createDeleteQuery() {
        return em.getCriteriaBuilder().createCriteriaDelete(entityClass);
    }

    protected CriteriaUpdate<T> createUpdateQuery() {
        return em.getCriteriaBuilder().createCriteriaUpdate(entityClass);
    }

    /**
     * @author Andrej Petras
     */
    private enum EntityServiceErrors {
        FAILED_TO_GET_ALL_ENTITIES,
        FAILED_TO_GET_ENTITY_BY_GUIDS,
        FAILED_TO_DELETE_ALL,
        FAILED_TO_DELETE_ALL_QUERY,
        FAILED_TO_DELETE_BY_GUID_QUERY,
        FAILED_TO_DELETE_ALL_BY_GUIDS_QUERY,
        FAILED_TO_LOAD_ALL_ENTITIES,
        FAILED_TO_LOAD_GUIDS_ENTITIES,
        FAILED_TO_LOAD_ENTITY_BY_GUID,
        PERSIST_ENTITY_FAILED,
        MERGE_ENTITY_FAILED,
        DELETE_ENTITY_FAILED,
        DELETE_ENTITIES_FAILED,
        FIND_ENTITY_BY_ID_FAILED,
        FIND_ALL_ENTITIES_FAILED,
        ;
    }
}
