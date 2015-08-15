package su.whs.plugins.core;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

/**
 * Created by igor n. boulliev on 12.08.15.
 */
public class TypeUtils {
    private static Map<String,Class> mTypesMap = new HashMap<String,Class>();

    static {
        mTypesMap.put("java.lang.String",String.class);
        mTypesMap.put("org.json.JSONObject",JSONObject.class);
        mTypesMap.put("org.json.JSONArray",JSONArray.class);
        mTypesMap.put("int",Integer.class);
        mTypesMap.put("boolean",Boolean.class);
        mTypesMap.put("char",Character.class);
        mTypesMap.put("float",Float.class);
    }

    public static boolean isArray(TypeMirror typeMirror) {
        String name = typeMirror.toString();
        if (name.endsWith("[]")) return true;
        return false;
    }
    public static Class getClassFor(TypeMirror typeMirror) {
        String name = typeMirror.toString();
        if (name.endsWith("[]")) {
            name = name.substring(0,name.length()-2);
        }
        if (!mTypesMap.containsKey(name)) {
            if ("void".equals(name))
                return null;
            System.err.println("unsupported type '"+name+"'");
        }
        return mTypesMap.get(name);
    }

    public static boolean isJSON(TypeMirror typeMirror) {
        return JSONObject.class.equals(getClassFor(typeMirror));
    }

    public static boolean isString(TypeMirror typeMirror) {
        return String.class.equals(getClassFor(typeMirror));
    }

    public static String asString(TypeMirror type) {
        if (isJSON(type)) {
            if (isArray(type)) {
                return "String[]";
            }
            return "String";
        } else if (isString(type)) {
            return "String";
        }
        return type.toString();
    }

    public class JSONObject {}
    public class JSONArray {}
}
