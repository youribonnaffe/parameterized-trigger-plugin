/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Tom Huybrechts
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.parameterizedtrigger.test;

import hudson.EnvVars;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.*;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiLineJobsBuildTriggerConfigTest extends HudsonTestCase {

    public void testJobListWithMultiLineInputNoComma() throws Exception {
        Project<?, ?> projectA = createTriggerProject("projectB\r\nprojectC");

        Project<?, ?> projectB = createTriggeredProject("projectB");
        Project<?, ?> projectC = createTriggeredProject("projectC");

        projectA.scheduleBuild2(0, new Cause.UserCause()).get();
        assertTrue(hudson.getQueue().contains(projectB));
        assertTrue(hudson.getQueue().contains(projectC));
        BlockableBuildTriggerConfig blockableBuildTriggerConfig = projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0);
        List<AbstractProject> projectList = blockableBuildTriggerConfig.getProjectList(new EnvVars());
        assertEquals(2, projectList.size());
    }

    public void testJobListWithMultiLineInputMixed() throws Exception {
        Project<?, ?> projectA = createTriggerProject("projectB\r\nprojectC,projectD");

        Project<?, ?> projectB = createTriggeredProject("projectB");
        Project<?, ?> projectC = createTriggeredProject("projectC");
        Project<?, ?> projectD = createTriggeredProject("projectD");

        BlockableBuildTriggerConfig blockableBuildTriggerConfig = projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0);
        List<AbstractProject> projectList = blockableBuildTriggerConfig.getProjectList(new EnvVars());
        assertEquals(3, projectList.size());

        projectA.scheduleBuild2(0, new Cause.UserCause()).get();
        assertTrue(hudson.getQueue().contains(projectB));
        assertTrue(hudson.getQueue().contains(projectC));
        assertTrue(hudson.getQueue().contains(projectD));
    }

    public void testJobListWithMonoLineInput() throws Exception {
        Project<?, ?> projectA = createTriggerProject("projectB,projectC");

        Project<?, ?> projectB = createTriggeredProject("projectB");
        Project<?, ?> projectC = createTriggeredProject("projectC");

        projectA.scheduleBuild2(0, new Cause.UserCause()).get();
        assertTrue(hudson.getQueue().contains(projectB));
        assertTrue(hudson.getQueue().contains(projectC));
        BlockableBuildTriggerConfig blockableBuildTriggerConfig = projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0);
        List<AbstractProject> projectList = blockableBuildTriggerConfig.getProjectList(new EnvVars());
        assertEquals(2, projectList.size());
    }

    private Project<?, ?> createTriggerProject(String triggeredProjects) throws IOException {
        Project<?, ?> projectA = createFreeStyleProject("projectA");
        List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
        buildParameters.add(new CurrentBuildParameters());
        projectA.getBuildersList().add(new TriggerBuilder(new BlockableBuildTriggerConfig(triggeredProjects, null, buildParameters)));
        return projectA;
    }

    private Project<?, ?> createTriggeredProject(String projectName) throws IOException {
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project<?, ?> projectB = createFreeStyleProject(projectName);
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(100);
        hudson.rebuildDependencyGraph();
        return projectB;
    }
}
