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
public class ServiceException extends Exception implements Serializable {

    /**
     * The UID for this class.
     */
    private static final long serialVersionUID = -4874552216768714025L;

    /**
     * The key of resource.
     */
    private final Enum key;

    /**
     * The arguments for the message.
     */
    private final List<Object> parameters = new ArrayList<>();

    private final Map<String, Object> namedParameters = new HashMap<>();

    private boolean stackTraceLog = false;

    /**
     * The constructor with the resource key and cause.
     *
     * @param key           the resource key.
     * @param parameters    the resource key arguments.
     * @param stackTraceLog the stack trace log flag.
     * @param cause         the throw able cause.
     */
    public ServiceException(boolean stackTraceLog, final Enum key, final Throwable cause, Serializable... parameters) {
        super(cause);
        this.key = key;
        this.stackTraceLog = stackTraceLog;
        if (parameters != null && parameters.length > 0) {
            this.parameters.addAll(Arrays.asList(parameters));
        }
    }

    /**
     * The constructor with the resource key and cause.
     *
     * @param key        the resource key.
     * @param parameters the resource key arguments.
     * @param cause      the throw able cause.
     */
    public ServiceException(final Enum key, final Throwable cause, Serializable... parameters) {
        this(false, key, cause, parameters);
    }

    /**
     * The constructor with the resource key and cause.
     *
     * @param key             the resource key.
     * @param parameters      the resource key arguments.
     * @param cause           the throw able cause.
     * @param namedParameters the named parameters.
     */
    public ServiceException(final Enum key, final Throwable cause, List<Object> parameters, Map<String, Object> namedParameters) {
        super(cause);
        this.key = key;
        addParameters(parameters);
        addNamedParameters(namedParameters);
    }

    /**
     * Gets the key of resource.
     *
     * @return the key of resource.
     */
    public final Enum<?> getKey() {
        return key;
    }

    /**
     * Gets the arguments of the message.
     *
     * @return the arguments of the message.
     */
    public final List<Object> getParameters() {
        return parameters;
    }

    public final void addParameter(Object parameter) {
        parameters.add(parameter);
    }

    public final void addParameters(List<Object> parameters) {
        if (parameters != null) {
            this.parameters.addAll(parameters);
        }
    }

    public final void addNamedParameter(String name, Object parameter) {
        namedParameters.put(name, parameter);
    }

    public final void addNamedParameters(Map<String, Object> namedParameters) {
        if (namedParameters != null) {
            this.namedParameters.putAll(namedParameters);
        }
    }

    public Map<String, Object> getNamedParameters() {
        return namedParameters;
    }

    public boolean isStackTraceLog() {
        return stackTraceLog;
    }

    public void setStackTraceLog(boolean stackTraceLog) {
        this.stackTraceLog = stackTraceLog;
    }

}
