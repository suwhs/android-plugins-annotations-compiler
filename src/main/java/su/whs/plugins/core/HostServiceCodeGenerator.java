package su.whs.plugins.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Created by igor n. boulliev on 13.08.15.
 */
public class HostServiceCodeGenerator extends BaseCodeGenerator {

    public HostServiceCodeGenerator(GenerationEnvironment env, PluginInterfacesGroup interfaces, PluginAnnotatedGroup plugins) {
        super(env, interfaces, plugins);
    }

    @Override
    public void generate() throws IOException, InterruptedException {
        log("call super generator first!");
        super.generate();
        log("HostServiceGenerator.generate()");
        /* and then - generate application.package.WHSPluginsHostService class */

        String packageName = mEnvironment.getAndroidPackageName();
        String className = "WHSPluginsHostService";
        String interfaceName = new StringBuilder("I").append(className).append("Cb").toString();

        injectPackageManager();

        StringBuilder javaCode = new StringBuilder("/* DO NOT MODIFY - HostServiceCodeGenerator.generate() */\n");
        javaCode.append("package ").append(packageName).append(";\n\n");
        injectJavaImports(javaCode)
                .append("public class ").append(className).append(" extends Service {\n")
                .append("    private ").append(interfaceName).append(".Stub mInstance = new ").append(interfaceName).append(".Stub() {\n")
                .append("        private Map<IInterface,IPluginInterface> mRegisteredInterfacesMap = new HashMap<IInterface,IPluginInterface>();\n");

        injectInterfaces(javaCode);

        injectRemoteExceptionHandlerCode(javaCode)
                .append("    };\n")
                .append("    @Nullable\n")
                .append("    @Override\n")
                .append("    public IBinder onBind(Intent intent) {\n")
                .append("        return mInstance;\n    }\n}\n");

        JavaFileObject serviceFile = mEnvironment.getFiler().createSourceFile(packageName+"."+"WHSPluginsHostService");
        Writer serviceWriter = serviceFile.openWriter();
        serviceWriter.write(javaCode.toString());
        serviceWriter.close();
    }

    private static final String[] DEFAULT_IMPORTS = new String[] {
            "android.app.Service",
            "android.content.Intent",
            "android.os.IBinder",
            "android.os.IInterface",
            "android.os.RemoteException",
            "android.support.annotation.Nullable",

            "org.json.JSONException",
            "org.json.JSONObject",

            "java.util.HashMap",
            "java.util.Map",

            "su.whs.plugins.annotations.IPluginInterface"
    };

    private StringBuilder injectJavaImports(StringBuilder code) {
        log("injectJavaImports");
        for (String packageName : DEFAULT_IMPORTS) {
            code.append("import ").append(packageName).append(";\n");
        }
        for (TypeElement pluginInterface : mInterfacesGroup.mItems) {
            String packageName = mEnvironment.getElementUtils().getPackageOf(pluginInterface).getQualifiedName().toString();
            String className = pluginInterface.getSimpleName().toString();
            code.append("import ").append(packageName).append(".").append(className).append(";\n");
            code.append("import ").append(packageName).append(".I").append(className).append("Cb;\n");
        }
        return code;
    }

    private StringBuilder injectInterfaces(StringBuilder code) {
        log("injectInterfaces");
        for(TypeElement pluginInterface : mInterfacesGroup.mItems) {
            injectInterfaceRegisterCode(pluginInterface, code);
            injectInterfaceUnregisterCode(pluginInterface, code);
        }
        return code;
    }

    private StringBuilder injectRemoteExceptionHandlerCode(StringBuilder code) {
        log("injectRemoteExceptionHandler");
        code.append("// HostServiceCodeGenerator.injectRemoteExceptionHandlerCode\n")
                .append("void collectRemoteException(IInterface arg0, RemoteException e) {\n")
                .append("    if (mRegisteredInterfacesMap.containsKey(arg0)) {\n")
                .append("        PluginsManager.getInstance().notifyOnRemoteException(mRegisteredInterfacesMap.get(arg0),e);\n")
                .append("    }\n}\n");
        return code;
    }

