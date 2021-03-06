package cn.hikyson.methodcanary.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Project

public class MethodCanaryTransform extends Transform {

    private Project mProject;

    public MethodCanaryTransform(Project project) {
        this.mProject = project
    }

    @Override
    String getName() {
        return "MethodCanaryTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        mProject.logger.quiet("[AndroidGodEye][MethodCanary] ======================== transform start ========================")
        def startTime = System.currentTimeMillis()
        TransformHandler.handle(mProject, transformInvocation)
        def cost = (System.currentTimeMillis() - startTime) / 1000
        mProject.logger.quiet("[AndroidGodEye][MethodCanary] Submit issue in [https://github.com/Kyson/AndroidGodEye/issues] if you have any question.")
        mProject.logger.quiet("[AndroidGodEye][MethodCanary] ======================== transform end, cost " + cost + " s ========================")
    }
}