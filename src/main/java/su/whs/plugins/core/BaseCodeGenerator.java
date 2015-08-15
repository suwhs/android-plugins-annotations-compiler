package su.whs.plugins.core;

import java.io.IOException;

import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Created by igor n. boulliev on 14.08.15.
 */
public class BaseCodeGenerator {
    protected GenerationEnvironment mEnvironment;

    protected PluginAnnotatedGroup mPluginsGroup;
    protected PluginInterfacesGroup mInterfacesGroup;

    private class ServiceAidlProcessor extends AidlProcessor {
        protected ServiceAidlProcessor(GenerationEnvironment env, String packageName, String interfaceName) {
            super(env, packageName, interfaceName);
        }

        @Override
        protected StringBuilder generateImports(StringBuilder aidlCode) {
            super.generateImports(aidlCode);
            for(TypeElement pluginInterface : mInterfacesGroup.mItems) {
                String className = pluginInterface.getSimpleName().toString();
                String packageName = mEnvironment.getElementUtils().getPackageOf(pluginInterface).toString();
                aidlCode.append("import ").append(packageName).append(".I").append(className).append("Cb;\n");
            }
            return aidlCode;
        }

        @Override
        protected StringBuilder generateMethods(StringBuilder aidlCode) {
            for(TypeElement pluginInterface : mInterfacesGroup.mItems) {
                String className = pluginInterface.getSimpleName().toString();
                String interfaceName = new StringBuilder("I").append(className).append("Cb").toString();
                aidlCode.append("    void register_")
                        .append(interfaceName)
                        .append("(in ").append(interfaceName)
                        .append(" arg0);\n");
                aidlCode.append("    void unregister_")
                        .append(interfaceName)
                        .append("(in ").append(interfaceName)
                        .append(" arg0);\n");
            }
            return aidlCode;
        }
    }

    public BaseCodeGenerator(GenerationEnvironment env, PluginInterfacesGroup interfaces, PluginAnnotatedGroup plugins) {
        mEnvironment = env;
        mInterfacesGroup = interfaces;
        mPluginsGroup = plugins;
    }


    public void generate() throws IOException, InterruptedException {
                /* first, generate interfaces aidl/java code */

        for(TypeElement pluginInterface : mInterfacesGroup.mItems) {
            if (mInterfacesGroup.isGenerated(pluginInterface)) continue;
            AidlProcessor processor = new AidlProcessor(mEnvironment,pluginInterface,mInterfacesGroup);
            processor.generate();
            mInterfacesGroup.markAsGenerated(pluginInterface);
        }
        /* next, generate <application.package.IWHSPluginsHostServiceCb aidl/java */
        ServiceAidlProcessor serviceAidlProcessor = new ServiceAidlProcessor(mEnvironment,mEnvironment.getAndroidPackageName(),"WHSPluginsHostService");
        serviceAidlProcessor.generate();
    }

    protected void msg(Diagnostic.Kind kind,String format, Object... args) {
        mEnvironment.getMessager().printMessage(
                kind,
                getClass().getSimpleName() + ":" +
                String.format(format, args)
        );
    }

    protected void log(String format, Object... args) {
        msg(Diagnostic.Kind.NOTE, format, args);
    }

}
