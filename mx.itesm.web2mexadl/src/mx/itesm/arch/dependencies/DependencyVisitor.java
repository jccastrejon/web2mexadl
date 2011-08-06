package mx.itesm.arch.dependencies;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * A visitor to obtain the class' dependencies while visiting it's components.
 * 
 * @author jccastrejon
 * 
 */
public class DependencyVisitor extends EmptyVisitor {

    /**
     * Class dependencies.
     */
    private Set<String> dependencies;

    /**
     * Default constructor.
     */
    public DependencyVisitor() {
        this.dependencies = new HashSet<String>();
    }

    /**
     * Add a full class name to the dependencies Set.
     * 
     * @param dependency
     *            Class name.
     */
    private void addDependencyFromName(final String dependency) {
        // Don't add null or array dependencies
        if ((dependency != null) && (!dependency.startsWith("["))) {
            // Convert to standard package name
            dependencies.add(dependency.replace('/', '.'));
        }
    }

    /**
     * Add several classes names to the dependencies Set.
     * 
     * @param dependencies
     *            Classes names.
     */
    private void addDependenciesFromNames(final String[] dependencies) {
        // Don't add null or array dependencies
        if (dependencies != null) {
            for (int i = 0; i < dependencies.length; i++) {
                if (!dependencies[i].startsWith("[")) {
                    this.addDependencyFromName(dependencies[i]);
                }
            }
        }
    }

    /**
     * Add a class name contained within an element description.
     * 
     * @param description
     *            Element description.
     */
    private void addDependencyFromDescription(final String description) {
        String dependencyName;
        int semicolonIndex;
        int lowerthanIndex;

        if (description != null) {
            // Look for 'L' descriptor
            for (int i = 0; i < description.length(); i++) {
                if (description.charAt(i) == 'L') {
                    // Dependency name
                    semicolonIndex = description.indexOf(';', i);
                    if (((i + 1) < description.length()) && (semicolonIndex > 0)) {
                        dependencyName = description.substring(i + 1, semicolonIndex);
                        lowerthanIndex = dependencyName.indexOf('<');
                        // More than one class name
                        if (lowerthanIndex > 0) {
                            this.addDependencyFromName(dependencyName.substring(0, lowerthanIndex));
                            this.addDependencyFromDescription(dependencyName.substring(lowerthanIndex + 1,
                                    dependencyName.length()) + ";");
                        } else {
                            this.addDependencyFromName(dependencyName);
                        }

                        i = semicolonIndex;
                    }
                }
            }
        }
    }

    /**
     * @return the dependencies
     */
    public Set<String> getDependencies() {
        return dependencies;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.addDependencyFromName(name);
        this.addDependencyFromDescription(signature);
        this.addDependencyFromName(superName);
        this.addDependenciesFromNames(interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        this.addDependencyFromDescription(desc);
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        this.addDependencyFromDescription(desc);
        return super.visitAnnotation(name, desc);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        this.addDependencyFromDescription(desc);
        this.addDependencyFromDescription(signature);
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        this.addDependencyFromDescription(desc);
        this.addDependencyFromDescription(signature);
        this.addDependenciesFromNames(exceptions);
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        this.addDependencyFromName(desc);
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitTypeInsn(int opcode, String desc) {
        this.addDependencyFromName(desc);
        super.visitTypeInsn(opcode, desc);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        this.addDependencyFromName(owner);
        this.addDependencyFromDescription(desc);
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        this.addDependencyFromName(owner);
        this.addDependencyFromDescription(desc);
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof Type) {
            this.addDependencyFromDescription(((Type) cst).getDescriptor());
        }

        super.visitLdcInsn(cst);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        this.addDependencyFromDescription(desc);
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        this.addDependencyFromDescription(desc);
        this.addDependencyFromDescription(signature);
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.addDependencyFromName(type);
        super.visitTryCatchBlock(start, end, handler, type);
    }
}
