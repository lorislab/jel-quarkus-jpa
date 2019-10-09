package org.lorislab.quarkus.jel.jpa.deployment;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.BiFunction;

public class AbstractEntityBuilderEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private String entityClass;

    private String entityName;

    public AbstractEntityBuilderEnhancer(String entityName, String entityClass) {
        this.entityClass = entityClass;
        this.entityName = entityName;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new AbstractEntityBuilderEnhancerClassVisitor(className, outputClassVisitor, entityName, entityClass);
    }

    static class AbstractEntityBuilderEnhancerClassVisitor extends ClassVisitor {

        private String entityClass;

        private String entityName;

        public AbstractEntityBuilderEnhancerClassVisitor(String className, ClassVisitor outputClassVisitor, String entityName, String entityClass) {
            super(Opcodes.ASM7, outputClassVisitor);
            this.entityClass = entityClass.replace('.', '/');
            this.entityName = entityName;
        }

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