    private StringBuilder injectInterfaceRegisterCode(TypeElement pluginInterface, StringBuilder code) {
        log("injectInterfaceRegisteredCode");
        String className = pluginInterface.getSimpleName().toString();
        String interfaceName = new StringBuilder("I").append(className).append("Cb").toString();
        code.append("            @Override\n            public void register_")
                .append(interfaceName).append("(").append(interfaceName).append(" arg0) throws RemoteException {\n")
                .append("                final ").append(interfaceName).append(" _instance = arg0;\n")
                .append("                ").append(className).append(" _").append(className).append(" = ");

        code.append(buildInterfaceRegisterCode(pluginInterface,"_instance"));
        code.append("            }\n");
        return code;
    }

    private StringBuilder injectInterfaceUnregisterCode(TypeElement pluginInterface, StringBuilder code) {
        log("injectInterfaceUnregisterCode");
        String className = pluginInterface.getSimpleName().toString();
        String interfaceName = new StringBuilder("I").append(className).append("Cb").toString();
        code.append(" /* HostServiceCodeGenerator.injectInterfaceUnregisterCode */\n");
        code.append("            @Override\n            public void unregister_")
                .append(interfaceName).append("(").append(interfaceName).append(" arg0) throws RemoteException {\n")
                .append("                ")
                .append("if (mRegisteredInterfacesMap.containsKey(arg0)) {\n")
                .append("                    ").append(className).append(" _").append(className).append(" = (").append(className).append(")mRegisteredInterfacesMap.remove(arg0);\n")
                .append("                   _").append(className).append(".unregistered();\n")
                .append("                    ").append("PluginsManager.getInstance().unregister(_").append(className).append(");\n                }\n            }\n");
        return code;
    }

    private String buildInterfaceRegisterCode(TypeElement pluginInterface, String instanceName) {
        StringBuilder code = new StringBuilder(" /* HostServiceCodeGenerator.buildInterfaceRegisterCode */ ");
        code
                .append("new ").append(pluginInterface.getSimpleName()).append("() {\n");
        if (mInterfacesGroup==null)
            System.err.println("NULL HERE 01");
        List<MarshalMethodDef> methods = mInterfacesGroup.getMarshalMethodDefs(pluginInterface);
        if (methods!=null)
        for (MarshalMethodDef def : methods) {
            boolean tryExists = false;
            code
                    .append("@Override\n")
                    .append("public ")
                    .append(def.getImplementationDeclaration())
                    .append("{\n");
            if (def.needHandleRemoteException||def.canThrowJSONException()) {
                code.append("try {\n");
                tryExists = true;
            }
            code.append("                 ");
            if (!"void".equals(def.implementationReturnType)) {
                code.append("return ");
            }

            String callDecl = def.getInterfaceCallCode(instanceName);
            if (def.implementationReturnType.contains("JSONObject")) {
                callDecl = "new JSONObject(" + callDecl + ")";
            }
            code.append(callDecl);
            code.append(";\n");

            if (tryExists) {
                if (def.needHandleRemoteException) {
                    code.append("} catch(android.os.RemoteException re) {\n")
                            .append("collectRemoteException(").append(instanceName).append(",re);\n");
                }
                if (def.canThrowJSONException()) {
                    code.append("} catch(org.json.JSONException je) {\n")
                            .append("return null;\n");
                }
                code.append("}\n");
            }

            code.append("\n}\n");
        }
        code.append("};\n /* end buildInterfaceRegisterCode */\n");
        return code.toString();
    }

    private void injectPackageManager() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("PluginsManager.java.template");
        String packageName = mEnvironment.getAndroidPackageName();
        /* create java file */
        try {
            JavaFileObject javaFileObject = mEnvironment.getFiler().createSourceFile(packageName+".PluginsManager");
            Writer javaWriter = javaFileObject.openWriter();
            BufferedReader templateReader = new BufferedReader(new InputStreamReader(is));
            javaWriter.write(String.format("package %s;\n\n",packageName));
            for(String line=templateReader.readLine();line!=null;line=templateReader.readLine()) {
                javaWriter.write(line+"\n");
            }
            javaWriter.close();
            templateReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write first string 'package <app.package>;
        // copy PackageManager from resources
    }
}
