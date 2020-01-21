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
package org.lorislab.quarkus.jel.jpa.exception;

import java.io.Serializable;
import java.util.*;

/**
 * The exception class for service exception with localized message.
 *
 * @author Andrej Petras
 */
public class DAOException extends Exception {

    /**
     * The key of resource.
     */
    public final Enum key;

    /**
     * The arguments for the message.
     */
    public final List<Object> parameters = new ArrayList<>();

    public final Map<String, Object> namedParameters = new HashMap<>();

    /**
     * The constructor with the resource key and cause.
     *
     * @param key           the resource key.
     * @param parameters    the resource key arguments.
     * @param cause         the throw able cause.
     */
    public DAOException(final Enum key, final Throwable cause, Serializable... parameters) {
        super(cause);
        this.key = key;
        if (parameters != null && parameters.length > 0) {
            this.parameters.addAll(Arrays.asList(parameters));
        }
    }

    public final void addParameter(Object parameter) {
        parameters.add(parameter);
    }

    public final void addParameter(List<Object> parameters) {
        if (parameters != null) {
            this.parameters.addAll(parameters);
        }
    }

    public final void addParameter(String name, Object parameter) {
        if (name != null) {
            namedParameters.put(name, parameter);
        }
    }

    public final void addParameter(Map<String, Object> namedParameters) {
        if (namedParameters != null) {
            this.namedParameters.putAll(namedParameters);
        }
    }

    @Override
    public String toString() {
        return key.name();
    }
}
