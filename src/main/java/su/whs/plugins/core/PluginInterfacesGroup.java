package su.whs.plugins.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Created by igor n. boulliev on 11.08.15.
 */
public class PluginInterfacesGroup extends AnnotatedGroup {

    static Map<TypeElement,List<MarshalMethodDef>> mMarshalMethodDefsMap = new HashMap<TypeElement,List<MarshalMethodDef>>();

    public PluginInterfacesGroup(Types typeUtils) {
        super(typeUtils);
    }

    public List<MarshalMethodDef> getMarshalMethodDefs(TypeElement pluginInterface) {
        return mMarshalMethodsListMap.get(pluginInterface);
    }

    @Override
    public void add(TypeElement element) {
        if (contains(element)) return;
        mMarshalMethodsListMap.put(element,buildMethodsDefs(element));
        super.add(element);
    }

    private List<MarshalMethodDef> buildMethodsDefs(TypeElement element) {
        List<MarshalMethodDef> result = new ArrayList<MarshalMethodDef>();

        TypeElement currentElement = element;
        List<? extends TypeMirror> interfacesMustImplements = currentElement.getInterfaces();

        for(TypeMirror interfaceType : interfacesMustImplements) {
            TypeElement interfaceElement = (TypeElement) mTypeUtils.asElement(interfaceType);
            System.err.println("scan interface'"+interfaceElement.getQualifiedName().toString()+"'");
            buildMethodsDefs(interfaceElement,result,true);

        }

        for(;currentElement!=null && !currentElement.getKind().equals(TypeKind.NONE);currentElement = (TypeElement)mTypeUtils.asElement(currentElement.getSuperclass())) {
            System.err.println("scan class'"+currentElement.getQualifiedName().toString()+"'");
            buildMethodsDefs(currentElement,result,false);
        }
        return result;
    }

    private void buildMethodsDefs(TypeElement element,List<MarshalMethodDef> methodsDefs, boolean isInterface) {
        List<? extends Element> els = element.getEnclosedElements();
        for (Element e : els) {
            if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) e;
                if (isInterface||e.getModifiers().contains(Modifier.ABSTRACT)) {
                    System.err.println("    element:"+e.getSimpleName().toString());
                        /* */
                    methodsDefs.add(buildMethodDef(method));
                }
            }
        }
    }

    private MarshalMethodDef buildMethodDef(ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();
        MarshalMethodDef def = new MarshalMethodDef();
        def.methodName = method.getSimpleName().toString();
        List<String> intArgs = new ArrayList<String>();
        List<String> implArgs = new ArrayList<String>();
        List<String> intModifiers = new ArrayList<String>();

        for(VariableElement arg : method.getParameters()) {
            String intArgType = TypeUtils.asString(arg.asType());
            intArgs.add(TypeUtils.isJSON(arg.asType()) ? "String" : intArgType);
            intModifiers.add("in ");
            implArgs.add(arg.asType().toString());
        }
        if (intArgs.size()>0) {
            def.interfaceArgsTypes = intArgs;
            def.implementationArgsTypes = implArgs;
            def.interfaceArgModifiers = intModifiers;
        }
        List<? extends TypeMirror> throwables = method.getThrownTypes();

        if (throwables!=null && throwables.size()>0) {
            def.throwables = new ArrayList<String>();
            for (TypeMirror throwable : method.getThrownTypes()) {
                String exceptionName = throwable.toString();
                if ("android.os.RemoteException".equals(exceptionName)) {
                    def.needHandleRemoteException = false;
                }
                if (def.throwables.contains(exceptionName)) continue;
                def.throwables.add(exceptionName);
                System.err.println("added throwable:"+exceptionName);
            }
        }
        def.implementationReturnType = returnType.toString();
        def.interfaceReturnType = TypeUtils.isJSON(returnType) ? "String" : returnType.toString();
        return def;
    }

    private List<String> mGeneratedList = new ArrayList<String>();

    public boolean isGenerated(TypeElement pluginInterface) {
        String fqdName = pluginInterface.getQualifiedName().toString();
        return mGeneratedList.contains(fqdName);
    }

    public void markAsGenerated(TypeElement pluginInterface) {
        String fqdName = pluginInterface.getQualifiedName().toString();
        mGeneratedList.add(fqdName);
    }
}
