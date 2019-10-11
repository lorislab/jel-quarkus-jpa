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

import org.lorislab.quarkus.jel.jpa.exception.ConstraintException;
import org.lorislab.quarkus.jel.jpa.exception.ServiceException;
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
 * @author Andrej Petras
 */
@Transactional(value = Transactional.TxType.NOT_SUPPORTED, rollbackOn = ServiceException.class)
public abstract class AbstractEntityService<T extends Persistent> implements EntityService {

    private static final Logger log = LoggerFactory.getLogger(AbstractEntityService.class);

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
     * The load entity graph.
     */
    private EntityGraph<? super T> loadEntityGraph;

    /**
     * The criteria builder.
     */
    protected CriteriaBuilder cb;

    /**
     * Initialize the entity service bean.
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        String serviceClass = getClass().getName();
        entityClass = getEntityClass();
        entityName = getEntityName();
        log.info("Initialize the entity service {} for entity {}/{}", serviceClass, entityClass, entityName);

        EntityGraph<? super T> graph = null;
        if (entityClass != null) {
            String loadName = entityName + ".load";

            List<EntityGraph<? super T>> graphs = em.getEntityGraphs(entityClass);
            if (graphs != null) {
                for (int i = 0; i < graphs.size() && graph == null; i++) {
                    if (graphs.get(i).getName().equals(loadName)) {
                        graph = graphs.get(i);
                    }
                }
            }
        }
        loadEntityGraph = graph;
        cb = em.getCriteriaBuilder();
    }

    /**
     * Gets all entities.
     *
     * @return the list of all entities.
     * @throws ServiceException if the method fails.
     */
    @SuppressWarnings("unchecked")
    protected List<T> findAll() throws ServiceException {
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            cq.from(entityClass);
            TypedQuery<T> query = em.createQuery(cq);
            return query.getResultList();
        } catch (Exception e) {
            throw new ServiceException(EntityServiceErrors.FIND_ALL_ENTITIES_FAILED, e);
        }
    }

    /**
     * Gets the entity by id.
     *
     * @param guid the entity GUID.
     * @return the entity corresponding to the GUID.
     * @throws ServiceException if the method fails.
     */
    @SuppressWarnings("unchecked")
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = ServiceException.class)
    public T findByGuid(final String guid) throws ServiceException {
        try {
            return em.find(entityClass, guid);
        } catch (Exception e) {
            throw new ServiceException(EntityServiceErrors.FIND_ENTITY_BY_ID_FAILED, e);
        }
    }

    /**
     * Updates the entity.
     *
     * @param entity the entity.
     * @return the updated entity.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public T update(T entity) throws ServiceException {
        try {
            T result = em.merge(entity);
            em.flush();
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
     * @throws ServiceException if the method fails.
     */
    @SuppressWarnings("unchecked")
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public List<T> update(List<T> entities) throws ServiceException {
        if (entities != null) {
            try {
                final List<T> result = new ArrayList<>(entities.size());
                entities.forEach(e -> result.add(em.merge(e)));
                em.flush();
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
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public T create(T entity) throws ServiceException {
        try {
            em.persist(entity);
            em.flush();
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
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public List<T> create(List<T> entities) throws ServiceException {
        if (entities != null) {
            try {
                entities.forEach(em::persist);
                em.flush();
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
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public boolean delete(T entity) throws ServiceException {
        try {
            if (entity != null) {
                em.remove(entity);
                em.flush();
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
     * @return the delete flag.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public boolean deleteAll(List<T> entities) throws ServiceException {
        try {
            if (entities != null && !entities.isEmpty()) {
                entities.forEach(e -> em.remove(e));
                em.flush();
                return true;
            }
            return false;
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
     * Finds the list of object by GUIDs.
     *
     * @param guids the set of GUIDs.
     * @return the corresponding list of entities.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public List<T> findByGuid(List<String> guids) throws ServiceException {
        List<T> result = null;
        if (guids != null && !guids.isEmpty()) {
            try {
                CriteriaQuery<T> cq = criteriaQuery();
                cq.where(cq.from(entityClass).get(Persistent_.guid).in(guids));
                result = em.createQuery(cq).getResultList();
            } catch (Exception e) {
                throw new ServiceException(EntityServiceErrors.FAILED_TO_GET_ENTITY_BY_GUIDS, e, entityName);
            }
        }
        return result;
    }

    /**
     * Performs delete operation on a list of entities. false is returned if one
     * object fails to be deleted.
     *
     * @param entities the list of entities.
     * @return {@code true} if all entities removed.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public int delete(List<T> entities) throws ServiceException {
        int result = 0;
        try {
            if (entities != null && !entities.isEmpty()) {
                for (int i = 0; i < entities.size(); i++) {
                    if (this.delete(entities.get(i))) {
                        result = result + 1;
                    }
                }
            }
        } catch (Exception e) {
            throw new ServiceException(EntityServiceErrors.FAILED_TO_DELETE_ENTITY, e, entityName);
        }
        return result;
    }

    /**
     * Performs delete operation on a list of entities. false is returned if one
     * object fails to be deleted.
     *
     * @return {@code true} if all entities removed.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public int deleteAll() throws ServiceException {
        int result = 0;
        try {
            List<T> tmp = findAll();
            if (tmp != null && !tmp.isEmpty()) {
                result = delete(tmp);
            }
        } catch (Exception e) {
            throw new ServiceException(EntityServiceErrors.FAILED_TO_DELETE_ALL, e, entityName);
        }
        return result;
    }

    /**
     * Finds all entities in the corresponding interval.
     *
     * @param from  the from index.
     * @param count the count index.
     * @return the corresponding list of the entities.
     * @throws ServiceException if the method fails.
     */
    public List<T> find(Integer from, Integer count) throws ServiceException {
        try {
            CriteriaQuery<T> cq = criteriaQuery();
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
            throw new ServiceException(EntityServiceErrors.FAILED_TO_GET_ALL_ENTITIES, e, entityName, from, count);
        }
    }

    /**
     * Removes all entities. Check on existence is made.
     *
     * @return the number of deleted entities.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public int deleteQueryAll() throws ServiceException {
        try {
            CriteriaQuery<T> cq = criteriaQuery();
            cq.from(entityClass);
            int result = em.createQuery(cq).executeUpdate();
            em.flush();
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
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public boolean deleteByGuid(String guid) throws ServiceException {
        if (guid != null) {
            try {
                CriteriaDelete<T> cq = deleteQuery();
                cq.where(
                        em.getCriteriaBuilder()
                                .equal(cq.from(entityClass).get(Persistent_.guid), guid)
                );
                int count = em.createQuery(cq).executeUpdate();
                em.flush();
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
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public int deleteByGuid(List<String> guids) throws ServiceException {
        try {
            if (guids != null && !guids.isEmpty()) {
                CriteriaDelete<T> cq = deleteQuery();
                cq.where(cq.from(entityClass).get(Persistent_.guid).in(guids));
                int result = em.createQuery(cq).executeUpdate();
                em.flush();
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
     * @return the list loaded entities.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public List<T> loadAll() throws ServiceException {
        return loadAll(loadEntityGraph);
    }

    /**
     * Loads all entities.
     *
     * @param entityGraph the entity graph.
     * @return the list loaded entities.
     * @throws ServiceException if the method fails.
     */
    protected List<T> loadAll(EntityGraph<?> entityGraph) throws ServiceException {
        try {
            CriteriaQuery<T> cq = criteriaQuery();
            cq.from(entityClass);
            cq.distinct(true);
            TypedQuery<T> query = em.createQuery(cq);
            if (entityGraph != null) {
                query.setHint(HINT_LOAD_GRAPH, entityGraph);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new ServiceException(EntityServiceErrors.FAILED_TO_LOAD_ALL_ENTITIES, e, entityName, entityGraph == null ? null : entityGraph.getName());
        }
    }

    /**
     * Loads all entities.
     *
     * @param guids the set of GUIDs.
     * @return the list loaded entities.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public List<T> loadByGuid(List<String> guids) throws ServiceException {
        return loadByGuid(guids, loadEntityGraph);
    }

    /**
     * Loads all entities.
     *
     * @param guids       the set of GUIDs.
     * @param entityGraph the entity graph.
     * @return the list loaded entities.
     * @throws ServiceException if the method fails.
     */
    protected List<T> loadByGuid(List<String> guids, EntityGraph<?> entityGraph) throws ServiceException {
        List<T> result = null;
        try {
            if (guids != null && !guids.isEmpty()) {
                CriteriaQuery<T> cq = criteriaQuery();
                cq.where(cq.from(entityClass).get(Persistent_.guid).in(guids));
                TypedQuery<T> query = em.createQuery(cq);
                if (entityGraph != null) {
                    query.setHint(HINT_LOAD_GRAPH, entityGraph);
                }
                result = query.getResultList();
            }
        } catch (Exception e) {
            throw new ServiceException(EntityServiceErrors.FAILED_TO_LOAD_GUIDS_ENTITIES, e, entityName, entityGraph == null ? null : entityGraph.getName());
        }
        return result;
    }

    /**
     * Loads the entity by GUID.
     *
     * @param guid the GUID.
     * @return the entity.
     * @throws ServiceException if the method fails.
     */
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ServiceException.class)
    public T loadByGuid(String guid) throws ServiceException {
        return loadByGuid(guid, loadEntityGraph);
    }

    /**
     * Loads the entity by GUID and entity graph name.
     *
     * @param guid        the GUID.
     * @param entityGraph the entity graph.
     * @return the entity.
     * @throws ServiceException if the method fails.
     */
    protected T loadByGuid(String guid, EntityGraph<?> entityGraph) throws ServiceException {
        if (guid != null) {
            try {
                Map<String, Object> properties = new HashMap<>();
                if (entityGraph != null) {
                    properties.put(HINT_LOAD_GRAPH, entityGraph);
                }
                return em.find(entityClass, guid, properties);
            } catch (Exception e) {
                throw new ServiceException(EntityServiceErrors.FAILED_TO_LOAD_ENTITY_BY_GUID, e, entityName, guid, entityGraph == null ? null : entityGraph.getName());
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
    @SuppressWarnings("squid:S1872")
    protected ServiceException handleConstraint(Exception ex, Enum<?> key) {
        if (ex instanceof ConstraintException) {
            return (ConstraintException) ex;
        }
        if (ex instanceof PersistenceException) {
            PersistenceException e = (PersistenceException) ex;
            if (e.getCause() != null) {

                Throwable providerException = e.getCause();
                // Hibernate constraint violation exception
                if ("org.hibernate.exception.ConstraintViolationException".equals(providerException.getClass().getName())) {

                    // for the org.postgresql.util.PSQLException get the constraints message.
                    String msg = providerException.getMessage();
                    if (providerException.getCause() != null) {
                        msg = providerException.getCause().getMessage();
                        if (msg != null) {
                            msg = msg.replaceAll("\n", "");
                        }
                    }
                    // throw own constraints exception.
                    return new ConstraintException(msg, key, e, entityName);
                }
            }
        }
        return new ServiceException(key, ex, entityName);
    }

    protected CriteriaQuery<T> criteriaQuery() {
        return this.em.getCriteriaBuilder().createQuery(this.entityClass);
    }

    protected CriteriaDelete<T> deleteQuery() {
        return em.getCriteriaBuilder().createCriteriaDelete(entityClass);
    }

    protected CriteriaUpdate<T> updateQuery() {
        return em.getCriteriaBuilder().createCriteriaUpdate(entityClass);
    }

    /**
     * @author Andrej Petras
     */
    private enum EntityServiceErrors {
        FAILED_TO_GET_ALL_ENTITIES,
        FAILED_TO_GET_ENTITY_BY_GUIDS,
        FAILED_TO_DELETE_ALL,
        FAILED_TO_DELETE_ENTITY,
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
