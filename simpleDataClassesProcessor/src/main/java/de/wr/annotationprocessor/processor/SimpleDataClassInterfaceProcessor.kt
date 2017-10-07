package de.wr.annotationprocessor.processor

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.Writer
import java.util.ArrayList
import java.util.HashSet
import java.util.Hashtable
import java.util.function.Consumer

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

import de.wr.libsimpledataclasses.DataClassFactory

import javax.lang.model.SourceVersion.latestSupported

class SimpleDataClassInterfaceProcessor : AbstractProcessor() {

    private val methodsForClass = Hashtable<TypeElement, List<ExecutableElement>>()

    private lateinit var objectType: String
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private lateinit var sourcePath: String
    private var qualifiedName: String? = null

    override fun getSupportedSourceVersion(): SourceVersion {
        return latestSupported()
    }

    override fun getSupportedAnnotationTypes() = supportedAnnotations

    @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
        messager = processingEnv.messager
        sourcePath = fetchSourcePath()
    }

//    private fun addMethodToClass(clazz: TypeElement, method: ExecutableElement) {
//        val executableElements = methodsForClass[clazz]
//        if (executableElements != null) {
//            for (executableElement in executableElements) {
//                System.err.println(executableElement)
//            }
//        }
//    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(DataClassFactory::class.java)

        elementsAnnotatedWith.forEach { element ->
            if (element.kind != ElementKind.CLASS || !element.modifiers.contains(Modifier.ABSTRACT)) {
                error(element, "The annotated element %s is not an abstract class",
                        element)
                return false
            }

            // We can cast it, because we know that it of ElementKind.CLASS
            val typeElement = element as TypeElement

            objectType = typeElement.simpleName.toString()

            qualifiedName = typeElement.qualifiedName.toString()

            info(element, "The annotated element %s found", element)

//            val interfaceName = objectType.substring(0, objectType.lastIndexOf("Impl"))
//
//            val newInterfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
//                    .addModifiers(Modifier.PUBLIC)
//
//            applyMethodsFromSource(Consumer{ methodDeclaration ->
//                //                System.err.printf("Method visited: %s %n", methodDeclaration);
//                for (methodElement in ArrayList(elementsAnnotatedWith)) {
//                    val method = methodElement as ExecutableElement
//                    val methodSimpleName = method.simpleName.toString()
//                    System.err.printf("Check Method: %s %s %n", methodSimpleName, methodDeclaration.getName().asString())
//                    if (methodSimpleName == methodDeclaration.getName().asString()) {
//                        if (method.parameters == methodElement.parameters) {
//                            System.err.printf("Method found: %s %n", method)
//                            elementsAnnotatedWith.remove(methodElement)
//                            newInterfaceBuilder.addMethod(createInterfaceMethod(method, methodDeclaration))
//                        }
//                    }
//                }
//                val nameAsString = methodDeclaration.getNameAsString()
//            })
//
//            val newInterface = newInterfaceBuilder.build()
//
//            val currentPackage = javaClass.`package`.name
//
//            val packageName = currentPackage.substring(0, currentPackage.lastIndexOf(".")) + ".generated"
//
//            val javaFile = JavaFile.builder(packageName, newInterface)
//                    .build()
//
//            try {
//                val fileName = javaFile.packageName + "." + newInterface.name
//                val source = processingEnv.filer.createSourceFile(fileName)
//
//                val writer = source.openWriter()
//
//                javaFile.writeTo(writer)
//
//                writer.flush()
//                writer.close()
//
//
//                System.err.printf("Class generated: %s %n", fileName)
//            } catch (e: IOException) {
//                System.err.println(objectType + " :" + e + e.message)
//            }
        }

        return true
    }

    private fun createInterfaceMethod(methodDeclaration: ExecutableElement, declaration: MethodDeclaration): MethodSpec {
        val builder = MethodSpec.methodBuilder(methodDeclaration.simpleName.toString())

        val parameters = methodDeclaration.parameters
        System.err.println("Parameters: " + parameters)
        for (parameter in parameters) {
            try {
                val parameterType = parameter.asType().toString()
                System.err.println("Parameter Type: " + parameterType)
                if (parameterType == "int") {
                    builder.addParameter(Int::class.javaPrimitiveType, parameter.simpleName.toString())
                } else {
                    builder.addParameter(Class.forName(parameterType), parameter.simpleName.toString())
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }

        }
        try {
            builder.returns(Class.forName(methodDeclaration.returnType.toString()))
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)

//        declaration.javadocComment.map<Builder> { doc -> builder.addJavadoc(doc.content.replace("\\*\\s+".toRegex(), "")) }

        System.err.println("JavaDoc: " + declaration.javadocComment)

        //                .addParameter(String[].class, "args")
        //                .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet 2!");

        return builder.build()
    }

    private fun applyMethodsFromSource(methodDeclarationConsumer: Consumer<MethodDeclaration>) {
        try {
            val filePath = sourcePath + "/src/main/java/" + qualifiedName!!.replace("\\.".toRegex(), "/") + ".java"

            val file = File(filePath)

            if (file.exists()) {
                // creates an input stream for the file to be parsed
                val `in` = FileInputStream(filePath)

                // parse the file
                val cu = JavaParser.parse(`in`)

                // visit and print the methods names
                MethodVisitor(methodDeclarationConsumer).visit(cu, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private class MethodVisitor internal constructor(private val consumer: Consumer<MethodDeclaration>) : VoidVisitorAdapter<Void>() {

        override fun visit(n: MethodDeclaration, arg: Void) {
            /* here you can access the attributes of the method.
             this method will be called for all methods in this
             CompilationUnit, including inner class methods */
            //            System.out.println(n.getJavadocComment());
            //            methodJavaDoc.put(n);

            consumer.accept(n)
            super.visit(n, arg)
        }
    }

    private fun fetchSourcePath(): String {
        try {
            val generationForPath = processingEnv.filer.createSourceFile("PathFor" + javaClass.simpleName)
            val writer = generationForPath.openWriter()
            val sourcePath = generationForPath.toUri().path
            writer.close()
            generationForPath.delete()

            return sourcePath.substring(0, sourcePath.lastIndexOf("/build"))
        } catch (e: IOException) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Unable to determine source file path!")
        }

        return ""
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, *args),
                e)
    }

    private fun info(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
                Diagnostic.Kind.WARNING,
                String.format(msg, *args),
                e)
    }


    companion object {
        private var supportedAnnotations = HashSet<String>()

        init {
            supportedAnnotations.add(DataClassFactory::class.java.canonicalName)
        }
    }
}