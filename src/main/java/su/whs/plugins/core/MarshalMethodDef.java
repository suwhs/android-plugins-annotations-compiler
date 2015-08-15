package su.whs.plugins.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor n. boulliev on 14.08.15.
 */
public class MarshalMethodDef {
    public String methodName;
    public List<String> implementationArgsTypes;
    public List<String> interfaceArgModifiers;
    public List<String> interfaceArgsTypes;
    public String implementationReturnType;
    public String interfaceReturnType;
    public ArrayList<String> throwables;
    public boolean needHandleRemoteException = true;

    public String getInterfaceDeclaration() {
        StringBuilder code = new StringBuilder();
        code
            .append(interfaceReturnType)
            .append(" ").append(methodName)
            .append("(");
        if (implementationArgsTypes!=null && implementationArgsTypes.size()>0) {
            for (int i = 0; i < implementationArgsTypes.size(); i++) {
                code
                        .append(interfaceArgModifiers.get(i))
                        .append(interfaceArgsTypes.get(i))
                        .append(" arg").append(i).append(", ");
            }
            code.replace(code.length() - 2, code.length(), "");
        }
        code.append(");\n");
        return code.toString();
    }

    public String getImplementationDeclaration() {
        StringBuilder code = new StringBuilder();
        code
            .append(implementationReturnType)
                .append(" ").append(methodName)
                .append("(");
        if(implementationArgsTypes!=null&&implementationArgsTypes.size()>0) {
            for (int i=0; i<implementationArgsTypes.size();i++) {
                code.append(implementationArgsTypes.get(i)).append(" arg").append(i).append(", ");
            }
            code.replace(code.length() - 2, code.length(), "");
        }
        code.append(")");
        if (throwables!=null && throwables.size()>0) {
            code.append(" throws ");
            for(String t : throwables) {
                code.append(t).append(", ");
            }
            code.replace(code.length()-2,code.length(),"");
        }
        return code.toString();
    }

    public boolean canThrowJSONException() {
        if (implementationArgsTypes==null||implementationArgsTypes.size()<1) return false;
        for (String type : implementationArgsTypes) {
            if (type.contains("JSONObject")) return true;
        }
        return false;
    }

    public String getImplementationCallCode(String instanceName,String... args) {
        StringBuilder code = new StringBuilder(instanceName).append(".").append(methodName).append("(");
        if (interfaceArgsTypes!=null&&interfaceArgsTypes.size()>0) {
            for(int i=0; i<interfaceArgsTypes.size();i++) {
                String expectedType = implementationArgsTypes.get(i);
                String interfaceType = interfaceArgsTypes.get(i);
                String passedArg = "arg"+String.valueOf(i);
                if (expectedType.contains("JSONObject")) {
                    passedArg = "new JSONObject(" + passedArg + ")";
                }
                code.append(passedArg).append(", ");
            }
            code.replace(code.length()-2,code.length(),"");
        }
        return code.append(")").toString();
    }

    public String getInterfaceCallCode(String interfaceName, String... args) {
        StringBuilder code = new StringBuilder(interfaceName).append(".").append(methodName).append("(");
        if (interfaceArgsTypes!=null&&interfaceArgsTypes.size()>0) {
            for(int i=0; i<interfaceArgsTypes.size();i++) {
                String passedType = implementationArgsTypes.get(i);
                String interfaceType = interfaceArgsTypes.get(i);
                String passedArg = "arg"+String.valueOf(i);
                if (passedType.contains("JSONObject")) {
                    passedArg = "arg"+String.valueOf(i)+".toString()";
                }
                code.append(passedArg).append(", ");
            }
            code.replace(code.length()-2,code.length(),"");
        }
        return code.append(")").toString();
    }


    public boolean isReturnsJSON() {
        return interfaceReturnType.contains("JSONObject");
    }


}
