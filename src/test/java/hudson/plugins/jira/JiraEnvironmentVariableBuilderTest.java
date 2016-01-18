package hudson.plugins.jira;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import sun.awt.shell.ShellFolder;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class JiraEnvironmentVariableBuilderTest {

    private static final String JIRA_URL = "http://example.com";
    private static final String JIRA_URL_PROPERTY_NAME = "JIRA_URL";
    private static final String ISSUES_PROPERTY_NAME = "JIRA_ISSUES";
    private static final String ISSUE_ID_1 = "ISS-1";
    private static final String ISSUE_ID_2 = "ISS-2";

    // Ordering of set created from collection intializer seems to depend on which JDK is used
    // but isn't important for this purpose
    private static final String EXPECTED_JIRA_ISSUES_1 = ISSUE_ID_1 + "," + ISSUE_ID_2;
    private static final String EXPECTED_JIRA_ISSUES_2 = ISSUE_ID_2 + "," + ISSUE_ID_1;

    Run run;
    Launcher launcher;
    TaskListener listener;
    EnvVars env;
    Job job;
    JiraSite site;
    UpdaterIssueSelector issueSelector;
    PrintStream logger;
    Node node;
    FilePath filePath;
    File file;

    @Before
    public void createMocks() throws IOException, InterruptedException {
        run = mock(Run.class);
        launcher = mock(Launcher.class);
        listener = mock(TaskListener.class);
        env = mock(EnvVars.class);
        job = mock(Job.class);
        site = mock(JiraSite.class);
        issueSelector = mock(UpdaterIssueSelector.class);
        logger = mock(PrintStream.class);
        file = File.createTempFile("pfx", "sfx");
        filePath = new FilePath(file);

        when(site.getName()).thenReturn(JIRA_URL);

        when(listener.getLogger()).thenReturn(logger);

        when(issueSelector.findIssueIds(run, site, listener))
                .thenReturn(new HashSet<String>(Arrays.asList(ISSUE_ID_1, ISSUE_ID_2)));

        when(run.getParent()).thenReturn(job);
        when(run.getEnvironment(listener)).thenReturn(env);
    }

    @After
    public void CleanUp() {
        if (file != null)
            file.delete();
    }

    @Test
    public void testIssueSelectorDefaultsToDefault() {
        final JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(null);
        assertThat(builder.getIssueSelector(), instanceOf(DefaultUpdaterIssueSelector.class));
    }

    @Test
    public void testSetIssueSelectorPersists() {
        final JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(issueSelector);
        assertThat(builder.getIssueSelector(), is(issueSelector));
    }
    
    @Test(expected = AbortException.class)
    public void testPerformWithNoSiteFailsBuild() throws InterruptedException, IOException {
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(null).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        builder.perform(run, filePath, launcher, listener);
    }
    
    @Test
    public void testPerformAddsAction() throws InterruptedException, IOException {
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(site).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        
        builder.perform(run, filePath, launcher, listener);

        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(run).addAction(captor.capture());
        
        assertThat(captor.getValue(),instanceOf(JiraEnvironmentContributingAction.class));
        
        JiraEnvironmentContributingAction action = (JiraEnvironmentContributingAction)(captor.getValue());
        
        assertThat(action.getJiraUrl(), is(JIRA_URL));
        assertThat(action.getIssuesList(), anyOf(is(EXPECTED_JIRA_ISSUES_1), is(EXPECTED_JIRA_ISSUES_2)));
    }
    
}
