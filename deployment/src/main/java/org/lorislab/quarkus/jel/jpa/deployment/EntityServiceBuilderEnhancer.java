package org.lorislab.quarkus.jel.jpa.deployment;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.BiFunction;

/**
 * The abstract  entity builder enhancer
 */
public class EntityServiceBuilderEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    /**
     * The entity class.
     */
    private String entityClass;

    /**
     * The entity name.
     */
    private String entityName;

    /**
     * The default constructor.
     *
     * @param entityName  entity name.
     * @param entityClass entity class.
     */
    public EntityServiceBuilderEnhancer(String entityName, String entityClass) {
        this.entityClass = entityClass;
        this.entityName = entityName;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new EntityServiceBuilderEnhancerClassVisitor(className, outputClassVisitor, entityName, entityClass);
    }

    /**
     * The entity builder enhancer class visitor.
     */
    static class EntityServiceBuilderEnhancerClassVisitor extends ClassVisitor {
        /**
         * The entity class.
         */
        private String entityClass;
        /**
         * The entity name.
         */
        private String entityName;

        public EntityServiceBuilderEnhancerClassVisitor(String className, ClassVisitor outputClassVisitor, String entityName, String entityClass) {
            super(Opcodes.ASM7, outputClassVisitor);
            this.entityClass = entityClass.replace('.', '/');
            this.entityName = entityName;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public void visitEnd() {

            MethodVisitor mv1 = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                    "getEntityClass",
                    "()Ljava/lang/Class;",
                    null,
                    null);
            mv1.visitCode();
            mv1.visitLdcInsn(Type.getType("L" + entityClass + ";"));
            mv1.visitInsn(Opcodes.ARETURN);
            mv1.visitMaxs(0, 0);
            mv1.visitEnd();

            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                    "getEntityName",
                    "()Ljava/lang/String;",
                    null,
                    null);
            mv.visitCode();
            mv.visitLdcInsn(entityName);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            super.visitEnd();
        }
    }
}
