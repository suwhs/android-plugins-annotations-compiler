package su.whs.plugins.core;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import su.whs.plugins.annotations.ImportPluginInterfaces;
import su.whs.plugins.annotations.Plugin;
import su.whs.plugins.annotations.PluginInterface;

@AutoService(PluginsAnnotationProcessor.class)
@SupportedAnnotationTypes(
        {
                "su.whs.plugins.annotations.Plugin",
                "su.whs.plugins.annotations.PluginInterface",
                "su.whs.plugins.annotations.ImportPluginInterfaces"
        })
@SupportedOptions({
        "androidManifestFile",
        "sdk.path",
        "projectRoot",
        "resourcePackageName"
})
public class PluginsAnnotationProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private PluginInterfacesGroup interfaces;
    private PluginAnnotatedGroup plugins;
    private ProcessingEnvironment processingEnvironment;
    private String packageName = null;
    // private static AndroidBuildEnvironment manifestWriter;
    private GenerationEnvironment mEnvironment = null;
    private boolean mFirstRound = true;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        System.err.println("<init> annotation processor");
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        processingEnvironment = processingEnv;
        interfaces = new PluginInterfacesGroup(typeUtils);
        plugins  = new PluginAnnotatedGroup(typeUtils,interfaces);
        Map<String,String> options = processingEnv.getOptions();
        for (String k:options.keySet()) {
            System.out.println("OPTION '"+k+"'='"+options.get(k)+"'");
        }
    }

    private static int round = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        mEnvironment = new GenerationEnvironment(processingEnvironment,roundEnv);
        System.out.println("round "+round);
        round++;
        // error(null,"processing started");
        for (Iterator iterator = annotations.iterator(); iterator.hasNext();) {
            TypeElement typeElement = (TypeElement) iterator.next();
            System.out.println(typeElement.getQualifiedName());
        }

        // System.out.println(roundEnv.getRootElements().toString());

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ImportPluginInterfaces.class)) {
            processImportPluginInterfaces(annotatedElement);
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(PluginInterface.class)) {
            if (!processPluginInterface((TypeElement)annotatedElement)) return false;
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Plugin.class)) {
            TypeElement pluginInterface = processPlugin((TypeElement)annotatedElement);
            if (!interfaces.contains(pluginInterface))
                interfaces.add(pluginInterface);
        }

        boolean serviceRequired = false;

        if (plugins.checkHostPackage(mEnvironment.getAndroidPackageName())) {
            serviceRequired = true;
            System.out.println("service required for package: '"+mEnvironment.getAndroidPackageName());
        } else {
            System.out.println("receiver for external plugin app required, service not required");
        }
        // build required wrappers
        // interfaces.buildSourceCode(mEnvironment);
        // plugins.buildSourceCode(mEnvironment);

        if (serviceRequired && round==1) {
            HostServiceCodeGenerator generator = new HostServiceCodeGenerator(mEnvironment,interfaces,plugins);
            try {
                System.err.println("run generator!");
                generator.generate();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {

                e.printStackTrace();
            }
            /*
            buildServiceSourceCode();
            buildHostReceiverCode(); */
            if (!mEnvironment.isServiceDefined()) {
                System.out.println("service not defined!");
                writeServiceDeclaration();
            }
        } else if (round==1) {
           /*  buildExternalPluginAppReceiverCode(); */
            PluginReceiverCodeGenerator generator = new PluginReceiverCodeGenerator(mEnvironment,interfaces,plugins);
            if (!mEnvironment.isReceiverDefined()) {
                writeReceiverForExternalPluginApp();
            }
        } else {
            System.err.println("conditions does not met");
        }

        return true;
    }

    private boolean processPluginInterface(TypeElement pluginInterface) {
        if (pluginInterface.getKind() != ElementKind.CLASS) {
            error(pluginInterface, "Only abstract classes can be annotated with @%s", PluginInterface.class.getSimpleName());
            return false;
        }
        // TypeElement typeElement = (TypeElement)pluginInterface;
        if (!pluginInterface.getModifiers().contains(Modifier.ABSTRACT)) {
            error(pluginInterface, "Only abstract classes can be annotated with @%s", PluginInterface.class.getSimpleName());
            return false;
        }
        System.out.println("add interface "+((TypeElement) pluginInterface).getQualifiedName());
        interfaces.add(pluginInterface);
        return true;
    }

    private TypeElement processPlugin(TypeElement plugin) {
        if (plugin.getKind() != ElementKind.CLASS) {
            error(plugin, "Only classes can be annotated with @%s",
                    Plugin.class.getSimpleName());
            return null;
        }
        // TypeElement typeElement = (TypeElement)plugin;
        if (plugin.getModifiers().contains(Modifier.ABSTRACT)) {
            error(plugin, "Plugin could not be abstract class");
            return null;
        }
        return plugins.addAndReturnInterface(plugin);
    }

    private void processImportPluginInterfaces(Element importer) {
        System.out.println("import plugin interfaces for "+importer.asType());
        ImportPluginInterfaces importPluginInterfaces = ((TypeElement)importer).getAnnotation(ImportPluginInterfaces.class);
        System.out.println("annotation extracted!");
        String[] imports = importPluginInterfaces.list();
        for (String classFQDN : imports) {
            System.out.println("try to get type element '"+classFQDN+"'");
            TypeElement typeElement = elementUtils.getTypeElement(classFQDN);
            PluginInterface pluginInterface = typeElement.getAnnotation(PluginInterface.class);
            if (pluginInterface!=null) {
                processPluginInterface(typeElement);
                System.out.println("" + typeElement + " with annotation " + pluginInterface);
            }
        }
        // for(Class cls : classes) {
            // TypeElement typeElement = elementUtils.getTypeElement(cls.getCanonicalName());
            // System.out.println("importPluginInterface:"+typeElement.getQualifiedName().toString());
        //    System.out.println("cls="+cls.getCanonicalName());
        // }
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }

    private void buildServiceSourceCode() {
        // all plugins are locals
        try {
            writePluginsManagerClass();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void writeServiceDeclaration() {

    }

    private void buildHostReceiverCode() {

    }

    private void writeReceiverForHost() {

    }

    private void buildExternalPluginAppReceiverCode() {

    }

    private void writeReceiverForExternalPluginApp() {

    }

    private void writePluginsManagerClass() throws IOException {
    }

    private void writePluginWrapperFor(Plugin annotatedClass) {

    }

}