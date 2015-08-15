package su.whs.plugins.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

/**
 * Created by igor n. boulliev on 11.08.15.
 */
public class AnnotatedGroup {
    protected List<TypeElement> mItems = new ArrayList<TypeElement>();
    protected Map<TypeElement,List<MarshalMethodDef>> mMarshalMethodsListMap = new HashMap<TypeElement,List<MarshalMethodDef>>();
    protected Types mTypeUtils;

    public AnnotatedGroup(Types typeUtils) {
        mTypeUtils = typeUtils;
    }
    public void add(TypeElement annotatedElement) {
        System.out.println(getClass().getSimpleName()+".add("+annotatedElement.getSimpleName()+")");
        mItems.add(annotatedElement);
    }
    public List<TypeElement> all() { return mItems; }

    public List<MarshalMethodDef> getMarshalMethodDef(TypeElement typeElement) {
        if (!mMarshalMethodsListMap.containsKey(typeElement)) {
            /* generate marshalMethodsDefs */
        }
        return mMarshalMethodsListMap.get(typeElement);
    }

    public boolean contains(TypeElement pluginInterface) {
        String fqdnName = pluginInterface.getQualifiedName().toString();
        for (TypeElement item : mItems) {
            if (fqdnName.equals(item.getQualifiedName().toString())) return true;
        }
        System.out.println("add:"+fqdnName);
        return mItems.contains(pluginInterface);
    }
}
