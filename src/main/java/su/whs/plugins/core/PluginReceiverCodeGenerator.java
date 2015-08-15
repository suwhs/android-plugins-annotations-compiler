package su.whs.plugins.core;

import java.io.IOException;
import java.util.List;

import javax.lang.model.element.TypeElement;

import su.whs.plugins.annotations.Plugin;

/**
 * Created by igor n. boulliev on 14.08.15.
 */
public class PluginReceiverCodeGenerator extends BaseCodeGenerator {

    public PluginReceiverCodeGenerator(GenerationEnvironment env, PluginInterfacesGroup interfaces, PluginAnnotatedGroup plugins) {
        super(env, interfaces, plugins);
    }

    @Override
    public void generate() throws IOException, InterruptedException {
        super.generate();
        /* next, generate BroadcastReceiver WHSPluginsBroadcastReceiver */
        /* connect to IWHSHostServiceCb */
        StringBuilder broadcastReceiverCode = new StringBuilder();
        broadcastReceiverCode.append("package ").append(mEnvironment.getAndroidPackageName()).append(";\n\n");

        injectImports(broadcastReceiverCode);

        injectReceiverCode(broadcastReceiverCode);
    }

    public String getReceiverClassName() {
        return "WHSPluginsBroadcastReceiver";
    }

    private String mPluginTargetPackage = null;

    public String getPluginTargetPackage() {
        if (mPluginTargetPackage==null) {
            for (TypeElement plugin : mPluginsGroup.mItems) {
                Plugin annotation = plugin.getAnnotation(Plugin.class);
                if (annotation!=null) {
                    mPluginTargetPackage = annotation.forPackage();
                    break;
                }
            }
        }
        return mPluginTargetPackage;
    }

    public String getManifestInjectCode() {
        return new StringBuilder("<receiver android:name=\".")
                .append(getReceiverClassName())
                .append("\" android:enabled=true>")
                .append("\n")
                .append("<intent-filter>\n")
                .append("<action android:name=\"")
                .append(getReceiverAction())
                .append("\"/>\n")
                .append("</intent-filter>\n</receiver>").toString();
    }

    private static String[] DEFAULT_JAVA_IMPORTS = new String[] {
            "android.content.BroadcastReceiver",
            "android.content.Context",
            "android.content.Intent",
            "android.os.Bundle",
            "import android.util.Log"
    };

    private StringBuilder injectImports(StringBuilder code) {
        for (String importPackage : DEFAULT_JAVA_IMPORTS) {
            code.append("import ").append(importPackage).append(";\n");
        }

        for (TypeElement plugin : mPluginsGroup.mItems) {
            code.append("import ").append(plugin.getQualifiedName()).append(";\n");
        }

        for (TypeElement pluginInterface : mInterfacesGroup.mItems) {
            String pp = mEnvironment.getElementUtils().getPackageOf(pluginInterface).getQualifiedName().toString();
            String cn = pluginInterface.getSimpleName().toString();
            code.append("import ").append(pp).append("I").append(cn).append("Cb;\n");
        }

        return code.append("\n");
    }

    private StringBuilder injectReceiverCode(StringBuilder code) {
        code    .append("/* PluginReceiverCodeGenerator.injectReceiverCode */\n")
                .append("public class ")
                .append(getReceiverClassName())
                .append(" extends BroadcastReceiver {\n")
                .append("    private ").append(Constants.whsHostServiceInterface).append(" mHostSvc;\n")
                .append("    private boolean mIsRegistered = false;\n")
                .append("    private List<IPluginInterface> mRegistered = new ArrayList<IPluginInterface>();\n")
                .append(generateServiceConnection())
                .append("    @Override\n     public void onReceive(Context context, Intent intent) {\n")
                .append("        if (\"").append(getReceiverAction()).append("\".equals(intent.getAction()) {\n")
                .append("            handlePresenceAction(context,intent)\n        }\n    }\n\n");

        code    .append("    private void handlePresenceAction(Context context, Intent intent) {\n")
                .append(generatePresenceHandler())
                .append("     }")
                .append("    private void registerPlugins() {\n");
        for (TypeElement plugin : mPluginsGroup.mItems) {
            code.append(generatePluginRegistrationCode(plugin));
        }
        code    .append("\n    mIsRegistered = true;\n    }")
                .append("    private void unregisterPlugins() {\n")
                .append("        for(IPpluginInterface ipi : mRegistered) ipi.unregister();\n")
                .append("        mRegistered.clear();\n    mIsRegistered = false;\n    }\n");
        return code;
    }

    private String getReceiverAction() {
        return new StringBuilder(Constants.whsHostBroadcastIntentPrefix)
                .append(mPluginTargetPackage)
                .append(Constants.whsHostBroadcastIntentSuffix).toString();
    }

    private String generateServiceConnection() {
        StringBuilder serviceConn = new StringBuilder("    private ServiceConnection mServiceConn = new ServiceConnection() {\n")
                .append("        @Override\n")
                .append("        public void onServiceConnected(ComponentName name, IBinder service) {\n")
                .append("             mHostSvc = ").append(Constants.whsHostServiceStub).append(".asInterface(service);\n")
                .append("             registerPlugins();\n")
                .append("        }\n")
                .append("        @Override\n")
                .append("        public void onServiceDisconnected(ComponentName name) {\n")
                .append("            unregisterPlugins();\n")
                .append("        }\n")
                .append("    }\n");
        return serviceConn.toString();
    }

    private String generatePresenceHandler() {
        StringBuilder handler = new StringBuilder();
        handler.append("         if (mRegistered) return;\n");
        return handler.toString();
    }

    private String generatePluginRegistrationCode(TypeElement plugin) {
        StringBuilder code = new StringBuilder();
        TypeElement pluginInterface = mPluginsGroup.getInterfaceForPlugin(plugin);
        String interfaceName = "I"+pluginInterface.getSimpleName().toString()+"Cb";
        code
                .append("final ").append(plugin.getSimpleName()).append(" _").append(plugin.getSimpleName())
                .append(" = new ").append(plugin.getSimpleName()).append("();\n")

                .append("mHostSvc.register_").append(interfaceName).append("(new ").append(interfaceName)
                .append(".Stub() {\n");
        injectInterfaceStub(pluginInterface, "_" + plugin.getSimpleName().toString(), code)
                .append("}\n");
        /* */

        return code.toString();
    }

    private StringBuilder injectInterfaceStub(TypeElement pluginInterface, String instance, StringBuilder code) {

        // marshalled method declarations!
        List<MarshalMethodDef> methods = mInterfacesGroup.getMarshalMethodDef(pluginInterface);

        return code;
    }
}
