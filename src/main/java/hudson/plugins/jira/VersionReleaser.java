package hudson.plugins.jira;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static hudson.plugins.jira.JiraVersionMatcher.hasName;
import static org.hamcrest.Matchers.equalTo;

/**
 * used by JiraReleaseVersionUpdaterBuilder to mark a version as released
 */
public class VersionReleaser {

    static boolean perform(JiraSite site, String jiraProjectKey, String jiraRelease, AbstractBuild<?, ?> build, BuildListener listener) {
        String realRelease = "NOT_SET";
        String realProjectKey = null;

        try {
            realRelease = build.getEnvironment(listener).expand(jiraRelease);
            realProjectKey = build.getEnvironment(listener).expand(jiraProjectKey);

            if (realRelease == null || realRelease.isEmpty()) {
                throw new IllegalArgumentException("Release is Empty");
            }

            if (realProjectKey == null || realProjectKey.isEmpty()) {
                throw new IllegalArgumentException("No project specified");
            }

            List<JiraVersion> sameNamedVersions = filter(
                    hasName(equalTo(realRelease)),
                    site.getVersions(realProjectKey));

            if (sameNamedVersions.size() == 1 && sameNamedVersions.get(0).isReleased()) {
                listener.getLogger().println(Messages.VersionReleaser_AlreadyReleased(realRelease, realProjectKey));
            } else {
                listener.getLogger().println(Messages.VersionReleaser_MarkingReleased(realRelease, realProjectKey));
                site.releaseVersion(realProjectKey, realRelease);
            }
        } catch (Exception e) {
            e.printStackTrace(listener.fatalError(
                    "Unable to release jira version %s/%s: %s",
                    realRelease,
                    realProjectKey,
                    e));
            listener.finished(Result.FAILURE);
            return false;
        }
        return true;
    }

}
