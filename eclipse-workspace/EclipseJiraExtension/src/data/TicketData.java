package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TicketData {
	private Map<String, Object> data;
	private String issueKey;
	private String assignee; 

	public TicketData(Map<String, Object> initialData) {
		this.data = new HashMap<>(initialData);
	}

	public TicketData() {
		this.data = new HashMap<>();
	}

	public TicketData(String issueKey, String assignee) {
		this.data = new HashMap<>();
		this.issueKey = issueKey;
		this.assignee = assignee;
		data.put("Clé", issueKey);
		data.put("Assignee", assignee);
	}

	public String getIssueKey() {
		return issueKey;
	}

	public String getAssignee() {
		return assignee;
	}

	public Object getValue(String field) {
		return data.get(field);
	}

	public Set<String> getFields() {
		return data.keySet();
	}

	public void setValue(String field, Object value) {
		data.put(field, value);
		if ("Clé".equals(field)) {
			this.issueKey = (String) value;
		} else if ("Assignee".equals(field)) {
			this.assignee = (String) value;
		}
	}

	public void removeValue(String key) {
		data.remove(key);
	}
}
