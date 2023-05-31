package tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import eclipsejiraextension.views.SampleView;
import tasks.JIRAPollingTask;
import data.TicketData;

public class JIRAPollingTask implements Runnable {
	private static final int MAX_RESULTS = 20;
	private static final int POLLING_INTERVAL_SECONDS = 10;

	private final SampleView sampleView;
	private final TableViewer viewer;
	private List<String> oldIssues;
	private static int maxResults;
	private static int currentPageIndex;

	// This method starts the scheduled task to poll JIRA for new issues
	public static ScheduledExecutorService startPolling(SampleView sampleView, TableViewer viewer, int maxResults,
			int currentPageIndex) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new JIRAPollingTask(sampleView, viewer, maxResults, currentPageIndex), 0,
				POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
		return executor;
	}

	public JIRAPollingTask(SampleView sampleView, TableViewer viewer, int maxResults, int currentPageIndex) {
		this.sampleView = sampleView;
		this.viewer = viewer;
		JIRAPollingTask.maxResults = maxResults;
		JIRAPollingTask.currentPageIndex = currentPageIndex;
		this.oldIssues = getRecentIssues();
	}

	@Override
	public void run() {
		List<String> newIssues = getRecentIssues();
		for (String issueKey : newIssues) {
			if (!oldIssues.contains(issueKey)) {
				System.out.println("New issue detected: " + issueKey);
				notifyNewIssue(issueKey);
				TicketData newTicket = sampleView.getTicketData(issueKey);

				if (!sampleView.containsTicket(newTicket)) {
					Display.getDefault().asyncExec(() -> {
						viewer.insert(newTicket, 0);

//						if (viewer.getTable().getItemCount() > maxResults) {
//						    viewer.remove(viewer.getElementAt(maxResults));
//						    System.out.println("Number of tickets after removal: " + viewer.getTable().getItemCount());
//						}

					});
				}
			}
		}
		oldIssues = newIssues;
	}

	// This method retrieves recent issues from the JIRA project using the REST API
	private List<String> getRecentIssues() {
		String jiraUrl = "https://internship-primatech.atlassian.net";
		String jiraUsername = "srihislem@gmail.com";
		String jiraPassword = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";
		String jiraProjectKey = "TMDT";

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(
					jiraUrl + "/rest/api/2/search?jql=project=" + jiraProjectKey + "+order+by+created+desc&startAt="
							+ (maxResults * currentPageIndex) + "&maxResults=" + maxResults);
			String encoding = Base64.getEncoder()
					.encodeToString((jiraUsername + ":" + jiraPassword).getBytes(StandardCharsets.UTF_8));
			httpGet.setHeader("Authorization", "Basic " + encoding);

			HttpResponse response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String result = EntityUtils.toString(entity);
				JSONObject jsonResult = new JSONObject(result);
				JSONArray issues = jsonResult.getJSONArray("issues");

				List<String> issueKeys = new ArrayList<>();
				for (int i = 0; i < issues.length(); i++) {
					issueKeys.add(issues.getJSONObject(i).getString("key"));
				}

				return issueKeys;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Collections.emptyList();
	}

	// This method creates a notification that a new issue has been detected
	private void notifyNewIssue(String issue) {
		Display.getDefault().asyncExec(() -> {
			Display display = Display.getDefault();
			Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
			shell.setBackgroundMode(SWT.INHERIT_FORCE);

			Color black = new Color(display, 0, 0, 0);
			shell.setBackground(black);
			shell.setAlpha(200);
			shell.setText("New JIRA Issue");

			GridLayout gridLayout = new GridLayout(2, false);
			shell.setLayout(gridLayout);

			InputStream iconStream = getClass().getResourceAsStream("/icons/notification.png");
			Image image = new Image(display, iconStream);

			Label label = new Label(shell, SWT.NONE);
			label.setImage(image);

			Text text = new Text(shell, SWT.NONE);
			text.setText("A new ticket is detected");
			text.setBounds(10, 10, 200, 20);
			text.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));

			shell.setSize(250, 80);
			Rectangle bounds = display.getPrimaryMonitor().getBounds();
			Rectangle rect = shell.getBounds();
			int x = bounds.x + (bounds.width - rect.width) - 50;
			int y = bounds.y + (bounds.height - rect.height) - 100;
			shell.setLocation(x, y);

			shell.open();
			Display.getDefault().timerExec(5000, () -> {
				if (!shell.isDisposed()) {
					shell.close();
					black.dispose();
					image.dispose();
				}
			});
		});
	}
}
