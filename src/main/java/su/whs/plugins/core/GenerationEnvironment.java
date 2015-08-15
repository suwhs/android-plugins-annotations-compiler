package su.whs.plugins.core;

import java.io.File;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Created by igor n. boulliev on 13.08.15.
 */
public class GenerationEnvironment {
    private final Types mTypeUtils;
    private final Elements mElementUtils;
    private final Filer mFiler;
    private String mBuildToolsVersion = "22.0.1";
    private Messager mMessager;
    private Map<String,String> mOptions = null;
    private AndroidBuildEnvironment mAndroidBuildEnvironment = null;

    public GenerationEnvironment( ProcessingEnvironment processingEnvironment, RoundEnvironment roundEnvironment) {
        mFiler = processingEnvironment.getFiler();
        mMessager = processingEnvironment.getMessager();
        mElementUtils = processingEnvironment.getElementUtils();
        mTypeUtils = processingEnvironment.getTypeUtils();
        mOptions = processingEnvironment.getOptions();
        mAndroidBuildEnvironment = new AndroidBuildEnvironment(roundEnvironment,processingEnvironment);
        if (mOptions.containsKey("buildToolVersion")) {
            mBuildToolsVersion = mOptions.get("buildToolVersion");
        }
    }

    public Filer getFiler() {
        return mFiler;
    }

    public String getAndroidSdkBuildTools() {
        return new StringBuilder(mAndroidBuildEnvironment.getAndroidSdkPath())
                .append(File.separator)
                .append("build-tools")
                .append(File.separator)
                .append(mBuildToolsVersion)
                .toString();
    }

    public Messager getMessager() {
        return mMessager;
    }

    public boolean isServiceDescribed() { return mAndroidBuildEnvironment.isServiceDescribed(); }

    public Elements getElementUtils() {
        return mElementUtils;
    }

    public String getAndroidPackageName() {
        return mAndroidBuildEnvironment.getManifestPackageName();
    }

    public boolean isServiceDefined() {
        return mAndroidBuildEnvironment.isServiceDefined();
    }

    public boolean isReceiverDefined() {
        return mAndroidBuildEnvironment.isReceiverDefined();
    }

    public Types getTypeUtils() {
        return mTypeUtils;
    }

    public String getAptSourcesRoot() {
        return mAndroidBuildEnvironment.getGeneratedSourcesRoot();
    }
}
