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

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.lorislab.quarkus.jel.jpa.model.Persistent;
import org.lorislab.quarkus.jel.jpa.model.PersistentTraceable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;


import java.beans.Transient;
import java.lang.reflect.Modifier;

public class PersistentModelEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {

    static final DotName DOT_NAME_PERSISTENT = DotName.createSimple(Persistent.class.getName());
    static final DotName DOT_NAME_PERSISTENT_TRACEABLE = DotName.createSimple(PersistentTraceable.class.getName());

    private static final DotName DOT_NAME_TRANSIENT = DotName.createSimple(Transient.class.getName());

    public PersistentModelEnhancer(IndexView index) {
        super(index, DOT_NAME_PERSISTENT);
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheJpaEntityClassVisitor(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo);
    }

    static class PanacheJpaEntityClassVisitor extends PanacheEntityClassVisitor<EntityField> {

        public PanacheJpaEntityClassVisitor(String className, ClassVisitor outputClassVisitor,
                                            MetamodelInfo<EntityModel<EntityField>> modelInfo, ClassInfo panacheEntityBaseClassInfo) {
            super(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo);
        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            mv.visitLdcInsn(thisClass);
        }

        @Override
        protected String getModelDescriptor() {
            return "Ljava/lang/Class;";
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return null;
        }

        @Override
        protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    thisClass.getInternalName(),
                    EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + field.name,
                    Type.getMethodDescriptor(Type.getType(void.class), Type.getType(field.descriptor)),
                    false);
        }

        @Override
        protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    thisClass.getInternalName(),
                    EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + field.name,
                    Type.getMethodDescriptor(Type.getType(field.descriptor)),
                    false);
        }
    }

    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOT_NAME_TRANSIENT)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }
}