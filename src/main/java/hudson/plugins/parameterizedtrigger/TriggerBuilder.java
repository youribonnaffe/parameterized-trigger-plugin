/*
 * The MIT License
 *
 * Copyright (c) 2010-2011, InfraDNA, Inc., Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
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

package hudson.plugins.parameterizedtrigger;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.IOException2;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * {@link Builder} that triggers other projects and optionally waits for their completion.
 *
 * @author Kohsuke Kawaguchi
 */
public class TriggerBuilder extends Builder {

	private final ArrayList<BlockableBuildTriggerConfig> configs;

    @DataBoundConstructor
	public TriggerBuilder(List<BlockableBuildTriggerConfig> configs) {
		this.configs = new ArrayList<BlockableBuildTriggerConfig>(Util.fixNull(configs));
	}

	public TriggerBuilder(BlockableBuildTriggerConfig... configs) {
		this(Arrays.asList(configs));
	}

	public List<BlockableBuildTriggerConfig> getConfigs() {
		return configs;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        Map<BlockableBuildTriggerConfig,List<Future<AbstractBuild>>> futures = new HashMap<BlockableBuildTriggerConfig, List<Future<AbstractBuild>>>();
		for (BlockableBuildTriggerConfig config : configs) {
            futures.put(config, config.perform(build, launcher, listener));
        }

        boolean buildStepResult = true;

        try {
            for (Entry<BlockableBuildTriggerConfig, List<Future<AbstractBuild>>> e : futures.entrySet()) {
                int n=0;
                BlockableBuildTriggerConfig config = e.getKey();
                List<AbstractProject> projectList = config.getProjectList(env);
                
                if(!projectList.isEmpty()){
	                AbstractProject p = projectList.get(n);
	                for (Future<AbstractBuild> f : e.getValue()) {
	                    try {
	                        listener.getLogger().println("Waiting for the completion of "+p.getFullDisplayName());
	                        AbstractBuild b = f.get();
	                        listener.getLogger().println(b.getFullDisplayName()+" completed. Result was "+b.getResult());
	                        
                            if(buildStepResult && config.getBlock().mapBuildStepResult(b.getResult())) {
                                build.setResult(config.getBlock().mapBuildResult(b.getResult()));
                            }
                            else {
                                buildStepResult = false;
                            }
	                    } catch (CancellationException x) {
	                        throw new AbortException(p.getFullDisplayName() +" aborted.");
	                    }
	                    n++;
	                }
                } else {
                	throw new AbortException("Build aborted. No projects to trigger. Check your configuration!");
                }
            }
        } catch (ExecutionException e) {
            throw new IOException2(e); // can't happen, I think.
        }

        return buildStepResult;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
		@Override
		public String getDisplayName() {
			return "Trigger/call builds on other projects";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
}
