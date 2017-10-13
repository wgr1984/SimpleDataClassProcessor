package de.wr.annotationprocessor.processor

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.google.auto.value.AutoValue

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
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

import de.wr.libsimpledataclasses.DataClassFactory
import de.wr.libsimpledataclasses.DefaultInt
import de.wr.libsimpledataclasses.DefaultString
import io.reactivex.annotations.Nullable
import io.reactivex.rxkotlin.toObservable
import java.io.*
import java.util.*
import java.util.stream.Collectors

import javax.lang.model.SourceVersion.latestSupported
import javax.lang.model.type.TypeKind
import javax.xml.crypto.Data

import com.github.javaparser.ast.Modifier as AstModifier

class SimpleDataClassInterfaceProcessor : AbstractProcessor() {

    private val methodsForClass = Hashtable<TypeElement, List<ExecutableElement>>()

    private lateinit var objectType: String
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
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

            typeElement.enclosedElements
                    .toObservable().filter {
                        it -> it is ExecutableElement && it.simpleName.startsWith("create")
                    }
                    .cast(ExecutableElement::class.java)
                    .blockingForEach {
                        info(element, "Data class def found: %s", it.simpleName)

                        try {
                            val fileName = it.simpleName.substring("create".length)
                            val source = processingEnv.filer.createSourceFile(fileName)

                            val writer = BufferedWriter(source.openWriter())

//                javaFile.writeTo(writer)

                            generateDataClass(
                                    element,
                                    writer,
                                    fileName,
                                    typeElement.qualifiedName.substring(0, typeElement.qualifiedName.length - typeElement.simpleName.length -1 ),
                                    it)

                            writer.flush()
                            writer.close()

                            info(element,"Data class generated: %s %n", fileName)
                        } catch (e: IOException) {
                            System.err.println(objectType + " :" + e + e.message)
                        }
                    }
        }

        return true
    }

    private fun generateDataClass(factoryElement: TypeElement, writer: BufferedWriter?, className: String, packageName: String, creationMethod: ExecutableElement) {
        val cu = CompilationUnit();
        val cuBuilder = CompilationUnit();
        // set the package
        cu.setPackageDeclaration(packageName);

        // create the type declaration
        val type = cu.addClass(className, AstModifier.ABSTRACT);
        type.addAnnotation(AutoValue::class.java)
        type.tryAddImportToParentCompilationUnit(AutoValue.Builder::class.java)

        val builderType = cuBuilder.addClass("Builder", AstModifier.ABSTRACT, AstModifier.STATIC)
        builderType.addAnnotation(AutoValue.Builder::class.java.canonicalName)
        type.addMember(builderType)

        val builderMethod = type.addMethod("builder", AstModifier.STATIC)
        builderMethod.setType("Builder")

        val builderBody = BlockStmt()
        val newOperation = ObjectCreationExpr()
        newOperation.setType("AutoValue_$className.Builder")

        var builderCall:Expression = newOperation

        creationMethod.parameters.forEach {
            val propertyName = it.simpleName.toString()
            // create a method

            // Create Getter
            val propertyGetter = type.addMethod(propertyName, AstModifier.PUBLIC, AstModifier.ABSTRACT) // add parameters to method
            propertyGetter.setType(it.asType().toString())
            propertyGetter.removeBody()

            // Create Setter
            val propertySetter = builderType.addMethod(propertyName, AstModifier.PUBLIC, AstModifier.ABSTRACT) // add parameters to method
            propertySetter.addParameter(it.asType().toString(), propertyName)
            propertySetter.setType("Builder")
            propertySetter.removeBody()

            //set defaults
            // ints
            it.getAnnotation(DefaultInt::class.java)?.let {
                val defaultMethod = MethodCallExpr(builderCall, propertyName)
                defaultMethod.addArgument(IntegerLiteralExpr(it.value))
                builderCall = defaultMethod
            }
            //Strings
            it.getAnnotation(DefaultString::class.java)?.let {
                val defaultMethod = MethodCallExpr(builderCall, propertyName)
                defaultMethod.addArgument(StringLiteralExpr(it.value))
                builderCall = defaultMethod
            }

            //def values for primary types
            it.asType().kind.let { kind ->
                if (kind.isPrimitive && it.annotationMirrors.isEmpty()) {
                    val defaultMethod = MethodCallExpr(builderCall, propertyName)
                    when(kind) {
                        TypeKind.INT -> "0"
                        TypeKind.BOOLEAN -> "false"
                        TypeKind.BYTE -> "(byte)0"
                        TypeKind.SHORT -> "(short)0"
                        TypeKind.LONG -> "0l"
                        TypeKind.CHAR -> "0"
                        TypeKind.FLOAT -> "0.0f"
                        TypeKind.DOUBLE -> "0.0d"
                        else -> ""
                    }.takeIf { !it.isEmpty() }?.let (defaultMethod::addArgument)
                    builderCall = defaultMethod
                }
            }

            //Nullables
            if (!it.asType().kind.isPrimitive) {
                val dataClassFactoryAnnotion = factoryElement.getAnnotation(DataClassFactory::class.java)
                if (dataClassFactoryAnnotion.nullableAsDefault) {
                    factoryElement.annotationMirrors.find { it.toString().contains("DataClassFactory") }?.let {
                        val nullableClass: ExecutableElement? = it.elementValues.keys.find { key -> key.toString().contains("value") }
                        val classValue = it.elementValues[nullableClass].toString()
                        propertyGetter.addAnnotation(classValue.substring(0, classValue.lastIndexOf(".")));
                    }
                } else {
                    creationMethod.annotationMirrors
                            .union(it.annotationMirrors)
                            .find { it.toString().contains("Nullable") }?.let {
                        propertyGetter.addAnnotation(it.annotationType.toString())
                    }
                }
            }
        }

        val returnStm = ReturnStmt(builderCall)
        builderBody.addStatement(returnStm)
        builderMethod.setBody(builderBody)

        // Add build method
        val propertySetter = builderType.addMethod("build", AstModifier.PUBLIC, AstModifier.ABSTRACT) // add parameters to method
        propertySetter.setType(className)
        propertySetter.removeBody()

        // toBuilder
        val toBuilder = type.addMethod("toBuilder", AstModifier.PUBLIC, AstModifier.ABSTRACT) // add parameters to method
        toBuilder.setType("Builder")
        toBuilder.removeBody()

        writer?.write(cu.toString())
        writer?.flush()
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, *args),
                e)
    }

    private fun info(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
                Diagnostic.Kind.NOTE,
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