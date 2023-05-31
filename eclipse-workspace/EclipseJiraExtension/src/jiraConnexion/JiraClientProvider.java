package jiraConnexion;

import java.net.URI;
import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

public class JiraClientProvider {
	public JiraRestClient createJiraRestClient() {
		try {
			JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
			URI jiraServerUri = new URI("https://internship-primatech.atlassian.net");
			return factory.createWithBasicHttpAuthentication(jiraServerUri, "srihislem@gmail.com",
					"ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.out.println("Error while connecting to Jira: " + e.getMessage());
			return null;
		}
	}
}
