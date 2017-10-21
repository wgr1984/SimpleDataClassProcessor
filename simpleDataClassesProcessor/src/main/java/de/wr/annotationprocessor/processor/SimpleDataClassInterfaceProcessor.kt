package de.wr.annotationprocessor.processor

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.google.auto.value.AutoValue
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory
import de.wr.libsimpledataclasses.*

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

            val qualifiedName = typeElement.qualifiedName.toString()

            info(element, "The annotated element %s found", element)

            val classesList = typeElement.enclosedElements
                    .filter { it -> it is ExecutableElement && it.returnType.toString().contains("Void") }
                    .map { it -> it as ExecutableElement }

            if (classesList.isNotEmpty()) {
                classesList.forEach { it -> generateAutoValueClasses(element, it, typeElement) }
                if (element.annotationMirrors
                        .union(classesList.flatMap { it -> it.annotationMirrors })
                        .any { it -> it.toString().contains("Gson") }) {
                    generateAutoValueGsonFactory(qualifiedName, typeElement)
                }
            }
        }

        return true
    }

    private fun generateAutoValueGsonFactory(qualifiedName: String, typeElement: TypeElement) {
        try {
            val fileName = typeElement.simpleName.substring(0, 1).toUpperCase() + typeElement.simpleName.substring(1) + "TypeAdapterFactory"
            val source = processingEnv.filer.createSourceFile(fileName)

            val writer = BufferedWriter(source.openWriter())

            val cu = CompilationUnit();
            // set the package
            cu.setPackageDeclaration(getPackageName(typeElement));

            cu.addClass(fileName, AstModifier.PUBLIC, AstModifier.ABSTRACT)
                .addAnnotation(GsonTypeAdapterFactory::class.java)
                .addImplementedType(TypeAdapterFactory::class.java)
                .addMethod("create", AstModifier.PUBLIC, AstModifier.STATIC).setBody(
                    BlockStmt().addStatement(ReturnStmt()
                            .setExpression(ObjectCreationExpr().setType("AutoValueGson_$fileName")))
                ).setType(TypeAdapterFactory::class.java)

            writer.run {
                write(cu.toString())
                flush()
                close()
            }

            info(typeElement, "Gson Data class generated: %s %n", fileName)
        } catch (e: IOException) {
            System.err.println(objectType + " :" + e + e.message)
        }
    }

    private fun generateAutoValueClasses(element: TypeElement, it: ExecutableElement, typeElement: TypeElement) {
        info(element, "Data class def found: %s", it.simpleName)

        try {
            val fileName = it.simpleName.substring(0, 1).toUpperCase() + it.simpleName.substring(1)
            val source = processingEnv.filer.createSourceFile(fileName)

            val writer = BufferedWriter(source.openWriter())

            generateDataClass(
                    element,
                    writer,
                    fileName,
                    getPackageName(typeElement),
                    it)

            writer.run {
                flush()
                close()
            }

            info(element, "Data class generated: %s %n", fileName)
        } catch (e: IOException) {
            System.err.println(objectType + " :" + e + e.message)
        }
    }

    private fun getPackageName(typeElement: TypeElement) =
            typeElement.qualifiedName.substring(0, typeElement.qualifiedName.length - typeElement.simpleName.length - 1)

    private fun generateDataClass(factoryElement: TypeElement, writer: BufferedWriter?, className: String, packageName: String, creationMethod: ExecutableElement) {
        val cu = CompilationUnit();
        val cuBuilder = CompilationUnit();
        // set the package
        cu.setPackageDeclaration(packageName);

        // create the type declaration
        val type = cu.addClass(className, AstModifier.ABSTRACT)
                    .addAnnotation(AutoValue::class.java)
        factoryElement.annotationMirrors
                .union(creationMethod.annotationMirrors)
                .find { it.toString().contains("Parcelable") }?.let {
            type.addImplementedType("android.os.Parcelable")
        }
        type.tryAddImportToParentCompilationUnit(AutoValue.Builder::class.java)

        val builderType = cuBuilder.addClass("Builder", AstModifier.ABSTRACT, AstModifier.STATIC)
                            .addAnnotation(AutoValue.Builder::class.java.canonicalName)
        type.addMember(builderType)

        val builderMethod = type.addMethod("builder", AstModifier.STATIC)
                                .setType("Builder")

        val builderBody = BlockStmt()
        val newOperation = ObjectCreationExpr().setType("AutoValue_$className.Builder")

        var builderCall:Expression = newOperation

        creationMethod.parameters.forEach {
            val propertyName = it.simpleName.toString()
            // create a method

            // Create Getter
            val propertyGetter = type.addMethod(propertyName, AstModifier.PUBLIC, AstModifier.ABSTRACT)
                    .setType(it.asType().toString())
                    .removeBody()

            // Create Setter
            builderType.addMethod(propertyName, AstModifier.PUBLIC, AstModifier.ABSTRACT)
                    .addParameter(it.asType().toString(), propertyName)
                    .setType("Builder")
                    .removeBody()

            //set defaults
            // ints
            it.getAnnotation(DefaultInt::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument(IntegerLiteralExpr(it.value))
            }

            // longs
            it.getAnnotation(DefaultLong::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument(LongLiteralExpr(it.value))
            }

            // short
            it.getAnnotation(DefaultShort::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument("(short)"+it.value.toString())
            }

            // Byte
            it.getAnnotation(DefaultByte::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument("(byte)"+it.value.toString())
            }

            // Boolean
            it.getAnnotation(DefaultBool::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument(BooleanLiteralExpr(it.value))
            }

            // Double
            it.getAnnotation(DefaultDouble::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument(DoubleLiteralExpr(it.value))
            }

            // Float
            it.getAnnotation(DefaultFloat::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument(it.value.toString()+"f")
            }

            //Strings
            it.getAnnotation(DefaultString::class.java)?.let {
                builderCall = MethodCallExpr(builderCall, propertyName)
                        .addArgument(StringLiteralExpr(it.value))
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
                    propertyGetter.addAnnotation(javax.annotation.Nullable::class.java)
                            .parameters.forEach { it.addAnnotation(javax.annotation.Nullable::class.java) }
                } else {
                    val nullableAnnotated =
                            (factoryElement.annotationMirrors
                                .union(creationMethod.annotationMirrors)
                                .any { it.toString().contains("Nullable") }
                                && !it.annotationMirrors
                                    .any { it.toString().toLowerCase().contains("nonnull") })
                            || it.annotationMirrors
                                .any { it.toString().contains("Nullable") }

                    if (nullableAnnotated) {
                        propertyGetter.addAnnotation(javax.annotation.Nullable::class.java)
                                .parameters.forEach { it.addAnnotation(javax.annotation.Nullable::class.java) }
                    } else {
                        propertyGetter.addAnnotation(javax.annotation.Nonnull::class.java)
                                .parameters.forEach { it.addAnnotation(javax.annotation.Nonnull::class.java) }
                    }
                }
            }
        }

        val returnStm = ReturnStmt(builderCall)
        builderBody.addStatement(returnStm)
        builderMethod.setBody(builderBody)

        // Add build method
        val propertySetter = builderType.addMethod("build", AstModifier.PUBLIC, AstModifier.ABSTRACT)
                .setType(className)
                .removeBody()

        // toBuilder
        val toBuilder = type.addMethod("toBuilder", AstModifier.PUBLIC, AstModifier.ABSTRACT)
                .setType("Builder")
                .removeBody()

        // add gson adapter
        factoryElement.annotationMirrors
                .union(creationMethod.annotationMirrors)
                .find { it.toString().contains("Gson") }?.let {
            val gsonTypeAdapter = type.addMethod("typeAdapter", AstModifier.PUBLIC, AstModifier.STATIC)
                    .setType(TypeAdapter::class.java.canonicalName+"<"+className+">")
                    .addParameter(com.google.gson.Gson::class.java, "gson")

            val typeAdapterBody = BlockStmt()
            val newTypeAdapterStmt = ObjectCreationExpr()
                    .setType("AutoValue_$className.GsonTypeAdapter")
                    .addArgument("gson")

            val typeAdapterReturn = ReturnStmt(newTypeAdapterStmt)
            typeAdapterBody.addStatement(typeAdapterReturn)
            gsonTypeAdapter.setBody(typeAdapterBody)
        }

        writer?.run {
            write(cu.toString())
            flush()
        }
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