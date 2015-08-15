package su.whs.plugins.core;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by igor n. boulliev on 11.08.15.
 */
public class AndroidBuildEnvironment {
    private org.w3c.dom.Element mXmlRoot = null;
    private static String androidManifestFile = null;
    private String mPackageName = null;
    private boolean mServiceDefined = false;
    private boolean mBroadcastReceiverDefined = false;
    private String mServiceName = null;
    private String mReceiverName = null;
    private String mAndroidSdkPath = null;
    private String mBuildRoot = null;
    private String mAptSourcesRoot = null;

    public AndroidBuildEnvironment(RoundEnvironment environment, ProcessingEnvironment processingEnvironment) {
        Map<String,String> options = processingEnvironment.getOptions();
        String tempMainfest = null;
        try {
             tempMainfest = findAndroidManifestFile(processingEnvironment);
        } catch (IOException e) {
            System.out.println("IOException while build pathnames");
        } catch (URISyntaxException e) {
            System.out.println("URLSyntaxException while build pathnames");
        }
        if (androidManifestFile==null) {
            System.out.println("try to find android manifest file");
            androidManifestFile = options.get("androidManifestFile");
            if (androidManifestFile == null) {
                System.out.println("androidManfestFile are null");
                try {
                    androidManifestFile = (tempMainfest == null ? findAndroidManifestFile(processingEnvironment) : tempMainfest);
                    if (androidManifestFile!=null) {
                        System.out.println("manifest: '"+androidManifestFile+"'");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            parse(new File(androidManifestFile));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
    public boolean isServiceDescribed() {
        return false;
    }

    private String findAndroidManifestFile(ProcessingEnvironment processingEnvironment) throws IOException, URISyntaxException {
        Filer filer = processingEnvironment.getFiler();
        FileObject stub = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "stub");
        String path = stub.toUri().toString();
        if (!path.startsWith("file://")) {
            if (path.startsWith("file:")) {
                path = "file://" + path.substring(5,path.length());
            } else {
                path = "file://" + path;
            }
        }
        URI uri = new URI(path);
        File f = new File(uri);
        File sourceCodeGenerationFolder = f.getParentFile();

        // now lookup
        System.out.println("sourceCodeGenerationFolder = '"+sourceCodeGenerationFolder+"'");
        File buildFolder = new File(sourceCodeGenerationFolder,"build");
        File parent = sourceCodeGenerationFolder.getParentFile();
        while ((!buildFolder.exists() || !buildFolder.isDirectory()) && buildFolder.getParentFile()!=null) {
            buildFolder = new File(parent,"build");
            parent = parent.getParentFile();
        }
        if (buildFolder.exists()) {
            System.out.println("build folder found '"+buildFolder.getAbsolutePath()+"'");
            mBuildRoot = buildFolder.getAbsolutePath();
            mAptSourcesRoot = mBuildRoot + File.separator + "generated" + File.separator + "source" + File.separator + "apt";
            File moduleRoot = buildFolder.getParentFile();
            lookupLocalProperties(moduleRoot);
            File srcRoot = new File(moduleRoot,"src");
            if (srcRoot.exists()) {
                File manifestFile = new File(srcRoot,"main/AndroidManifest.xml");
                if (manifestFile.exists()) {
                    return manifestFile.getAbsolutePath();
                } else {
                    System.out.println("could not find AndroidManifest at '"+manifestFile.getAbsolutePath()+"'");
                }
            } else {
                System.out.println(srcRoot.getAbsolutePath()+" not exists?");
            }
        } else {
            System.out.println("build folder NOT found");
        }
        return null;
    }

    private void lookupLocalProperties(File dir) {
        File localPropertiesFile = new File(dir,"local.properties");
        while(!localPropertiesFile.exists() && dir.getParentFile()!=null) {
            dir = dir.getParentFile();
            localPropertiesFile = new File(dir,"local.properties");
        }
        if (localPropertiesFile.exists()) {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(localPropertiesFile));
                for(Object k: props.keySet()) {
                    System.out.println("prop key:"+k+"="+props.get(k));
                }
                mAndroidSdkPath = props.getProperty("sdk.dir");
                System.out.println("SDK PATH='"+mAndroidSdkPath+"'");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parse(File androidManifestXml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = xmlFactory.newDocumentBuilder();
        Document doc = builder.parse(androidManifestXml);
        if (doc==null) {
            System.out.println("could not parse "+androidManifestXml);
        }
        org.w3c.dom.Element root = doc.getDocumentElement();
        root.normalize();
        mXmlRoot = root;
        NodeList applications = doc.getElementsByTagName("application");
        Node packageName = root.getAttributes().getNamedItem("package"); // applicationDef.getAttributes().getNamedItemNS("","package");

        if (packageName==null) {
            System.out.println("node '"+root.toString()+"' has no 'package' attribute");
            return;
        }

        mPackageName = packageName.getNodeValue();
        mServiceName = mPackageName + ".PluginsHostService";
        mReceiverName = mPackageName + ".PluginsBroadcastReceiver";

        System.out.println("processing packageName='" + mPackageName + "'");
        if (applications==null) {
            System.out.println("no 'application' tag found");
        } else {
            serviceLookupLoop:
            for (int i=0; i< applications.getLength(); i++) {
                Node applicationDef = applications.item(0);
                NodeList childNodes = applicationDef.getChildNodes();
                for (int j=0; j<childNodes.getLength(); j++) {
                    Node testNode = childNodes.item(j);
                    if ("service".equals(testNode.getNodeName())) {
                        System.out.println("service found");
                        NamedNodeMap attributes = testNode.getAttributes();
                        Node name = attributes.getNamedItemNS("android","name");
                        if (name!=null) {
                            String serviceName = name.getNodeValue();
                            if (mServiceName.equals(serviceName)) {
                                mServiceDefined = true;
                            }
                        }
                    }
                    if ("receiver".equals(testNode.getNodeName())) {
                        System.out.println("receiver");
                        NamedNodeMap attributes = testNode.getAttributes();
                        Node name = attributes.getNamedItemNS("android","name");
                        if (name!=null) {
                            String serviceName = name.getNodeValue();
                            if (mReceiverName.equals(serviceName)) {
                                mBroadcastReceiverDefined = true;
                            }
                        }
                    }
                }
            }
        }


    }

    public String getManifestPackageName() {
        return mPackageName;
    }

    public boolean isServiceDefined() { return mServiceDefined; }
    public boolean isReceiverDefined() { return mBroadcastReceiverDefined; }
    public String getAndroidSdkPath() { return mAndroidSdkPath; }
    public void writeServiceDeclaration() {

    }

    public void writeReceiverForHost() {

    }

    public void writeReceiverForExternalPluginApp() {

    }

    public String getGeneratedSourcesRoot() {
        return mAptSourcesRoot;
    }
}
