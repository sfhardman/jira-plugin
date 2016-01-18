package hudson.plugins.jira;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;

import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;

/**
 * Adds JIRA related environment variables to the build
 */
public class JiraEnvironmentVariableBuilder extends Builder implements SimpleBuildStep {
    
    private UpdaterIssueSelector issueSelector;
    
    @DataBoundConstructor
    public JiraEnvironmentVariableBuilder(UpdaterIssueSelector issueSelector) {
        this.issueSelector = issueSelector;
    }
    
    public UpdaterIssueSelector getIssueSelector() {
        UpdaterIssueSelector uis = this.issueSelector;
        if (uis == null) uis = new DefaultUpdaterIssueSelector();
        return (this.issueSelector = uis);
    }
    
    JiraSite getSiteForProject(Job<?, ?> job) {
        return JiraSite.get(job);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        JiraSite site = getSiteForProject(run.getParent());

        if (site == null) {
            throw new AbortException(Messages.JiraEnvironmentVariableBuilder_NoJiraSite());
        }

        Set<String> ids = getIssueSelector().findIssueIds(run, site, taskListener);

        String idList = StringUtils.join(ids, ",");

        taskListener.getLogger().println(Messages.JiraEnvironmentVariableBuilder_Updating(JiraEnvironmentContributingAction.ISSUES_VARIABLE_NAME, idList));

        run.addAction(new JiraEnvironmentContributingAction(idList, site.getName()));
    }

    /**
    * Descriptor for {@link JiraEnvironmentVariableBuilder}.
    */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> klass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraEnvironmentVariableBuilder_DisplayName();
        }
    }
}
