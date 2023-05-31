package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetJiraSprints {

	private static final String JIRA_URL = "https://internship-primatech.atlassian.net";
	private static final String USERNAME = "srihislem@gmail.com";
	private static final String API_KEY = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";
	private static final String BOARD_ID = "1";

	public static List<Sprint> getSprints() {
		List<Sprint> sprints = new ArrayList<>();
		try {
			URL url = new URL(JIRA_URL + "/rest/agile/1.0/board/" + BOARD_ID + "/sprint");
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
			StringBuilder responseBuilder = new StringBuilder();
			while ((output = br.readLine()) != null) {
				responseBuilder.append(output);
			}
			conn.disconnect();

			JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
			JSONArray sprintsJson = jsonResponse.getJSONArray("values");

			for (int i = 0; i < sprintsJson.length(); i++) {
				JSONObject sprintJson = sprintsJson.getJSONObject(i);
				int sprintId = sprintJson.getInt("id");
				String sprintName = sprintJson.getString("name");
				sprints.add(new Sprint(sprintId, sprintName));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return sprints;
	}

	public static void main(String[] args) {
		List<Sprint> sprints = getSprints();
		for (Sprint sprint : sprints) {
			System.out.println("Sprint ID: " + sprint.getId() + ", Sprint Name: " + sprint.getName());
		}
	}

	public static class Sprint {
		private int id;
		private String name;

		public Sprint(int sprintId, String name) {
			this.id = sprintId;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
