/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.models.Extensible;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ExtensibleImpl<T extends Extensible<T>> implements Extensible<T> {

    protected Map<String, Object> extensions = new HashMap<>();

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T addExtension(String name, Object value) {
        extensions.put(name, value);
        return (T) this;
    }

    @Override
    public void removeExtension(String name) {
        extensions.remove(name);
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public static void merge(Extension from, Extensible<?> to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (to.getExtensions() == null) {
            to.setExtensions(new LinkedHashMap<>());
        }
        if (from.name() != null && !from.name().isEmpty()) {
            Object value = mergeProperty(to.getExtensions().get(from.name()), 
                    convertExtensionValue(from.value(), from.parseValue()), override);
            to.getExtensions().put(from.name(), value);
        }
    }

    public static Object convertExtensionValue(String value, boolean parseValue) {
        if (value == null) {
            return null;
        }
        if (parseValue) {
            if (value.startsWith("{") || value.startsWith("[")) { // JSON object + array
                try {
                    return new ObjectMapper().readTree(value);
                } catch (Exception e) {
                    return value;
                }
            }
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.valueOf(value);
            }
            // must be number
            if (value.indexOf('.') >= 0) {
                return Double.valueOf(value);
            }
            return Long.valueOf(value);
        }
        // Could be an array
        if (value.contains(",")) {
            // Remove leading and trailing brackets, then parse to an array
            String[] possibleArray = value.replaceAll("^[\\[\\{\\(]", "").replaceAll("[\\]\\}\\)]$", "").split(",");

            if (possibleArray.length > 1) {
                return possibleArray;
            }
        }
        return value;
    }

}