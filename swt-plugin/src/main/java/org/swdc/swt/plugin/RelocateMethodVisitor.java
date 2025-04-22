package org.swdc.swt.plugin;

import org.eclipse.sisu.space.asm.Label;
import org.eclipse.sisu.space.asm.MethodVisitor;

public class RelocateMethodVisitor extends MethodVisitor {

    private String oldPackage;

    private String newPackage;

    public RelocateMethodVisitor(int api,MethodVisitor visitor, String oldPackage, String newPackage) {
        super(api,visitor);
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

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, relocate(type));
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof String) {
            String str = (String) cst;
            super.visitLdcInsn(relocate(str));
        } else {
            super.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(
                opcode,
                relocate(owner),
                name,
                relocate(desc),
                itf
        );
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(
                opcode,
                relocate(owner),
                name,
                relocate(desc)
        );
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, relocate(descriptor), relocate(signature), start, end, index);
    }
}
