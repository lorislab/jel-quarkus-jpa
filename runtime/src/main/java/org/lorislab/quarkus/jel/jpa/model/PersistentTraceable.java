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

import java.util.Date;

import javax.persistence.*;

/**
 * Traceable persistent entity implementation.
 * <p>
 * The implementation class for Persistent interface.
 *
 * @author Andrej Petras
 */
@SuppressWarnings("squid:S2160")
@MappedSuperclass
@EntityListeners(TraceableListener.class)
public class PersistentTraceable extends Persistent {

    /**
     * The UID for this class.
     */
    private static final long serialVersionUID = 1942935094847270053L;

    /**
     * The creation date.
     */
    @Column(name = "C_CREATIONDATE")
    @Temporal(TemporalType.TIMESTAMP)
    public Date creationDate;
    /**
     * The creation user.
     */
    @Column(name = "C_CREATIONUSER")
    public String creationUser;
    /**
     * The modification user.
     */
    @Column(name = "C_MODIFICATIONUSER")
    public String modificationUser;
    /**
     * The modification date.
     */
    @Column(name = "C_MODIFICATIONDATE")
    @Temporal(TemporalType.TIMESTAMP)
    public Date modificationDate;

}