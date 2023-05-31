package tasks;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jface.viewers.TableViewer;
import org.json.JSONArray;
import org.json.JSONObject;

import eclipsejiraextension.views.SampleView;
import tasks.JIRAUpdatePollingTask;

public class JIRAUpdatePollingTask extends TimerTask {
	private Map<String, Map<String, String>> oldIssueFields = new HashMap<>();
	private final SampleView sampleView;
	private final TableViewer viewer;
	private final int maxResults;
	private final int currentPageIndex;

	public JIRAUpdatePollingTask(SampleView sampleView, TableViewer viewer, int maxResults, int currentPageIndex) {
		this.sampleView = sampleView;
		this.viewer = viewer;
		this.maxResults = maxResults;
		this.currentPageIndex = currentPageIndex;
		this.oldIssueFields = getIssueFields();
	}

	public static ScheduledExecutorService startPolling(SampleView sampleView, TableViewer viewer, int maxResults,
			int currentPageIndex) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new JIRAUpdatePollingTask(sampleView, viewer, maxResults, currentPageIndex), 0, 10,
				TimeUnit.SECONDS);
		return executor;
	}

	public void run() {
		Map<String, Map<String, String>> newIssueFields = getIssueFields();
		for (Map.Entry<String, Map<String, String>> entry : newIssueFields.entrySet()) {
			String issueKey = entry.getKey();
			Map<String, String> newFields = entry.getValue();

			if (!oldIssueFields.containsKey(issueKey)) {
				// notifyUpdatedIssue(issueKey, "New Issue");
				continue;
			}

			Map<String, String> oldFields = oldIssueFields.get(issueKey);
			for (Map.Entry<String, String> field : newFields.entrySet()) {
				String fieldName = field.getKey();
				String newValue = field.getValue();
				String oldValue = oldFields.get(fieldName);

				if (!Objects.equals(oldValue, newValue)) {
					System.out.println("Issue " + issueKey + " has been updated. Field " + fieldName + " changed from "
							+ oldValue + " to " + newValue);
					// notifyUpdatedIssue(issueKey, fieldName);
					switch (fieldName) {
					case "status":
						sampleView.updateIssueStatusInTable(issueKey, newValue);
						break;
					case "description":
						sampleView.updateIssueDescriptionInTable(issueKey, newValue);
						break;
					case "summary":
						sampleView.updateIssueSummaryInTable(issueKey, newValue);
						break;
					case "dueDate":
						sampleView.updateIssueDueDateInTable(issueKey, newValue);
						break;
					case "priority":
						sampleView.updateIssuePriorityInTable(issueKey, newValue);
						break;
					case "type":
						sampleView.updateIssueTypeInTable(issueKey, newValue);
						break;
					case "sprint":
						sampleView.updateIssueSprintInTable(issueKey, newValue);
						break;
					case "assignee":
						sampleView.updateIssueAssigneeInTable(issueKey, newValue);
						break;
					case "resolution":
						sampleView.updateIssueResolutionInTable(issueKey, newValue);
						break;

					}
				}
			}
		}

		oldIssueFields = newIssueFields;
	}

	// This method fetches and returns the issue fields from the JIRA REST API
	private Map<String, Map<String, String>> getIssueFields() {
		int currentPageIndex = sampleView.getCurrentPageIndex();
		int startAt = currentPageIndex * maxResults;
		String jiraUrl = "https://internship-primatech.atlassian.net";
		String jiraUsername = "srihislem@gmail.com";
		String jiraPassword = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";
		String jiraProjectKey = "TMDT";

		Map<String, Map<String, String>> issueFields = new HashMap<>();

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(jiraUrl + "/rest/api/2/search?jql=project=" + jiraProjectKey
					+ "+order+by+updated+desc&startAt=" + startAt + "&maxResults=" + maxResults);
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
					JSONObject issue = issues.getJSONObject(i);
					String issueKey = issue.getString("key");
					JSONObject fields = issue.getJSONObject("fields");
					String status = fields.getJSONObject("status").getString("name");
					String description = fields.optString("description");
					String summary = fields.optString("summary");
					String dueDate = fields.optString("duedate");
					String priority = fields.getJSONObject("priority").optString("name");
					String type = fields.getJSONObject("issuetype").optString("name");
					JSONObject assigneeObject = fields.optJSONObject("assignee");
					String assignee = (assigneeObject != null) ? assigneeObject.optString("displayName") : null;
					String sprint = null;
					JSONArray sprintArray = fields.optJSONArray("customfield_10020");
					if (sprintArray != null && sprintArray.length() > 0) {
						JSONObject sprintObject = sprintArray.optJSONObject(0);
						if (sprintObject != null) {
							sprint = sprintObject.optString("name");
						}
					}
					String resolution = "";
					if (fields.has("resolution") && !fields.isNull("resolution")) {
						resolution = fields.getJSONObject("resolution").optString("name");
					}

					Map<String, String> fieldsMap = new HashMap<>();
					fieldsMap.put("status", status);
					fieldsMap.put("description", description);
					fieldsMap.put("summary", summary);
					fieldsMap.put("dueDate", dueDate);
					fieldsMap.put("priority", priority);
					fieldsMap.put("type", type);
					fieldsMap.put("assignee", assignee);
					fieldsMap.put("sprint", sprint);
					fieldsMap.put("resolution", resolution);

					issueFields.put(issueKey, fieldsMap);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return issueFields;
	}
//	private void notifyUpdatedIssue(String issueKey, String fieldName) {
//		Display.getDefault().syncExec(() -> {
//			Display display = Display.getDefault();
//			Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
//			shell.setBackgroundMode(SWT.INHERIT_FORCE);
//
//			Color black = new Color(display, 0, 0, 0);
//			shell.setBackground(black);
//			shell.setAlpha(200);
//			shell.setText("Updated JIRA Issue");
//
//			GridLayout gridLayout = new GridLayout(2, false);
//			shell.setLayout(gridLayout);
//
//			Label label = new Label(shell, SWT.NONE);
//
//			Text text = new Text(shell, SWT.NONE);
//			text.setText("Issue " + issueKey + " has been modified. Field " + fieldName);
//			text.setBounds(10, 10, 200, 20);
//			text.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
//
//			shell.setSize(350, 80);
//			Rectangle bounds = display.getPrimaryMonitor().getBounds();
//			Rectangle rect = shell.getBounds();
//			int x = bounds.x + (bounds.width - rect.width) - 50;
//			int y = bounds.y + (bounds.height - rect.height) - 100;
//			shell.setLocation(x, y);
//
//			shell.open();
//			Timer closeTimer = new Timer();
//			closeTimer.schedule(new TimerTask() {
//				@Override
//				public void run() {
//					display.asyncExec(() -> {
//						if (!shell.isDisposed()) {
//							shell.close();
//							black.dispose();
//						}
//					});
//				}
//			}, 5000);
//		});
//	}
//
}