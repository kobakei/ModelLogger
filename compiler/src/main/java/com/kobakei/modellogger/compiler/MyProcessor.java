package com.kobakei.modellogger.compiler;

import com.google.auto.service.AutoService;
import com.kobakei.modellogger.LogField;
import com.kobakei.modellogger.Loggable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.kobakei.modellogger.Loggable", "com.kobakei.modellogger.LogField"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class MyProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Class<Loggable> loggableClass = Loggable.class;
        for (Element element : roundEnv.getElementsAnnotatedWith(loggableClass)) {
            ElementKind kind = element.getKind();
            if (kind == ElementKind.CLASS) {
                try {
                    generateLogger(element);
                } catch (IOException e) {
                    this.messager.printMessage(Diagnostic.Kind.ERROR, "IO error");
                }
            } else {
                this.messager.printMessage(Diagnostic.Kind.ERROR, "Type error");
            }
        }
        return true;
    }

    private void generateLogger(Element element) throws IOException {

        String packageName = elements.getPackageOf(element).getQualifiedName().toString();
        this.messager.printMessage(Diagnostic.Kind.OTHER, packageName);
        ClassName modelClass = ClassName.get(packageName, element.getSimpleName().toString());

        // LogFieldアノテーション付きのフィールドを見つけて、ログの作成式に追加
        Class<LogField> logFieldClass = LogField.class;
        String message = null;
        for (Element el : element.getEnclosedElements()) {
            if (el.getAnnotation(logFieldClass) != null) {
                this.messager.printMessage(Diagnostic.Kind.OTHER, el.getSimpleName());
                this.messager.printMessage(Diagnostic.Kind.OTHER, el.getAnnotation(logFieldClass).toString());
                String fieldName = el.getSimpleName().toString();
                if (message == null) {
                    message = String.format("\"%s = \" + model.%s", fieldName, fieldName);
                } else {
                    message += String.format(" + \" %s = \" + model.%s ", fieldName, fieldName);
                }
            }
        }
        String tag = element.getSimpleName().toString();

        ClassName logClass = ClassName.get("android.util", "Log");

        MethodSpec logMethod = MethodSpec.methodBuilder("log")
                .addParameter(modelClass, "model")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("$T.v(\"" + tag + "\", " + message + ")", logClass) // Log.v("modelclass", "hoge=" + hoge)
                .build();

        String className = element.getSimpleName() + "Logger";
        TypeSpec logger = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(logMethod)
                .build();

        JavaFile.builder(packageName, logger)
                .build()
                .writeTo(filer);
    }
}
