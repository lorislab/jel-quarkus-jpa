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

package org.lorislab.quarkus.jel.jpa.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.*;
import org.lorislab.quarkus.jel.jpa.service.AbstractEntityService;

import java.util.List;


public class JPABuild {

    private static final String FEATURE_NAME = "jel-jpa";

    private static final DotName DOTNAME_REPOSITORY = DotName.createSimple(AbstractEntityService.class.getName());

    private static final DotName ENTITY = DotName.createSimple("javax.persistence.Entity");

    private static final String ATTRIBUTE_NAME = "name";


    @BuildStep
    FeatureBuildItem createFeatureItem() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void build(CombinedIndexBuildItem index, BuildProducer<BytecodeTransformerBuildItem> transformers) throws Exception {

        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_REPOSITORY)) {
            if (classInfo.superClassType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
                Type entity = classInfo.superClassType().asParameterizedType().arguments().get(0);
                ClassInfo ec = index.getIndex().getClassByName(entity.name());

                String name = entity.name().withoutPackagePrefix();

                if (ec.annotations() != null) {
                    List<AnnotationInstance> annotations = ec.annotations().get(ENTITY);
                    if (annotations != null && !annotations.isEmpty()) {
                        AnnotationInstance annotationInstance = annotations.get(0);
                        for (AnnotationValue a : annotationInstance.values()) {
                            if (ATTRIBUTE_NAME.equals(a.name())) {
                                name = a.asString();
                            }
                        }
                    }
                }

                transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), new AbstractEntityBuilderEnhancer(name, entity.name().toString())));
            }
        }

    }


}
