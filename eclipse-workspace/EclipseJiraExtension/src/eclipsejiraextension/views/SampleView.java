package eclipsejiraextension.views;

import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;

import org.joda.time.DateTime;

import org.eclipse.jface.window.Window;

import org.json.JSONObject;

import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.Watchers;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.api.domain.Resolution;

import eclipsejiraextension.Activator;
import eclipsejiraextension.views.SampleView;
import io.atlassian.util.concurrent.Promise;
import jiraConnexion.JiraClientProvider;
import tasks.JIRADeletionDetectionTask;
import tasks.JIRAPollingTask;
import tasks.JIRAUpdatePollingTask;

import dialogs.AddTicketDialog;
import dialogs.EditTicketDialog;

import util.GetCurrentJiraUser;
import util.GetJiraAssignees;
import util.GetJiraIssueTypes;
import util.GetJiraPriorities;
import util.GetJiraProjects;
import util.GetJiraSprints;
import util.GetJiraSprints.Sprint;
import data.TicketData;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import java.util.stream.Collectors;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class SampleView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "connexion.views.SampleView";

	@Inject
	IWorkbench workbench;
	private List<TableEditor> tableEditors = new ArrayList<>();
	private TableViewer viewer;
	private JiraRestClient restClient;
	private List<String> selectedFields = new ArrayList<>();
	private List<TicketData> ticketsData = new ArrayList<>();
	private int MAX_RESULTS = 20;
	private int currentPageIndex = 0;
	private Map<TicketData, List<Button>> ticketButtons = new HashMap<>();
	private int totalIssues;
	private Composite searchComposite;
	private String lastJqlQuery = "order by created DESC";
	private ScheduledExecutorService jiraPollingExecutor;
	private ScheduledExecutorService jiraDeletionDetectionExecutor;
	private String lastSearchJqlQuery = null;
	public static final Color COLUMN_HEADER_COLOR = new Color(Display.getDefault(), 222, 255, 254);
	public static final Color CELL_COLOR = new Color(Display.getDefault(), 247, 225, 212);
	private Button prevButton;
	private Button nextButton;
	private List<String> checkedElements = new ArrayList<>();
	List<String> searchColumns = Arrays.asList("Clé", "Type de ticket", "Projet", "Sprint", "Priorité", "Responsable");
	private static final Map<String, String> COLUMN_TO_JIRA_FIELD_MAP = new HashMap<String, String>() {
		{
			put("Clé", "key");
			put("Type de ticket", "issuetype");
			put("Projet", "project");
			put("Sprint", "sprint");
			put("Priorité", "priority");
			put("Responsable", "assignee");
			put("État", "status");
			put("Créateur", "creator");
			put("Date d'échéance", "duedate");
			put("Pièce jointe", "attachment");
			put("Résumé", "summary");
			put("Commentaire", "comment");
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		selectedFields = Arrays.asList("Clé", "Type de ticket", "Projet", "Sprint", "Priorité", "Responsable");
		createGetFieldsButton(parent);
		createViewer(parent);
		createSearchBoxes(parent, selectedFields);
		workbench.getHelpSystem().setHelp(viewer.getControl(), "Connexion.viewer");
		getSite().setSelectionProvider(viewer);
		hookContextMenu();
		createAddButton(parent);
		ticketsData = new ArrayList<>();
		loadJiraData("order by created DESC");
		jiraPollingExecutor = JIRAPollingTask.startPolling(this, viewer, MAX_RESULTS, currentPageIndex);
		jiraPollingExecutor = JIRAUpdatePollingTask.startPolling(this, viewer, MAX_RESULTS, currentPageIndex);
		JIRADeletionDetectionTask.startPolling(this, viewer, MAX_RESULTS, currentPageIndex);
		createNavigationButtons(parent);
		createComboBox(parent);
	}

	private void createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns(parent, viewer, selectedFields);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		viewer.setContentProvider(new ArrayContentProvider());
		getSite().setSelectionProvider(viewer);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);
		viewer.getTable().addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Point pt = new Point(event.x, event.y);
				TableItem item = viewer.getTable().getItem(pt);
				if (item == null)
					return;
				for (int col = 0; col < viewer.getTable().getColumnCount(); col++) {
					if (item.getBounds(col).contains(pt)) {
						String header = viewer.getTable().getColumn(col).getText();
						if ("Clé".equals(header)) {
							String ticketKey = item.getText(col);
							try {
								java.awt.Desktop.getDesktop().browse(
										new URI("https://internship-primatech.atlassian.net/browse/" + ticketKey));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
	}

	// retrieve Jira fields from Jira server
	private List<String> getJiraFields() {
		List<String> jiraFields = new ArrayList<>();
		List<String> fieldsToRetrieve = Arrays.asList("Clé", "Type de ticket", "Projet", "Sprint", "Priorité",
				"Responsable", "État", "Créateur", "Description", "Résumé", "Date d'échéance", "Pièce jointe",
				"Commentaire", "Résolution");
		try {
			JiraClientProvider jiraClientProvider = new JiraClientProvider();
			JiraRestClient jiraRestClient = jiraClientProvider.createJiraRestClient();
			Iterable<Field> fields = jiraRestClient.getMetadataClient().getFields().claim();
			Map<String, Field> fieldMap = new HashMap<>();
			for (Field field : fields) {
				fieldMap.put(field.getName(), field);
			}
			for (String fieldName : fieldsToRetrieve) {
				Field field = fieldMap.get(fieldName);
				if (field != null) {
					System.out.println("Field '" + field.getName() + "' exists and will be added to the window.");
					jiraFields.add(field.getName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error while connecting to Jira: " + e.getMessage());
		}
		return jiraFields;
	}

	// dynamic fields selection
	private void createGetFieldsButton(final Composite parent) {
		Button getFieldsButton = new Button(parent, SWT.PUSH);
		getFieldsButton.setText("Add Fields");
		getFieldsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell shell = new Shell(parent.getShell());
				shell.setText("Select Fields");
				shell.setSize(400, 400);

				CheckboxTableViewer checkboxTableViewer = CheckboxTableViewer.newCheckList(shell,
						SWT.BORDER | SWT.V_SCROLL);
				checkboxTableViewer.getTable().setHeaderVisible(true);
				checkboxTableViewer.getTable().setLinesVisible(true);
				checkboxTableViewer.setContentProvider(new ArrayContentProvider());
				checkboxTableViewer.setInput(getJiraFields());

				for (String checkedElement : checkedElements) {
					checkboxTableViewer.setChecked(checkedElement, true);
				}
				checkboxTableViewer.setChecked("Clé", true);
				checkboxTableViewer.setChecked("Type de ticket", true);
				checkboxTableViewer.setChecked("Projet", true);
				checkboxTableViewer.setChecked("Sprint", true);
				checkboxTableViewer.setChecked("Priorité", true);
				checkboxTableViewer.setChecked("Responsable", true);

				checkboxTableViewer.addCheckStateListener(new ICheckStateListener() {
					@Override
					public void checkStateChanged(CheckStateChangedEvent event) {
						if (event.getElement().toString().equals("Clé")
								&& event.getElement().toString().equals("Type de ticket")
								&& event.getElement().toString().equals("Projet")
								&& event.getElement().toString().equals("Sprint")
								&& event.getElement().toString().equals("Priorité")
								&& event.getElement().toString().equals("Responsable") && !event.getChecked()) {
							checkboxTableViewer.setChecked(event.getElement(), true);
						}
					}
				});

				checkboxTableViewer.getTable().setBounds(0, 0, 375, 300);

				Button selectAllButton = new Button(shell, SWT.PUSH);
				selectAllButton.setText("Select All");
				selectAllButton.setBounds(50, 320, 100, 30);
				selectAllButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						checkboxTableViewer.setAllChecked(true);
					}
				});

				Button okButton = new Button(shell, SWT.PUSH);
				okButton.setText("OK");
				okButton.setBounds(250, 320, 100, 30);
				okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						List<String> newSelectedFields = Arrays.asList(checkboxTableViewer.getCheckedElements())
								.stream().map(Object::toString).collect(Collectors.toList());
						checkedElements = new ArrayList<>(newSelectedFields);
						newSelectedFields.removeAll(selectedFields);
						createColumns(parent, viewer, newSelectedFields);
						selectedFields = checkedElements;
						createSearchBoxes(parent, selectedFields);
						parent.layout();
						loadJiraData("order by created DESC");
						shell.close();
					}
				});

				shell.open();
			}
		});
	}

	// These 6 methods provide functionality to retrieve and update JIRA data in the
	// application:
	// first method fetches JIRA data based on a given JQL query and updates the
	// application's data.
	public void loadJiraData(String jqlQuery) {
		int startAt = currentPageIndex * MAX_RESULTS;
		JSONObject currentUser = GetCurrentJiraUser.getCurrentUser();
		String currentUserId = currentUser.getString("accountId");
		this.lastJqlQuery = jqlQuery;

		System.out.println("Loading Jira data with JQL query: " + jqlQuery);
		clearButtons();

		try (JiraRestClient restClient = new JiraClientProvider().createJiraRestClient()) {
			SearchResult searchResult = performSearch(restClient, jqlQuery, MAX_RESULTS, startAt);
			List<TicketData> ticketsData = processSearchResults(restClient, searchResult, currentUserId);
			updateTicketsDataAndView(ticketsData);
			updatePaginationButtons();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error while connecting to Jira: " + e.getMessage());
		}
	}

	// second method updates the states of the pagination buttons according to the
	// current page index and total issues count.
	private void updatePaginationButtons() {
		if (prevButton != null) {
			prevButton.setEnabled(currentPageIndex > 0);
		}
		if (nextButton != null) {
			nextButton.setEnabled((currentPageIndex + 1) * MAX_RESULTS < totalIssues);
		}
	}

	// third method uses the JIRA Rest Client to perform a search using a provided
	// JQL query, maximum results, and starting point.
	private SearchResult performSearch(JiraRestClient restClient, String jqlQuery, int maxResults, int startAt) {
		SearchRestClient searchRestClient = restClient.getSearchClient();
		Promise<SearchResult> searchJqlPromise = searchRestClient.searchJql(jqlQuery, maxResults, startAt, null);
		SearchResult searchResult = searchJqlPromise.claim();
		totalIssues = searchResult.getTotal();
		System.out.println("Search result count: " + totalIssues);
		return searchResult;
	}

	// 4th method processes the search results returned from JIRA, creating a list
	// of TicketData for issues that involve the current user.
	private List<TicketData> processSearchResults(JiraRestClient restClient, SearchResult searchResult,
			String currentUserId) {
		List<TicketData> ticketsData = new ArrayList<>();
		for (Issue partialIssue : searchResult.getIssues()) {
			Issue fullIssue = restClient.getIssueClient().getIssue(partialIssue.getKey()).claim();
			if (isCurrentUserInvolvedInIssue(restClient, fullIssue, currentUserId)) {
				Map<String, Object> ticketData = createTicketWithSelectedFields(fullIssue);
				ticketsData.add(new TicketData(ticketData));
			}
		}
		return ticketsData;
	}

	// 5th method checks if the current user is involved in a given issue - they are
	// considered involved if they are the assignee, reporter, or a watcher.
	private boolean isCurrentUserInvolvedInIssue(JiraRestClient restClient, Issue issue, String currentUserId) {
		BasicUser assignee = issue.getAssignee();
		User reporter = issue.getReporter();

		if (reporter != null && reporter.getAccountId().equals(currentUserId)) {
			System.out.println("Current user is a reporter for ticket: " + issue.getKey());
			return true;
		}

		if (assignee != null && assignee.getAccountId().equals(currentUserId)) {
			System.out.println("Current user is an assignee for ticket: " + issue.getKey());
			return true;
		}

		Watchers watchers = restClient.getIssueClient().getWatchers(issue.getWatchers().getSelf()).claim();
		for (BasicUser watcher : watchers.getUsers()) {
			if (watcher.getAccountId().equals(currentUserId)) {
				System.out.println("Current user is a watcher for ticket: " + issue.getKey());
				return true;
			}
		}

		return false;
	}

	// 6th method updates the local tickets data with a provided list and refreshes
	// the viewer to reflect the new data.
	private void updateTicketsDataAndView(List<TicketData> ticketsData) {
		this.ticketsData.clear();
		this.ticketsData.addAll(ticketsData);
		viewer.setInput(this.ticketsData);
		viewer.refresh();
	}

	// this method aims to create columns based on the provided fields
	private void createColumns(final Composite parent, final TableViewer viewer, List<String> fields) {
		for (String field : fields) {
			TableViewerColumn col = createTableViewerColumn(field, 120, 0);

			if ("Clé".equals(field)) {
				col.setLabelProvider(new StyledCellLabelProvider() {
					@Override
					public void update(ViewerCell cell) {
						TicketData t = (TicketData) cell.getElement();
						String text = t.getValue(field) == null ? "" : t.getValue(field).toString();
						StyledString styledString = new StyledString(text);
						styledString.setStyle(0, text.length(), StyledString.COUNTER_STYLER);
						cell.setText(styledString.toString());
						cell.setStyleRanges(styledString.getStyleRanges());
					}
				});
			} else {
				col.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						TicketData t = (TicketData) element;
						Object fieldValue = t.getValue(field);
						System.out.println("Field: " + field + ", Value: " + fieldValue);
						return fieldValue == null ? "" : fieldValue.toString();
					}

					@Override
					public void update(ViewerCell cell) {
						TicketData t = (TicketData) cell.getElement();
						String etat = (String) t.getValue("État");

						Color green = new Color(Display.getCurrent(), 174, 245, 179);
						Color red = new Color(Display.getCurrent(), 255, 216, 216);
						Color blue = new Color(Display.getCurrent(), 196, 234, 255);
						try {
							super.update(cell);

							if (Arrays.asList("Description", "Pièce jointe", "Commentaire", "Résolution")
									.contains(field)) {
								createButtonForCell(cell, field);
							}

							if ("Terminé(e)".equals(etat) || "Terminé".equals(etat)) {
								cell.setBackground(green);
							} else if ("À faire".equals(etat)) {
								cell.setBackground(red);
							} else if ("En cours".equals(etat)) {
								cell.setBackground(blue);
							} else {
								cell.setBackground(SampleView.CELL_COLOR);
							}
						} finally {
							green.dispose();
							red.dispose();
							blue.dispose();
						}
					}
				});
			}
		}
	}

	// This method aims to create a button for each cell in a table viewer
	private void createButtonForCell(ViewerCell cell, String field) {
		TableItem item = (TableItem) cell.getItem();
		Button button = new Button((Composite) cell.getViewerRow().getControl(), SWT.PUSH);
		List<Button> buttons = new ArrayList<>();

		TicketData ticketData = (TicketData) cell.getElement();
		ticketButtons.computeIfAbsent(ticketData, k -> buttons).add(button);

		String buttonText;
		Font font = new Font(Display.getDefault(), "Arial", 10, SWT.BOLD);
		Color color = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

		switch (field) {
		case "Description":
			buttonText = field;
			color = Display.getDefault().getSystemColor(SWT.COLOR_RED);
			break;
		case "Pièce jointe":
			buttonText = field;
			color = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
			break;
		case "Résolution":
			String resolution = (String) ((TicketData) cell.getElement()).getValue("Résolution");
			if ("null".equals(resolution)) {
				buttonText = "Resolve";
				color = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			} else {
				buttonText = "Resolved";
				color = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
			}
			break;
		default:
			buttonText = field;
			break;
		}

		button.setText(buttonText);
		button.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
		button.setFont(font);
		button.setForeground(color);

		button.addListener(SWT.Selection, event -> {
			try {
				if ("Résolution".equals(field)) {
					Object value = ticketData.getValue("État");
					Object resolution = ticketData.getValue("Résolution");
					if ("En cours".equals(value) || "À faire".equals(value)) {
						ticketData.setValue("État", "Terminé");
						viewer.update(ticketData, null);

						updateJiraIssue(ticketData);
						if ("null".equals(resolution)) {
							button.setText("Resolved");
							button.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
						} else {
							button.setText("Resolve");
							button.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
						}
					}
				}
				System.out.println("Button clicked: " + field);

				Object value = ticketData.getValue(field);
				if (value == null) {
					System.out.println("Field value is null.");
					return;
				}

				System.out.println("Field value: " + value);

				String description = null;
				if (value instanceof List) {
					StringBuilder descriptionBuilder = new StringBuilder();
					List<Map<String, Object>> allCommentsData = (List<Map<String, Object>>) value;
					for (Map<String, Object> commentData : allCommentsData) {
						String body = (String) commentData.get("body");
						String author = (String) commentData.get("author");
						String time = (String) commentData.get("time");

						String commentDescription = author + " : " + body + "        " + time;
						descriptionBuilder.append(commentDescription).append("\n\n");
					}

					description = descriptionBuilder.toString();
					System.out.println("Description for multiple comments: " + description);
				} else if (value instanceof Map) {
					Map<String, Object> commentData = (Map<String, Object>) value;
					System.out.println("Value is a Map: " + commentData);
					String body = (String) commentData.get("body");
					String author = (String) commentData.get("author");
					String time = (String) commentData.get("time");
					description = author + " : " + body + "        " + time;

					System.out.println("Description for Map: " + description);
				} else if (value instanceof URI) {
					description = value.toString();
					System.out.println("Description for URI: " + description);
				} else if (value instanceof Resolution) {
					description = ((Resolution) value).getName();
					System.out.println("Description for Resolution: " + description);
				} else if (value != null) {
					description = (String) value;
					System.out.println("Description for other value: " + description);
				}

				if (description != null && !field.equals("Résolution")) {
					Display display = Display.getDefault();
					Shell shell = new Shell(display);
					shell.setText(field);
					shell.setLayout(new FillLayout());
					shell.setSize(400, 300);
					Text text = new Text(shell, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
					text.setText(description);
					shell.open();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		TableEditor editor = new TableEditor(item.getParent());
		editor.grabHorizontal = true;
		editor.grabVertical = true;
		editor.setEditor(button, item, cell.getColumnIndex());
		editor.layout();
		tableEditors.add(editor);

	}

	// this method aims to create tickets with the selected fields
	private Map<String, Object> createTicketWithSelectedFields(Issue fullIssue) {
		Map<String, Object> ticketData = new HashMap<>();
		for (String field : selectedFields) {
			switch (field) {
			case "Clé":
				ticketData.put("Clé", fullIssue.getKey() != null ? fullIssue.getKey() : "null");
				break;
			case "Résumé":
				ticketData.put("Résumé", fullIssue.getSummary() != null ? fullIssue.getSummary() : "null");
				break;
			case "Type de ticket":
				ticketData.put("Type de ticket",
						fullIssue.getIssueType() != null ? fullIssue.getIssueType().getName() : "null");
				break;
			case "Priorité":
				ticketData.put("Priorité",
						fullIssue.getPriority() != null ? fullIssue.getPriority().getName() : "null");
				break;
			case "Créateur":
				ticketData.put("Créateur",
						fullIssue.getReporter() != null ? fullIssue.getReporter().getDisplayName() : "null");
				break;
			case "Responsable":
				ticketData.put("Responsable",
						fullIssue.getAssignee() != null ? fullIssue.getAssignee().getDisplayName() : "null");
				break;
			case "Sprint":
				try {
					IssueField sprintField = fullIssue.getFieldByName("Sprint");
					Object sprintValue = sprintField.getValue();
					if (sprintValue != null) {
						String sprintName = null;
						Pattern pattern = Pattern.compile("\"name\":\"([^\"]+)\"");
						Matcher matcher = pattern.matcher(sprintValue.toString());
						if (matcher.find()) {
							sprintName = matcher.group(1);
							ticketData.put("Sprint", sprintName);
						}
					}
				} catch (NullPointerException e) {
					System.out.println("Sprint field is null");
				}
				break;

			case "État":
				ticketData.put("État", fullIssue.getStatus() != null ? fullIssue.getStatus().getName() : "null");
				break;
			case "Projet":
				ticketData.put("Projet", fullIssue.getProject() != null ? fullIssue.getProject().getName() : "null");
				break;
			case "Date d'échéance":
				if (fullIssue.getDueDate() != null) {
					DateTime dueDate = fullIssue.getDueDate();
					Date date = dueDate.toDate();
					ZonedDateTime zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					String formattedDate = zdt.format(formatter);
					ticketData.put("Date d'échéance", formattedDate);
				} else {
					ticketData.put("Date d'échéance", "null");
				}
				break;
			case "Résolution":
				ticketData.put("Résolution",
						fullIssue.getResolution() != null ? fullIssue.getResolution().getName() : "null");
				break;

			case "Description":
				ticketData.put("Description", fullIssue.getDescription() != null ? fullIssue.getDescription() : "null");
				break;
			case "Pièce jointe":
				try {
					Attachment attachment = fullIssue.getAttachments().iterator().next();
					ticketData.put("Pièce jointe", attachment.getThumbnailUri());
				} catch (NoSuchElementException e) {
					ticketData.put("Pièce jointe", "null");
				}
				break;

			case "Commentaire":
				Iterable<Comment> commentsIterable = fullIssue.getComments();
				List<Comment> comments = new ArrayList<>();

				if (commentsIterable != null) {
					for (Comment comment : commentsIterable) {
						comments.add(comment);
					}
				}
				if (!comments.isEmpty()) {
					List<Map<String, Object>> allCommentsData = new ArrayList<>();

					for (Comment comment : comments) {
						Map<String, Object> commentData = new HashMap<>();

						String body = comment.getBody();
						System.out.println("Comment body: " + body);
						commentData.put("body", body != null ? body : "null");

						BasicUser author = comment.getAuthor();
						if (author != null) {
							String authorName = author.getDisplayName();
							System.out.println("Author: " + authorName);
							commentData.put("author", authorName);
						} else {
							System.out.println("Author not found.");
							commentData.put("author", "null");
						}

						DateTime time = comment.getCreationDate();
						System.out.println("Comment creation time: " + time);
						commentData.put("time", time != null ? time.toString() : "null");

						allCommentsData.add(commentData);
					}

					ticketData.put("Commentaire", allCommentsData);
				} else {
					System.out.println("No comment found.");
					ticketData.put("Commentaire", "null");
				}

				break;

			}
		}
		return ticketData;
	}

	// dynamic JQL query generation
	private String generateJqlQuery(List<String> columnNames, List<String> searchStrs) {
		StringBuilder jqlQuery = new StringBuilder();
		for (int i = 0; i < columnNames.size(); i++) {
			if (i < searchStrs.size()) {
				String columnName = columnNames.get(i);
				String jiraFieldName = COLUMN_TO_JIRA_FIELD_MAP.get(columnName);
				String searchStr = searchStrs.get(i);
				if (!searchStr.trim().isEmpty()) {
					if (jqlQuery.length() > 0) {
						jqlQuery.append(" AND ");
					}

					jqlQuery.append(jiraFieldName).append(" = '").append(searchStr).append("'");

				}
			}
		}

		String jqlQueryString = jqlQuery.toString();
		System.out.println("Generated JQL query: " + jqlQueryString);
		return jqlQueryString;
	}

	// this method aims to create search boxes
	private void createSearchBoxes(Composite parent, List<String> columnNames) {
		if (searchComposite != null) {
			searchComposite.dispose();
		}
		searchComposite = new Composite(parent, SWT.NONE);
		GridLayout searchLayout = new GridLayout(columnNames.size(), false);
		searchLayout.marginWidth = 0;
		searchComposite.setLayout(searchLayout);
		List<Control> searchBoxes = new ArrayList<>();
		List<String> searchStrs = new ArrayList<>();

		for (String columnName : columnNames) {
			if (searchColumns.contains(columnName)) {
				searchStrs.add("");
			}
		}

		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			if (!searchColumns.contains(columnName)) {
				continue;
			}
			Control searchControl = createSearchControl(columnName, searchComposite, columnNames, searchStrs,
					searchBoxes);
			searchBoxes.add(searchControl);
		}

		GridData searchGridData = new GridData();
		searchGridData.horizontalSpan = columnNames.size();
		searchGridData.grabExcessHorizontalSpace = true;
		searchGridData.horizontalAlignment = GridData.FILL;
		searchComposite.setLayoutData(searchGridData);
		Control[] children = parent.getChildren();
		int viewerIndex = -1;
		for (int i = 0; i < children.length; i++) {
			if (children[i] == viewer.getControl()) {
				viewerIndex = i;
				break;
			}
		}
		if (viewerIndex != -1) {
			searchComposite.moveAbove(children[viewerIndex]);
		}
		parent.layout();
	}

	private Control createSearchControl(String columnName, Composite parent, List<String> columnNames,
			List<String> searchStrs, List<Control> searchBoxes) {
		Control searchControl;
		if (columnName.equals("Clé")) {
			searchControl = createComboSearchControl(parent, 85,
					GetJiraProjects.getProjectKeys().stream().map(key -> key + "-").collect(Collectors.toList()),
					columnNames, searchStrs, searchBoxes);
		} else if (columnName.equals("Type de ticket")) {
			searchControl = createComboSearchControl(parent, 85, GetJiraIssueTypes.getIssueTypeNames(), columnNames,
					searchStrs, searchBoxes);
		} else if (columnName.equals("Projet")) {
			searchControl = createComboSearchControl(parent, 85, GetJiraProjects.getProjectKeys(), columnNames,
					searchStrs, searchBoxes);
		} else if (columnName.equals("Sprint")) {
			searchControl = createComboSearchControl(parent, 80,
					GetJiraSprints.getSprints().stream().map(Sprint::getName).collect(Collectors.toList()), columnNames,
					searchStrs, searchBoxes);
		} else if (columnName.equals("Priorité")) {
			searchControl = createComboSearchControl(parent, 80, GetJiraPriorities.getPriorityNames(), columnNames,
					searchStrs, searchBoxes);
		} else if (columnName.equals("Responsable")) {
			searchControl = createComboSearchControl(parent, 80,
					new ArrayList<>(GetJiraAssignees.getAssignableUsers("TMDT").values()), columnNames, searchStrs,
					searchBoxes);
		} else {
			Text searchText = new Text(parent, SWT.SEARCH | SWT.CANCEL | SWT.ICON_SEARCH);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			gridData.widthHint = 68;
			searchText.setLayoutData(gridData);
			searchText.setMessage(columnName);
			searchStrs.add("");
			searchControl = searchText;
		}
		searchControl.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					System.out.println("Enter key pressed. Search criteria: " + searchStrs);
					String jqlQuery = generateJqlQuery(columnNames, searchStrs);
					lastSearchJqlQuery = jqlQuery;
					loadJiraData(jqlQuery);
				}
			}
		});
		return searchControl;
	}

	private Combo createComboSearchControl(Composite parent, int widthHint, List<String> items,
			List<String> columnNames, List<String> searchStrs, List<Control> searchBoxes) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.widthHint = widthHint;
		combo.setLayoutData(gridData);
		for (String item : items) {
			combo.add(item);
		}
		combo.addModifyListener(e -> {
			String currentText = combo.getText();
			final String columnName = columnNames.get(searchBoxes.indexOf(combo));
			int index = columnNames.indexOf(columnName);
			if (index >= 0 && index < searchStrs.size()) {
				searchStrs.set(index, currentText);
			} else {
				System.out.println("Index " + index + " is out of bounds for searchStrs");
				searchStrs.add(currentText);
			}
		});
		return combo;
	}

	// this method creates navigation buttons for pagination
	private void createNavigationButtons(Composite parent) {
		Composite buttonsComposite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		buttonsComposite.setLayout(gridLayout);
		prevButton = new Button(buttonsComposite, SWT.PUSH);
		prevButton.setText("Previous");
		prevButton.setEnabled(false);
		prevButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (currentPageIndex > 0) {
					currentPageIndex--;
					loadJiraData(lastJqlQuery);
				}
				prevButton.setEnabled(false);
			}
		});

		nextButton = new Button(buttonsComposite, SWT.PUSH);
		nextButton.setText("Next");
		nextButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ((currentPageIndex + 1) * MAX_RESULTS < totalIssues) {
					currentPageIndex++;
					loadJiraData(lastJqlQuery);
				}
				nextButton.setEnabled((currentPageIndex + 1) * MAX_RESULTS < totalIssues);
			}
		});
		GridData gridData = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
		buttonsComposite.setLayoutData(gridData);
	}

	// this method creates a combo box for selecting the number of results to
	// display per page
	private void createComboBox(Composite parent) {
		Composite comboComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		comboComposite.setLayout(layout);

		GridData gridDataComposite = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
		comboComposite.setLayoutData(gridDataComposite);

		Label comboTitle = new Label(comboComposite, SWT.NONE);
		comboTitle.setText("number of results to display per page:");

		String[] items = new String[] { "10", "20", "50" };
		Combo combo = new Combo(comboComposite, SWT.DROP_DOWN);
		combo.setItems(items);
		combo.select(1);

		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		combo.setLayoutData(gridData);

		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MAX_RESULTS = Integer.parseInt(combo.getText());
				loadJiraData(lastJqlQuery);
			}
		});
	}

	// this method updates issue details in Jira as it was updated in the table
	private void updateJiraIssue(TicketData updatedTicket) {
		try {
			JiraClientProvider jiraClientProvider = new JiraClientProvider();
			JiraRestClient jiraRestClient = jiraClientProvider.createJiraRestClient();

			String issueKey = updatedTicket.getValue("Clé").toString();
			IssueInputBuilder issueInputBuilder = new IssueInputBuilder();

			for (String field : updatedTicket.getFields()) {
				if (!"Clé".equals(field) && !"Créateur".equals(field)) {
					Object fieldValue = updatedTicket.getValue(field);
					System.out.println("field = " + field + ", fieldValue = " + fieldValue + ", fieldValue type = "
							+ fieldValue.getClass().getSimpleName());
					switch (field) {
					case "Résumé":
						issueInputBuilder.setFieldValue("summary", fieldValue);
						break;
					case "Type de ticket":
						issueInputBuilder.setFieldValue("issuetype",
								ComplexIssueInputFieldValue.with("name", fieldValue));
						break;
					case "Projet":
						issueInputBuilder.setFieldValue("project",
								ComplexIssueInputFieldValue.with("name", fieldValue));
						break;
					case "Date d'échéance":
						issueInputBuilder.setFieldValue("duedate", fieldValue);
						break;
					case "Priorité":
						issueInputBuilder.setFieldValue("priority",
								ComplexIssueInputFieldValue.with("name", fieldValue));
						break;
					case "Description":
						issueInputBuilder.setFieldValue("description", fieldValue);
						break;
					case "Sprint":
						Integer sprintId = Integer.valueOf(updatedTicket.getValue("Sprint").toString());
						issueInputBuilder.setFieldValue("customfield_10020", sprintId);
						break;
					case "État":
						String desiredStatusName = (String) fieldValue;
						Issue issue = jiraRestClient.getIssueClient().getIssue(issueKey).claim();
						Iterable<Transition> transitions = jiraRestClient.getIssueClient().getTransitions(issue)
								.claim();
						Transition transitionToDesiredStatus = null;
						for (Transition transition : transitions) {
							if (transition.getName().equalsIgnoreCase(desiredStatusName)) {
								transitionToDesiredStatus = transition;
								break;
							}
						}
						if (transitionToDesiredStatus != null) {
							jiraRestClient.getIssueClient()
									.transition(issue, new TransitionInput(transitionToDesiredStatus.getId())).claim();
						} else {
							System.out.println(
									"Transition to status " + desiredStatusName + " not found for issue " + issueKey);
						}
						break;

					default:
						break;

					}
				}

			}

			IssueInput issueInput = issueInputBuilder.build();
			jiraRestClient.getIssueClient().updateIssue(issueKey, issueInput).claim();
			System.out.println("Le ticket " + issueKey + " a été mis à jour avec succès dans Jira.");
		} catch (Exception e) {
			System.out.println("Échec de la mise à jour du ticket dans Jira. Erreur : " + e.getMessage());
		}
	}

	// This method opens an edit dialog for a selected ticket, allows the user to
	// update its details, and reflects the changes in the application
	private void editTicketDialog(JiraRestClient restClient) {
		IStructuredSelection selection = viewer.getStructuredSelection();
		TicketData selectedTicket = (TicketData) selection.getFirstElement();
		if (selectedTicket != null) {
			EditTicketDialog editDialog = new EditTicketDialog(viewer.getControl().getShell(), selectedTicket);
			if (editDialog.open() == Window.OK) {
				TicketData updatedTicket = editDialog.getUpdatedTicket();
				int selectedIndex = ticketsData.indexOf(selectedTicket);
				ticketsData.set(selectedIndex, updatedTicket);
				viewer.refresh();
				updateJiraIssue(updatedTicket);
			}
		}
	}

	// This code updates the issue details in the table as they are updated in JIRA
	public void updateIssueFieldInTable(String issueKey, String columnName, String newValue,
			BiConsumer<TableItem, Integer> additionalActions) {
		viewer.getControl().getDisplay().asyncExec(() -> {
			Table table = viewer.getTable();
			int columnIndex = getColumnIndexByName(table, columnName);

			if (columnIndex != -1) {
				for (TableItem item : table.getItems()) {
					if (item.getText(0).equals(issueKey)) {
						item.setText(columnIndex, newValue);

						if (additionalActions != null) {
							additionalActions.accept(item, columnIndex);
						}

						if (columnName.equals("Description")) {
							TicketData ticketData = (TicketData) item.getData();
							ticketData.setValue("Description", newValue);
						}

						break;
					}
				}
			}
		});
	}

	public void updateIssueStatusInTable(String issueKey, String newStatus) {
		BiConsumer<TableItem, Integer> additionalActions = (item, columnIndex) -> {
			Color green = new Color(Display.getCurrent(), 174, 245, 179);
			Color red = new Color(Display.getCurrent(), 255, 216, 216);
			Color blue = new Color(Display.getCurrent(), 196, 234, 255);
			Color newColor;
			Table table = viewer.getTable();

			if ("Terminé(e)".equals(newStatus)) {
				newColor = green;
			} else if ("À faire".equals(newStatus)) {
				newColor = red;
			} else if ("En cours".equals(newStatus)) {
				newColor = blue;
			} else {
				newColor = table.getBackground();
			}

			int keyColumnIndex = getColumnIndexByName(table, "Clé");
			for (int i = 0; i < table.getColumnCount(); i++) {
				if (i != keyColumnIndex) {
					item.setBackground(i, newColor);
				}
			}

			green.dispose();
			red.dispose();
			blue.dispose();
		};

		updateIssueFieldInTable(issueKey, "État", newStatus, additionalActions);
	}

	public void updateIssueDescriptionInTable(String issueKey, String newDescription) {
		BiConsumer<TableItem, Integer> additionalActions = (item, columnIndex) -> {
			TicketData ticketData = (TicketData) item.getData();
			ticketData.setValue("Description", newDescription);
		};

		updateIssueFieldInTable(issueKey, "Description", newDescription, additionalActions);
	}

	public void updateIssueSummaryInTable(String issueKey, String newSummary) {
		updateIssueFieldInTable(issueKey, "Résumé", newSummary, null);
	}

	public void updateIssueAssigneeInTable(String issueKey, String newAssignee) {
		updateIssueFieldInTable(issueKey, "Responsable", newAssignee != null ? newAssignee : "null", null);
	}

	public void updateIssueSprintInTable(String issueKey, String newSprint) {
		updateIssueFieldInTable(issueKey, "Sprint", newSprint != null ? newSprint : " ", null);
	}

	public void updateIssueDueDateInTable(String issueKey, String newDueDate) {
		updateIssueFieldInTable(issueKey, "Date d'échéance", newDueDate, null);
	}

	public void updateIssueResolutionInTable(String issueKey, String newResolution) {
		BiConsumer<TableItem, Integer> additionalActions = (item, columnIndex) -> {
			TicketData ticketData = (TicketData) item.getData();
			ticketData.setValue("Résolution", newResolution);

			List<Button> buttonsForTicket = ticketButtons.get(ticketData);
			for (Button button : buttonsForTicket) {
				if ("Resolve".equals(button.getText())) {
					if ("null".equals(newResolution)) {
						button.setText("Resolve");
						button.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
					} else {
						button.setText("Resolved");
						button.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
					}
					break;
				}
			}
		};

		updateIssueFieldInTable(issueKey, "Résolution", newResolution, additionalActions);
	}

	public void updateIssuePriorityInTable(String issueKey, String newPriority) {
		updateIssueFieldInTable(issueKey, "Priorité", newPriority, null);
	}

	public void updateIssueTypeInTable(String issueKey, String newType) {
		updateIssueFieldInTable(issueKey, "Type de ticket", newType, null);
	}

	private int getColumnIndexByName(Table table, String columnName) {
		for (int i = 0; i < table.getColumnCount(); i++) {
			if (table.getColumn(i).getText().equals(columnName)) {
				return i;
			}
		}
		return -1;
	}

	// This code deletes the issue from the table as it is deleted in JIRA
	public void removeTicket(String issueKey) {
		for (TableItem item : viewer.getTable().getItems()) {
			TicketData ticketData = (TicketData) item.getData();
			if (issueKey.equals(ticketData.getValue("Clé"))) {
				System.out.println("Found issue in viewer: " + issueKey);

				if (ticketButtons.containsKey(ticketData)) {
					for (Button button : ticketButtons.get(ticketData)) {
						button.dispose();
					}
					ticketButtons.remove(ticketData);
				}

				viewer.remove(ticketData);
				break;
			}
		}
	}

	// This code creates an "Add" button in the toolbar, which opens a dialog to add
	// a new ticket and updates the viewer accordingly
	private void createAddButton(final Composite parent) {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		ImageDescriptor addImageDescriptor = Activator.getImageDescriptor("icons/addicon.png");
		Action addAction = new Action() {
			public void run() {
				System.out.println("Selected fields in createAddButton: " + selectedFields);
				AddTicketDialog dialog = new AddTicketDialog(parent.getShell(), new ArrayList<>(selectedFields));
				dialog.setViewer(viewer); // Assuming 'viewer' is the TableViewer instance
				int result = dialog.open();
				if (result == Window.OK) {
					totalIssues++;

					if (ticketsData.size() > MAX_RESULTS) {
						currentPageIndex++;
						ticketsData.remove(ticketsData.size() - 1);
					}

					viewer.refresh();
				}
			}
		};
		addAction.setImageDescriptor(addImageDescriptor);
		addAction.setToolTipText("Add a ticket");
		toolbarManager.add(addAction);
	}

	// This method checks if the given TicketData object is already present in the
	// ticketsData list
	public boolean containsTicket(TicketData ticket) {
		for (TicketData existingTicket : ticketsData) {
			if (existingTicket.getValue("Clé").equals(ticket.getValue("Clé"))) {
				return true;
			}
		}
		return false;
	}

	// This method retrieves the ticket data for a given issue key from JIRA
	public TicketData getTicketData(String issueKey) {
		System.out.println("Connecting to Jira server...");
		JiraClientProvider jiraClientProvider = new JiraClientProvider();
		JiraRestClient restClient = jiraClientProvider.createJiraRestClient();
		System.out.println("Jira client connected");
		Issue fullIssue = restClient.getIssueClient().getIssue(issueKey).claim();
		Map<String, Object> ticketData = createTicketWithSelectedFields(fullIssue);
		return new TicketData(ticketData);
	}

	// This method clears and disposes all buttons and associated table editors
	private void clearButtons() {
		for (TableEditor editor : tableEditors) {
			Button button = (Button) editor.getEditor();
			if (button != null && !button.isDisposed()) {
				button.dispose();
			}
			editor.dispose();
		}
		tableEditors.clear();
	}

	// Other methods
	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	public TableViewer getViewer() {
		return viewer;
	}

	public int getMaxResults() {
		return MAX_RESULTS;
	}

	public int getCurrentPageIndex() {
		return currentPageIndex;
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				SampleView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("Edit") {
			public void run() {
				editTicketDialog(restClient);
			}
		});

	}

	public static void disposeColors() {
		COLUMN_HEADER_COLOR.dispose();
		CELL_COLOR.dispose();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

}
