package de.wr.annotationprocessor.processor

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.google.auto.value.AutoValue

import java.util.HashSet
import java.util.Hashtable

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
import io.reactivex.rxkotlin.toObservable
import java.io.*

import javax.lang.model.SourceVersion.latestSupported

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

    private fun generateDataClass(writer: BufferedWriter?, className: String, packageName: String, creationMethod: ExecutableElement) {
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
        }

        // Add build method
        val propertySetter = builderType.addMethod("build", AstModifier.PUBLIC, AstModifier.ABSTRACT) // add parameters to method
        propertySetter.setType(className)
        propertySetter.removeBody()

        val builderMethod = type.addMethod("builder", AstModifier.STATIC)
        builderMethod.setType("Builder")

        val builderBody = BlockStmt()
        val newOperation = ObjectCreationExpr()
        newOperation.setType("AutoValue_$className.Builder")
        val returnStm = ReturnStmt(newOperation)
        builderBody.addStatement(returnStm)

        builderMethod.setBody(builderBody)

        val rawMainClass = cu.toString()
        val mainClass = rawMainClass.substring(0, rawMainClass.length - 2) +
                "\n" +
                cuBuilder.toString().replace("import "+AutoValue.Builder::class.java.canonicalName, "" ) +
                "\n}"

        writer?.write(mainClass)
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