package org.swdc.swt.plugin;

import org.eclipse.sisu.space.asm.*;

public class RelocatePackageVisitor extends ClassVisitor {

    private String oldPackage;

    private String newPackage;

    public RelocatePackageVisitor(int api, ClassWriter writer, String oldPackage, String newPackage) {

        super(api,writer);
        this.oldPackage = oldPackage;
        this.newPackage = newPackage;

    }

    private String relocate(String define) {

        if (define == null) {
            return null;
        }
        String declareSource = oldPackage.replace(".","/");
        String declareTarget = newPackage.replace(".", "/");

        return define.replace(oldPackage,newPackage)
                .replace(declareSource,declareTarget);

    }

    private String[] relocate(String[] defined) {
        if (defined == null) {
            return new String[0];
        }
        for (int idx = 0; idx < defined.length; idx ++) {
            defined[idx] = relocate(defined[idx]);
        }
        return defined;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(
                version,
                access,
                relocate(name),
                relocate(signature),
                relocate(superName),
                relocate(interfaces)
        );
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(
                relocate(desc),
                visible
        );
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return super.visitField(
                access,
                name,
                relocate(desc),
                relocate(signature),
                value
        );
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(
                access,
                name,
                relocate(desc),
                relocate(signature),
                relocate(exceptions)
        );
        return new RelocateMethodVisitor(api,methodVisitor,oldPackage,newPackage);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return super.visitTypeAnnotation(typeRef, typePath, relocate(descriptor), visible);
    }

}
