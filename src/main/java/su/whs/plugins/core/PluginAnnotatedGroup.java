package su.whs.plugins.core;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import su.whs.plugins.annotations.Plugin;
import su.whs.plugins.annotations.PluginInterface;


public class PluginAnnotatedGroup extends AnnotatedGroup {
    private Types typeUtils;
    // private Map<String,List<Element>> interfacesMap = new HashMap<String,List<Element>>();
    private Map<TypeElement,TypeElement> pluginToInterfaceMap = new HashMap<TypeElement,TypeElement>();
    private PluginInterfacesGroup mInterfaces = null;
    public PluginAnnotatedGroup(Types typeUtils, PluginInterfacesGroup interfacesGroup) {
        super(typeUtils);
        mInterfaces = interfacesGroup;
        this.typeUtils = typeUtils;
    }

    public TypeElement addAndReturnInterface(TypeElement item) {
        if (contains(item)) return getInterfaceForPlugin(item);
        TypeElement typeElement = item;
        TypeElement currentClass = typeElement;
        while(true) {
            TypeMirror superClass = currentClass.getSuperclass();

            if (superClass.getKind()== TypeKind.NONE) {
                System.err.println("class "+typeElement.getQualifiedName()+" must be inherited from abstract class, annotated with @PluginInterface");
                break;
            }

            TypeElement superType = (TypeElement) typeUtils.asElement(superClass);
            System.out.println("superClass="+superType.getQualifiedName());
            PluginInterface pluginInterface = superType.getAnnotation(PluginInterface.class);
            if (pluginInterface!=null) {
                if (!mInterfaces.contains(superType)) {
                    mInterfaces.add(superType);
                }
                System.out.println("PluginInterface found, add Plugin " + typeElement.getQualifiedName());
       //         if (!interfacesMap.containsKey(superType.getQualifiedName())) {
       //             interfacesMap.put(superType.getQualifiedName().toString(),new ArrayList<Element>());
       //         }
       //         interfacesMap.get(superType.getQualifiedName().toString()).add(item);
                pluginToInterfaceMap.put(typeElement,superType);
                super.add(item);
                return superType;
            }
            currentClass = (TypeElement) typeUtils.asElement(superClass);
        }
        System.out.println("PluginInterface declaration for "+item.getClass().getSimpleName()+" not found, skip!");
        return null;
    }

    public TypeElement getInterfaceForPlugin(TypeElement plugin) {
        return pluginToInterfaceMap.get(plugin);
    }

    public boolean checkHostPackage(String manifestPackageName) {
        boolean result = true;
        for(Element item: mItems) {
            Plugin annotation = item.getAnnotation(Plugin.class);
            String forPackage = annotation.forPackage();
            if (forPackage.length()>0 && !manifestPackageName.equals(forPackage)) {
                result = false;
            }
        }
        return result;
    }

    // public List<Element> getImplementationsForInterface(TypeElement pluginInterface) {
    //    return interfacesMap.get(pluginInterface.getQualifiedName().toString());
    // }

    public void buildSourceCode(GenerationEnvironment env) {

    }
}
