package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetJiraAssignees {

	private static final String JIRA_URL = "https://internship-primatech.atlassian.net";
	private static final String USERNAME = "srihislem@gmail.com";
	private static final String API_KEY = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";

	public static void main(String[] args) {
		Map<String, String> assignees = getAssignableUsers("TMDT");
		System.out.println("Assignee list: ");
		for (Map.Entry<String, String> entry : assignees.entrySet()) {
			String accountId = entry.getKey();
			String displayName = entry.getValue();
			System.out.println("ID: " + accountId + " - Name: " + displayName);
		}
	}

	public static Map<String, String> getAssignableUsers(String projectKey) {
		Map<String, String> users = new HashMap<>();

		try {
			URL url = new URL(JIRA_URL + "/rest/api/2/user/assignable/search?project=" + projectKey);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization",
					"Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + API_KEY).getBytes()));

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;
			StringBuilder jsonOutput = new StringBuilder();
			while ((output = br.readLine()) != null) {
				jsonOutput.append(output);
			}

			JSONArray jsonArray = new JSONArray(jsonOutput.toString());

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject user = jsonArray.getJSONObject(i);
				String accountId = user.getString("accountId");
				String displayName = user.getString("displayName");
				users.put(accountId, displayName);
			}
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return users;
	}

	public static Map<String, String> getAssignableUsers() {
		return getAssignableUsers("TMDT");
	}

}
