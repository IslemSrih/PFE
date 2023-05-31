package dialogs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.File;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TableViewer;

import util.GetCurrentJiraUser;
import util.GetJiraAssignees;
import util.GetJiraIssueTypes;
import util.GetJiraPriorities;
import util.GetJiraProjects;
import util.GetJiraSprints;
import data.TicketData;

public class AddTicketDialog extends Dialog {
	private List<String> selectedFields;
	private Table table;
	private TableViewer viewer;

	private TicketData ticketData;
	List<String> priorityNames = GetJiraPriorities.getPriorityNames();
	Map<String, String> assignees = GetJiraAssignees.getAssignableUsers();

	private Map<String, Text> textFields = new HashMap<>();

	public AddTicketDialog(Shell parentShell, List<String> selectedFields) {
		super(parentShell);
		this.selectedFields = selectedFields;
		System.out.println("Selected fields in AddTicketDialog: " + selectedFields);
		this.ticketData = new TicketData();

	}

	// This method is responsible for creating the dialog area
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, false));
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = 600;
		container.setLayoutData(gridData);

		List<String> projectKeys = GetJiraProjects.getProjectKeys();
		List<String> issueTypeNames = GetJiraIssueTypes.getIssueTypeNames();

		for (String field : selectedFields) {
			if ("Commentaire".equals(field) || "Clé".equals(field) || "Résolution".equals(field)) {
				continue;
			}
			Label label = new Label(container, SWT.NONE);
			label.setText(field + ":");

			if ("Projet".equals(field)) {
				Combo projectKeyCombo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				projectKeys.forEach(projectKeyCombo::add);
				projectKeyCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				projectKeyCombo.addModifyListener(e -> ticketData.setValue("Projet", projectKeyCombo.getText()));
			} else if ("Type de ticket".equals(field)) {
				Combo issueTypeCombo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				issueTypeNames.forEach(issueTypeCombo::add);
				issueTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				issueTypeCombo.addModifyListener(e -> ticketData.setValue("Type de ticket", issueTypeCombo.getText()));
			} else if ("Priorité".equals(field)) {
				Combo priorityCombo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				priorityNames.forEach(priorityCombo::add);
				priorityCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				priorityCombo.addModifyListener(e -> ticketData.setValue("Priorité", priorityCombo.getText()));
			} else if ("Responsable".equals(field)) {
				Combo assigneeCombo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				assignees.values().forEach(assigneeCombo::add);
				assigneeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				assigneeCombo.addModifyListener(e -> {
					String selectedName = assigneeCombo.getText();
					String selectedId = assignees.entrySet().stream()
							.filter(entry -> entry.getValue().equals(selectedName)).map(Map.Entry::getKey).findFirst()
							.orElse(null);
					if (selectedId != null) {
						ticketData.setValue("Responsable", selectedId);
					}
				});
			} else if ("Sprint".equals(field)) {
				Combo sprintCombo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				Map<String, String> sprintMap = new HashMap<>();
				GetJiraSprints.getSprints().forEach(sprint -> {
					sprintMap.put(sprint.getName(), Integer.toString(sprint.getId()));
					sprintCombo.add(sprint.getName());
				});
				sprintCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String selectedSprintName = sprintCombo.getText();
						String selectedSprintId = sprintMap.get(selectedSprintName);
						if (selectedSprintId != null) {
							ticketData.setValue("Sprint", selectedSprintId);
						}
					}
				});
			} else if ("Créateur".equals(field)) {
				Text creatorTextField = new Text(container, SWT.BORDER | SWT.READ_ONLY);
				JSONObject currentUser = GetCurrentJiraUser.getCurrentUser();
				String creatorId = currentUser.getString("accountId");
				String creatorDisplayName = currentUser.getString("displayName");
				creatorTextField.setText(creatorDisplayName);
				creatorTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				ticketData.setValue("Créateur", creatorId);
			} else if ("Description".equals(field) || "Résumé".equals(field)) {
				Text textField = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
				GridData gridData1 = new GridData(GridData.FILL_HORIZONTAL);
				gridData1.heightHint = 100;
				textField.setLayoutData(gridData1);
				textField.addModifyListener(e -> ticketData.setValue(field, textField.getText()));
			} else if ("Pièce jointe".equals(field)) {
				Composite attachmentComposite = new Composite(container, SWT.NONE);
				attachmentComposite.setLayout(new GridLayout(2, false));

				Text filePathText = new Text(attachmentComposite, SWT.BORDER | SWT.READ_ONLY);
				filePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				Button browseButton = new Button(attachmentComposite, SWT.PUSH);
				browseButton.setText("Parcourir...");

				GridData browseButtonGridData = new GridData();
				browseButtonGridData.horizontalAlignment = SWT.END;
				browseButtonGridData.verticalAlignment = SWT.CENTER;
				browseButton.setLayoutData(browseButtonGridData);
				attachmentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				browseButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog dialog = new FileDialog(container.getShell(), SWT.NULL);
						String path = dialog.open();
						if (path != null) {
							filePathText.setText(path);
							ticketData.setValue("Pièce jointe", path);
						}
					}
				});
			} else {
				Text textField = new Text(container, SWT.BORDER);
				textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				if ("État".equals(field)) {
					textField.setText("À faire");
					textField.setEditable(false);
					ticketData.setValue(field, "À faire");
				} else {
					textField.addModifyListener(e -> ticketData.setValue(field, textField.getText()));
				}
			}
		}

		return container;
	}

	// This method handles the creation of a ticket in Jira
	private void createTicketInJira(TicketData ticketData) {
		try {
			System.out.println("Starting the creation of a ticket in Jira...");

			HttpClient httpClient = HttpClientBuilder.create().build();
			System.out.println("HttpClient created.");
			HttpPost httpPost = new HttpPost("https://internship-primatech.atlassian.net/rest/api/2/issue");
			System.out.println("HttpPost created.");
			httpPost.setHeader("Content-Type", "application/json");
			System.out.println("Content-Type set to application/json.");
			String encoding = Base64.getEncoder().encodeToString(
					("srihislem@gmail.com:ATATT3xFfGF0aFfubrvbmfrk15PiD90uXEBEbJZe4cD0yDLrrAzyxkBqDWtOBUlA7Cru0JVXHaeA4_7SGK26B-GESPginn_Q7ttKsKtWOfMm_TBONMIgfbWBnJQ8yqNWJnIHnKNcr7pROTxGi_rUe8oL4dsDhTmHSsiL775Rax1cse-2Wn-vFU8=CB1B2BC1")
							.getBytes(StandardCharsets.UTF_8));
			httpPost.setHeader("Authorization", "Basic " + encoding);
			System.out.println("Authorization header set.");

			JSONObject body = new JSONObject();
			JSONObject fields = new JSONObject();
			fields.put("summary", ticketData.getValue("Résumé") == null ? "" : ticketData.getValue("Résumé"));
			fields.put("issuetype", new JSONObject().put("name",
					ticketData.getValue("Type de ticket") == null ? "" : ticketData.getValue("Type de ticket")));
			fields.put("assignee", new JSONObject().put("accountId",
					ticketData.getValue("Responsable") == null ? "" : ticketData.getValue("Responsable")));
			fields.put("project", new JSONObject().put("key",
					ticketData.getValue("Projet") == null ? "" : ticketData.getValue("Projet")));
			fields.put("description",
					ticketData.getValue("Description") == null ? "" : ticketData.getValue("Description"));
			fields.put("priority", new JSONObject().put("name",
					ticketData.getValue("Priorité") == null ? "" : ticketData.getValue("Priorité")));
			fields.put("duedate",
					ticketData.getValue("Date d'échéance") == null ? "" : ticketData.getValue("Date d'échéance"));
			Object sprintValue = ticketData.getValue("Sprint");
			int sprintId = 0;
			if (sprintValue != null) {
				try {
					sprintId = Integer.parseInt((String) sprintValue);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			fields.put("customfield_10020", sprintId);

			body.put("fields", fields);
			System.out.println("Request body created: " + body.toString());
			System.out.println("JSON Request Body: " + body.toString(2));
			httpPost.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));
			System.out.println("Entity set for the httpPost.");
			HttpResponse response = httpClient.execute(httpPost);

			if (response.getStatusLine().getStatusCode() != 201) {
				System.out.println(
						"Failed to create ticket. HTTP error code : " + response.getStatusLine().getStatusCode());
				throw new RuntimeException(
						"Failed to create ticket. HTTP error code : " + response.getStatusLine().getStatusCode());
			}
			System.out.println("Ticket successfully created in Jira!");
			JSONObject responseJson = new JSONObject(EntityUtils.toString(response.getEntity()));
			String issueKey = responseJson.getString("key");
			System.out.println("Issue key: " + issueKey);
			ticketData.setValue("Clé", issueKey); 

			String filePath = (String) ticketData.getValue("Pièce jointe");
			if (filePath != null && !filePath.isEmpty()) {
				attachFileToTicket(issueKey, filePath);
			}
		} catch (Exception e) {
			System.out.println("An error occurred during the creation of the ticket in Jira:");
			e.printStackTrace();
		}

	}

	// This method handles the attachment of a file to a Jira ticket
	private void attachFileToTicket(String issueKey, String filePath) {
		try {
			System.out.println("File to attach: " + filePath);

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost httpPostAttach = new HttpPost(
					"https://internship-primatech.atlassian.net/rest/api/2/issue/" + issueKey + "/attachments");
			httpPostAttach.setHeader("X-Atlassian-Token", "no-check");

			String encoding = Base64.getEncoder().encodeToString(
					("srihislem@gmail.com:ATATT3xFfGF0aFfubrvbmfrk15PiD90uXEBEbJZe4cD0yDLrrAzyxkBqDWtOBUlA7Cru0JVXHaeA4_7SGK26B-GESPginn_Q7ttKsKtWOfMm_TBONMIgfbWBnJQ8yqNWJnIHnKNcr7pROTxGi_rUe8oL4dsDhTmHSsiL775Rax1cse-2Wn-vFU8=CB1B2BC1")
							.getBytes(StandardCharsets.UTF_8));
			httpPostAttach.setHeader("Authorization", "Basic " + encoding);

			File fileToUpload = new File(filePath);
			if (!fileToUpload.exists()) {
				System.out.println("File does not exist: " + filePath);
				return;
			}
			FileBody fileBody = new FileBody(fileToUpload);
			HttpEntity entity = MultipartEntityBuilder.create().addPart("file", fileBody).build();
			httpPostAttach.setEntity(entity);
			System.out.println("HTTP request for file attachment prepared.");

			HttpResponse responseAttach = httpClient.execute(httpPostAttach);
			System.out.println("Attach file response: " + EntityUtils.toString(responseAttach.getEntity()));
			if (responseAttach.getStatusLine().getStatusCode() != 200) {
				System.out.println(
						"Failed to attach file. HTTP error code : " + responseAttach.getStatusLine().getStatusCode());
				throw new RuntimeException(
						"Failed to attach file. HTTP error code : " + responseAttach.getStatusLine().getStatusCode());
			}
			System.out.println("File successfully attached to the ticket!");
		} catch (Exception e) {
			System.out.println("An error occurred during the attachment of the file:");
			e.printStackTrace();
		}
	}

	@Override
	protected void okPressed() {
		createTicketInJira(ticketData);
		viewer.refresh();
		super.okPressed();
	}

	public void setViewer(TableViewer viewer) {
		this.viewer = viewer;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public TicketData getTicketData() {
		return ticketData;

	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Ajouter un ticket");
	}

}
