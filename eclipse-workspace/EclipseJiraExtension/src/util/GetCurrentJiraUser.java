package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.json.JSONObject;

public class GetCurrentJiraUser {
	private static final String JIRA_URL = "https://internship-primatech.atlassian.net";
	private static final String USERNAME = "srihislem@gmail.com";
	private static final String API_KEY = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";

	public static JSONObject getCurrentUser() {
		JSONObject user = new JSONObject();

		try {
			URL url = new URL(JIRA_URL + "/rest/api/2/myself");
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

			JSONObject jsonObject = new JSONObject(jsonOutput.toString());
			String accountId = jsonObject.getString("accountId");
			String displayName = jsonObject.getString("displayName");
			user.put("accountId", accountId);
			user.put("displayName", displayName);
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return user;
	}
}