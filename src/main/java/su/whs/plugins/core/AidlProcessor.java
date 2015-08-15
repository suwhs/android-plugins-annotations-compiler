package su.whs.plugins.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Created by igor n. boulliev on 13.08.15.
 */
public class AidlProcessor {
    private String mPackageName = null;
    private String mInterfaceName = null;
    private String mClassName = null;
    private boolean mCodeGenerated = false;
    // private List<ExecutableElement> mMethods = new ArrayList<ExecutableElement>();
    private List<MarshalMethodDef> mMethods = new ArrayList<MarshalMethodDef>();
    private List<String> mParcelables = new ArrayList<String>();
    private GenerationEnvironment mEnvironment = null;

    protected AidlProcessor(GenerationEnvironment env,String packageName, String interfaceName) {
        mPackageName  = packageName;
        mClassName = interfaceName;
        mInterfaceName = "I"+interfaceName+"Cb";
        mEnvironment = env;
    }

    public AidlProcessor(GenerationEnvironment env, Element element, PluginInterfacesGroup interfacesGroup) {
        this(env, env.getElementUtils().getPackageOf(element).toString(), element.getSimpleName().toString());

        if (element instanceof TypeElement) {
            mMethods = interfacesGroup.getMarshalMethodDefs((TypeElement) element);
            /* TypeElement typeElement = (TypeElement)element;
            List<? extends Element> els = typeElement.getEnclosedElements();
            for(Element e : els) {
                if (e.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement)e;
                    addMethod(method);
                }
            } */
        } else {
            env.getMessager().printMessage(Diagnostic.Kind.ERROR," could not generate interface code for ",element);
        }
    }

    public void generate() throws IOException, InterruptedException {
        if (mCodeGenerated) throw new IllegalStateException("code already generated for '"+mInterfaceName+"'");
        mCodeGenerated = true;
        String aidlName = mInterfaceName + ".aidl";
        FileObject aidl = mEnvironment.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, mPackageName, aidlName);
        FileObject java = mEnvironment.getFiler().createSourceFile(mPackageName + "." + mInterfaceName);

        StringBuilder aidlCode = new StringBuilder("package ");
        aidlCode
                .append(mPackageName)
                .append(";")
                .append("\n\n");

        generateImports(aidlCode)
                .append("interface ")
                .append(mInterfaceName)
                .append(" {\n");

        for(String parcelable : mParcelables) {
            aidlCode
                    .append("parcelable ")
                    .append(parcelable)
                    .append(";\n");
        }

        generateMethods(aidlCode)
            .append("}\n");

        BufferedWriter aidlWriter = new BufferedWriter(aidl.openWriter());
        aidlWriter.write(aidlCode.toString());
        aidlWriter.close();
        String aidlPathName = aidl.toUri().toString();
        if (aidlPathName.startsWith("file:")) {
            if (aidlPathName.startsWith("file://")) {
                aidlPathName = aidlPathName.substring(7);
            } else {
                aidlPathName = aidlPathName.substring(5);
            }
        }
        Process aidlCompiler = executeAidlTool(aidlPathName);
        BufferedReader javaReader = new BufferedReader(new InputStreamReader(aidlCompiler.getInputStream()));
        BufferedWriter javaWriter = new BufferedWriter(java.openWriter());
        for(String line = javaReader.readLine(); line!=null; line = javaReader.readLine()) {
            javaWriter.write(line);
            javaWriter.newLine();
        }
        javaWriter.close();

        int aidlCompilerResult = aidlCompiler.waitFor();
        if (aidlCompilerResult!=0) {
            StringBuilder errorOutput = new StringBuilder();
            javaReader = new BufferedReader(new InputStreamReader(aidlCompiler.getErrorStream()));
            for(String line = javaReader.readLine(); line != null; line = javaReader.readLine()) {
                errorOutput.append(line).append("\n");
            }
            mEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR,"aidl compiler reports error:\n"+errorOutput.toString());
            return;
        }
        mEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE,mInterfaceName + ": interface code generated ");
    }

    protected StringBuilder generateImports(StringBuilder aidlCode) {
        return aidlCode;
    }

    protected StringBuilder generateMethods(StringBuilder aidlCode) {
        for(MarshalMethodDef method : mMethods) {
            aidlCode.append("    ").append(method.getInterfaceDeclaration());
            // generateAidlMethodDeclaration(method,aidlCode.append("\t"));
        }
        return aidlCode;
    }

    private void generateAidlMethodDeclaration(ExecutableElement method, StringBuilder code) {
        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> argsList = method.getParameters();
        StringBuilder methodArgsStr = new StringBuilder();
        TypeMirror returnType = method.getReturnType();
        code.append(TypeUtils.asString(returnType)).append(" ").append(methodName).append("(");

        for(VariableElement arg : argsList) {
            TypeMirror argType = arg.asType();
            methodArgsStr
                    .append("in ")
                    .append(TypeUtils.asString(argType))
                    .append(" ")
                    .append(arg.getSimpleName())
                    .append(", ");
        }
        if (methodArgsStr.length()>0) {
            methodArgsStr.replace(methodArgsStr.length()-2,methodArgsStr.length(),"");
        }
        code
                .append(methodArgsStr)
                .append(");");
    }

    private Process executeAidlTool(String aidlPath) throws IOException {
        String aidlToolCmd = new StringBuilder(mEnvironment.getAndroidSdkBuildTools())
                .append(File.separatorChar)
                .append("aidl ")
                .append("-I")
                .append(getAidlIncludeRoot(aidlPath))
                .append(" ")
                .append(aidlPath)
                .append(" -")
                .toString();
        mEnvironment
                .getMessager()
                .printMessage(Diagnostic.Kind.NOTE, "invoke aidl compiler '" + aidlToolCmd + "'");
        return Runtime.getRuntime().exec(aidlToolCmd);
    }

    private String getAidlIncludeRoot(String aidlPath) {
        String aptSourcesRoot = mEnvironment.getAptSourcesRoot();
        System.err.println("compare: '"+aidlPath+"'");
        System.err.println("    and: '"+aptSourcesRoot+"'");
        if (aidlPath.startsWith(aptSourcesRoot)) {
            System.err.println("match found!");
        }
        return aptSourcesRoot + File.separator + "debug";
    }

    public String getPackageName() { return mPackageName; }
    public String getInterfaceName() { return mInterfaceName; }
    public String getClassName() { return mClassName; }
}
