package dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
import org.eclipse.swt.widgets.Text;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import util.GetJiraIssueTypes;
import util.GetJiraPriorities;
import util.GetJiraProjects;
import util.GetJiraSprints;
import util.GetJiraStatusesCategory;
import data.TicketData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditTicketDialog extends Dialog {
	private TicketData ticketData;
	private Map<String, Control> fieldInputs = new HashMap<>();

	public EditTicketDialog(Shell parentShell, TicketData ticketData) {
		super(parentShell);
		this.ticketData = ticketData;
	}

	// this method is responsible for dynamically creating controls based on the
	// ticket fields
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = 600;
		container.setLayoutData(gridData);

		List<String> statusNames = GetJiraStatusesCategory.getStatusCategoryNames();

		for (String field : ticketData.getFields()) {
			if ("Résolution".equals(field) || "Commentaire".equals(field) || "Clé".equals(field)) {
				continue;
			}
			Label label = new Label(container, SWT.NONE);
			label.setText(field);

			if ("État".equals(field)) {
				Combo combo = new Combo(container, SWT.READ_ONLY);
				combo.setItems(statusNames.toArray(new String[statusNames.size()]));
				combo.setText(ticketData.getValue(field).toString());

				String currentStatus = ticketData.getValue(field).toString();
				int index = statusNames.indexOf(currentStatus);
				if (index >= 0) {
					combo.select(index);
				}

				combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fieldInputs.put(field, combo);
			} else if ("Sprint".equals(field)) {
				Combo sprintCombo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				Map<String, String> sprintMap = new HashMap<>();
				List<GetJiraSprints.Sprint> sprints = GetJiraSprints.getSprints();
				for (GetJiraSprints.Sprint sprint : sprints) {
					sprintMap.put(sprint.getName(), Integer.toString(sprint.getId()));
					sprintCombo.add(sprint.getName());
				}

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

			}

			else if ("Type de ticket".equals(field)) {
				Combo combo = new Combo(container, SWT.READ_ONLY);
				List<String> issueTypeNames = GetJiraIssueTypes.getIssueTypeNames();
				combo.setItems(issueTypeNames.toArray(new String[issueTypeNames.size()]));
				combo.setText(ticketData.getValue(field).toString());

				String currentIssueType = ticketData.getValue(field).toString();
				int index = issueTypeNames.indexOf(currentIssueType);
				if (index >= 0) {
					combo.select(index);
				}

				combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fieldInputs.put(field, combo);
			} else if ("Projet".equals(field)) {
				Combo combo = new Combo(container, SWT.READ_ONLY);
				List<String> projectKeys = GetJiraProjects.getProjectKeys();
				combo.setItems(projectKeys.toArray(new String[projectKeys.size()]));
				combo.setText(ticketData.getValue(field).toString());

				String currentProject = ticketData.getValue(field).toString();
				int index = projectKeys.indexOf(currentProject);
				if (index >= 0) {
					combo.select(index);
				}

				combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fieldInputs.put(field, combo);
			} else if ("Pièce jointe".equals(field)) {
				Composite fieldComposite = new Composite(container, SWT.NONE);
				fieldComposite.setLayout(new GridLayout(2, false)); 
				GridData gridDataForFieldComposite = new GridData(GridData.FILL_HORIZONTAL);
				gridDataForFieldComposite.horizontalSpan = 2; 
				fieldComposite.setLayoutData(gridDataForFieldComposite);

				Label label1 = new Label(fieldComposite, SWT.NONE);
				label1.setText(field);

				Text textField = new Text(fieldComposite, SWT.BORDER);
				textField.setText(ticketData.getValue(field).toString());
				textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fieldInputs.put(field, textField);

				Composite buttonComposite = new Composite(container, SWT.NONE);
				buttonComposite.setLayout(new GridLayout(2, false)); 
				GridData gridDataForButtonComposite = new GridData(GridData.FILL_HORIZONTAL);
				gridDataForButtonComposite.horizontalSpan = 2; 
				buttonComposite.setLayoutData(gridDataForButtonComposite);

				Button attachButton = new Button(buttonComposite, SWT.PUSH);
				attachButton.setText("Add Attachment");
				attachButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
				attachButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fileDialog = new FileDialog(attachButton.getShell(), SWT.OPEN);
						fileDialog.setText("Select an image");
						fileDialog.setFilterExtensions(new String[] { "*.jpg", "*.png", "*.gif" });
						String selectedImage = fileDialog.open();
						if (selectedImage != null) {
							textField.setText(selectedImage);
							String issueId = ticketData.getValue("Clé").toString(); 
																					
							uploadFileToJira(selectedImage, issueId); 
						}
					}
				});

			}

			else if ("Priorité".equals(field)) {
				Combo combo = new Combo(container, SWT.READ_ONLY);
				List<String> priorityNames = GetJiraPriorities.getPriorityNames();
				combo.setItems(priorityNames.toArray(new String[priorityNames.size()]));
				combo.setText(ticketData.getValue(field).toString());

				String currentPriority = ticketData.getValue(field).toString();
				int index = priorityNames.indexOf(currentPriority);
				if (index >= 0) {
					combo.select(index);
				}

				combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fieldInputs.put(field, combo);
			} else {
				if ("Description".equals(field) || "Résumé".equals(field)) {
					int textFieldStyle = SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL;
					Text textField = new Text(container, textFieldStyle);
					textField.setText(ticketData.getValue(field).toString());
					GridData gridDataForTextArea = new GridData(GridData.FILL_HORIZONTAL);
					gridDataForTextArea.heightHint = 100; 
					textField.setLayoutData(gridDataForTextArea);
					fieldInputs.put(field, textField);
				} else {
					int textFieldStyle = SWT.BORDER;
					boolean isReadOnly = "Clé".equals(field) || "Créateur".equals(field) || "Responsable".equals(field);
					if (isReadOnly) {
						textFieldStyle |= SWT.READ_ONLY;
					}
					Text textField = new Text(container, textFieldStyle);
					textField.setText(ticketData.getValue(field).toString());
					textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

					if (isReadOnly) {
						textField.addFocusListener(new FocusAdapter() {
							@Override
							public void focusGained(FocusEvent e) {
								textField.clearSelection();
								textField.setSelection(textField.getCharCount());
							}
						});
					}

					fieldInputs.put(field, textField);
				}
			}
		}

		return container;
	}

	// this method handles the file upload functionality
	private void uploadFileToJira(String filePath, String issueId) {
		File file = new File(filePath);

		try {
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost postRequest = new HttpPost(
					"https://internship-primatech.atlassian.net/rest/api/3/issue/" + issueId + "/attachments");

			String username = "srihislem@gmail.com";
			String password = "ATATT3xFfGF02hdOHJfMs4yNUK30uewslRizjtsWLmgsX6gh5Dy_jd5TmkqiF1uU3yjZcmDxZUVyUENORbtZDgGpMvOFl3s8xXBC89dgavs76RpQCBtKIUwTPnD_bZtBR-ZucENaj6OBrhgRdIB1SvKjlpfvzIclQdMbXe28Xh3TUF1Y8msOa6k=AC9CC24F";
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
			String authHeader = "Basic " + new String(encodedAuth);
			postRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addBinaryBody("file", new FileInputStream(file), ContentType.APPLICATION_OCTET_STREAM,
					file.getName());
			HttpEntity multipart = builder.build();
			postRequest.setEntity(multipart);
			postRequest.setHeader("X-Atlassian-Token", "no-check");
			HttpResponse response = httpClient.execute(postRequest);

			// Check response status
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				System.out.println("Pièce jointe mise à jour avec succès dans Jira pour le ticket : " + issueId);
			} else {
				System.out.println("Echec de la mise à jour de la pièce jointe dans Jira pour le ticket : " + issueId
						+ ". Statut HTTP : " + statusCode);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			System.out.println("Réponse du serveur Jira : " + responseBody);
		} catch (Exception e) {
			System.out.println("Erreur lors de la mise à jour de la pièce jointe pour le ticket : " + issueId);
			e.printStackTrace();
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Edit Ticket");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			for (String field : fieldInputs.keySet()) {
				Control control = fieldInputs.get(field);
				if (control instanceof Text) {
					String newValue = ((Text) control).getText();
					ticketData.setValue(field, newValue);
				} else if (control instanceof Combo) {
					String newValue = ((Combo) control).getText();
					ticketData.setValue(field, newValue);
				}
			}

		}
		super.buttonPressed(buttonId);
	}

	public TicketData getUpdatedTicket() {
		return ticketData;
	}

}