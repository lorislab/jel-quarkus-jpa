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
package org.lorislab.quarkus.jel.jpa.model;

import javax.persistence.*;
import java.util.UUID;

/**
 * String GUID Persistent entity implementation.
 * <p>
 * The implementation class for Persistent interface.
 *
 * @author Andrej Petras
 */
@MappedSuperclass
public class Persistent {

    /**
     * The persisted flag.
     */
    @Transient
    public boolean persisted;

    /**
     * The version attribute.
     */
    @Version
    @Column(name = "C_OPLOCK")
    public Integer version;

    /**
     * The entity life-cycle method.
     */
    @PostPersist
    @PostLoad
    @PostUpdate
    public void checkPersistentState() {
        this.persisted = true;
    }

    /**
     * The GUID of entity.
     */
    @Id
    @Column(name = "C_GUID")
    public String guid = UUID.randomUUID().toString();

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        int code = 0;
        if (guid != null) {
            result = guid.hashCode();
        }
        result = prime * result + code;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Persistent other = (Persistent) obj;
        if (guid == null) {
            return other.guid == null;
        } else return guid.equals(other.guid);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + this.guid;
    }

}
