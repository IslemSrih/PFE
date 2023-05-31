package tasks;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import eclipsejiraextension.views.SampleView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JIRADeletionDetectionTask implements Runnable {
	private static final int POLLING_INTERVAL_SECONDS = 10;
	private final Set<String> trackedIssues = new HashSet<>();
	private final SampleView sampleView;
	private final int maxResults;
	private final int currentPageIndex;
	private final TableViewer viewer;

	public JIRADeletionDetectionTask(SampleView sampleView, TableViewer viewer, int maxResults, int currentPageIndex) {
		this.sampleView = sampleView;
		this.viewer = viewer;
		this.maxResults = maxResults;
		this.currentPageIndex = currentPageIndex;
	}

	// This method schedules a new task that polls JIRA for changes
	public static ScheduledExecutorService startPolling(SampleView sampleView, TableViewer viewer, int maxResults,
			int currentPageIndex) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new JIRADeletionDetectionTask(sampleView, viewer, maxResults, currentPageIndex), 0,
				POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
		return executor;
	}

	@Override
	public void run() {
		Set<String> currentIssues = getIssues();
		for (String issueKey : trackedIssues) {
			if (!currentIssues.contains(issueKey)) {
				System.out.println("Issue deleted: " + issueKey);

				// Remove the deleted ticket from the TableViewer
				Display.getDefault().asyncExec(() -> {
					sampleView.removeTicket(issueKey);
				});
			}
		}
		trackedIssues.clear();
		trackedIssues.addAll(currentIssues);
	}

	// This method retrieves the current set of issue keys from JIRA
	private Set<String> getIssues() {
		int currentPageIndex = sampleView.getCurrentPageIndex();
		int startAt = currentPageIndex * maxResults;
		String jiraUrl = "https://internship-primatech.atlassian.net";
		String jiraUsername = "srihislem@gmail.com";
		String jiraPassword = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";
		String jiraProjectKey = "TMDT";

		Set<String> issueKeys = new HashSet<>();
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(jiraUrl + "/rest/api/2/search?jql=project=" + jiraProjectKey
					+ "+order+by+created+desc&startAt=" + startAt + "&maxResults=" + maxResults);
			String encoding = Base64.getEncoder()
					.encodeToString((jiraUsername + ":" + jiraPassword).getBytes(StandardCharsets.UTF_8));
			httpGet.setHeader("Authorization", "Basic " + encoding);

			HttpResponse response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String result = EntityUtils.toString(entity);
				JSONObject jsonResult = new JSONObject(result);
				JSONArray issues = jsonResult.getJSONArray("issues");

				for (int i = 0; i < issues.length(); i++) {
					issueKeys.add(issues.getJSONObject(i).getString("key"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return issueKeys;
	}
}
