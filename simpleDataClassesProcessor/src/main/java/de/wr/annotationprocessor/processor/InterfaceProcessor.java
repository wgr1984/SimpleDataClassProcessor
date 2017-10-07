package de.wr.annotationprocessor.processor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import de.wr.libsimpledataclasses.DataClassFactory;

import static javax.lang.model.SourceVersion.latestSupported;

@SuppressWarnings("unused")
public class InterfaceProcessor extends AbstractProcessor {

    private static Set<String> supportedAnnotations;

    private Hashtable<TypeElement, List<ExecutableElement>> methodsForClass = new Hashtable<>();

    static {
        supportedAnnotations = new HashSet<>();
        supportedAnnotations.add(DataClassFactory.class.getCanonicalName());
    }

    private String objectType;
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private String sourcePath;
    private String qualifiedName;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return supportedAnnotations;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        sourcePath = fetchSourcePath();
    }

    private void addMethodToClass(TypeElement clazz, ExecutableElement method) {
        List<ExecutableElement> executableElements = methodsForClass.get(clazz);
        if (executableElements != null) {
            for (ExecutableElement executableElement : executableElements) {
                System.err.println(executableElement);
            }
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(DataClassFactory.class);

        for (Element element : elementsAnnotatedWith) {

            if (element.getKind() != ElementKind.CLASS && element.getModifiers().contains(Modifier.ABSTRACT)) {
                error(element, "The annotated element %s is not an abstract class",
                        element);
                return false;
            }

            // We can cast it, because we know that it of ElementKind.CLASS
            TypeElement typeElement = (TypeElement) element;

            objectType = typeElement.getSimpleName().toString();

            qualifiedName = typeElement.getQualifiedName().toString();

            if (objectType != null) {
                String interfaceName = objectType.substring(0, objectType.lastIndexOf("Impl"));

                TypeSpec.Builder newInterfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                        .addModifiers(Modifier.PUBLIC);

                applyMethodsFromSource(methodDeclaration -> {
//                System.err.printf("Method visited: %s %n", methodDeclaration);
                    for (Element methodElement : new ArrayList<>(elementsAnnotatedWith)) {
                        ExecutableElement method = (ExecutableElement) methodElement;
                        String methodSimpleName = method.getSimpleName().toString();
                        System.err.printf("Check Method: %s %s %n", methodSimpleName,  methodDeclaration.getName().asString());
                        if (methodSimpleName.equals(methodDeclaration.getName().asString())) {
                            if (method.getParameters().equals(((ExecutableElement) methodElement).getParameters())) {
                                System.err.printf("Method found: %s %n", method);
                                elementsAnnotatedWith.remove(methodElement);
                                newInterfaceBuilder.addMethod(createInterfaceMethod(method, methodDeclaration));
                            }
                        }
                    }
                    String nameAsString = methodDeclaration.getNameAsString();
                });

                TypeSpec newInterface = newInterfaceBuilder.build();

                String currentPackage = getClass().getPackage().getName();

                String packageName = currentPackage.substring(0, currentPackage.lastIndexOf(".")) + ".generated";

                JavaFile javaFile = JavaFile.builder(packageName, newInterface)
                        .build();

                try {
                    String fileName = javaFile.packageName + "." + newInterface.name;
                    JavaFileObject source = processingEnv.getFiler().createSourceFile(fileName);

                    Writer writer = source.openWriter();

                    javaFile.writeTo(writer);

                    writer.flush();
                    writer.close();


                    System.err.printf("Class generated: %s %n", fileName);
                } catch (IOException e) {
                    System.err.println(objectType + " :" + e + e.getMessage());
                }

                objectType = null;
            }
        }

        return true;
    }

    private MethodSpec createInterfaceMethod(ExecutableElement methodDeclaration, MethodDeclaration declaration) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodDeclaration.getSimpleName().toString());

        List<? extends VariableElement> parameters = methodDeclaration.getParameters();
        System.err.println("Parameters: " + parameters);
        for (VariableElement parameter : parameters) {
            try {
                String parameterType = parameter.asType().toString();
                System.err.println("Parameter Type: " + parameterType);
                if (parameterType.equals("int")) {
                    builder.addParameter(int.class, parameter.getSimpleName().toString());
                } else {
                    builder.addParameter(Class.forName(parameterType), parameter.getSimpleName().toString());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            builder.returns(Class.forName(methodDeclaration.getReturnType().toString()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

        declaration.getJavadocComment().map(doc -> builder.addJavadoc(doc.getContent().replaceAll("\\*\\s+", "")));

        System.err.println("JavaDoc: " + declaration.getJavadocComment());

//                .addParameter(String[].class, "args")
//                .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet 2!");

        return builder.build();
    }

    private void applyMethodsFromSource(Consumer<MethodDeclaration> methodDeclarationConsumer) {
        try {
            String filePath = sourcePath + "/src/main/java/" + qualifiedName.replaceAll("\\.", "/") + ".java" ;

            File file = new File(filePath);

            if (file.exists()) {
                // creates an input stream for the file to be parsed
                FileInputStream in = new FileInputStream(filePath);

                // parse the file
                CompilationUnit cu = JavaParser.parse(in);

                // visit and print the methods names
                new MethodVisitor(methodDeclarationConsumer).visit(cu, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodVisitor extends VoidVisitorAdapter<Void> {

        private final Consumer<MethodDeclaration> consumer;

        MethodVisitor(Consumer<MethodDeclaration> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            /* here you can access the attributes of the method.
             this method will be called for all methods in this
             CompilationUnit, including inner class methods */
//            System.out.println(n.getJavadocComment());
//            methodJavaDoc.put(n);

            consumer.accept(n);
            super.visit(n, arg);
        }
    }

    private String fetchSourcePath() {
        try {
            JavaFileObject generationForPath = processingEnv.getFiler().createSourceFile("PathFor" + getClass().getSimpleName());
            Writer writer = generationForPath.openWriter();
            String sourcePath = generationForPath.toUri().getPath();
            writer.close();
            generationForPath.delete();

            return sourcePath.substring(0, sourcePath.lastIndexOf("/build"));
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unable to determine source file path!");
        }

        return "";
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }
}