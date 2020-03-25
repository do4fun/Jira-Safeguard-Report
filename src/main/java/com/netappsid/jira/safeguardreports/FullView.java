package com.netappsid.jira.safeguardreports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.apache.lucene.search.Collector;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.component.pico.ComponentManager;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.customfields.statistics.CustomFieldStattable;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.search.ReaderCache;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchQuery;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.statistics.FilterStatisticsValuesGenerator;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.issue.statistics.StatsGroup;
import com.atlassian.jira.issue.statistics.util.OneDimensionalDocIssueHitCollector;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;

@Scanned
public class FullView extends AbstractReport
{
	// customfield_10000
	private static final Logger log = Logger.getLogger(FullView.class);
	private static final int MAX_HEIGHT = 360;
	private long maxCount = 0;
	private Collection<Long> openIssuesCounts = new ArrayList<>();
	private Collection<String> formattedDates = new ArrayList<>();
	@JiraImport
	private final SearchProvider searchProvider;
	@JiraImport
	private final ProjectManager projectManager;
	@JiraImport
	private final com.atlassian.jira.datetime.DateTimeFormatter formatter;
	@JiraImport
	private final SearchRequestService searchRequestService;
	@JiraImport
	private final IssueFactory issueFactory;
	@JiraImport
	private final ReaderCache readerCache;
	@JiraImport
	private final FieldVisibilityManager fieldVisibilityManager;
	@JiraImport
	private final FieldManager fieldManager;
	@JiraImport
	private final IssueIndexManager issueIndexManager;

	private Date startDate;
	private Date endDate;
	private Long projectId;

	public FullView(SearchProvider searchProvider, ProjectManager projectManager, FieldVisibilityManager fieldVisibilityManager,
			IssueIndexManager issueIndexManager, IssueFactory issueFactory, ReaderCache readerCache, FieldManager fieldManager,
			DateTimeFormatterFactory dateTimeFormatterFactory, SearchRequestService searchRequestService)
	{
		this.searchProvider = searchProvider;
		this.projectManager = projectManager;
		this.searchRequestService = searchRequestService;
		this.issueFactory = issueFactory;
		this.readerCache = readerCache;
		this.issueIndexManager = issueIndexManager;
		this.fieldManager = fieldManager;
		this.fieldVisibilityManager = fieldVisibilityManager;
		this.formatter = dateTimeFormatterFactory.formatter().withStyle(DateTimeStyle.DATE).forLoggedInUser();
	}

	@Override
	public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception
	{
		// action.getLoggedInUser() since Jira 7.0.
		// getLoggedInApplicationUser() since Jira 5.2
		String filterId = (String) params.get("filterid");
		String mapperName = action.getText("report.fullview.mappername");
		ApplicationUser loggedInUser = action.getLoggedInUser();

		StatisticsMapper statsMapper = new FilterStatisticsValuesGenerator().getStatsMapper(FilterStatisticsValuesGenerator.COMPONENTS);
		JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(action.getLoggedInUser());
		final SearchRequest request = searchRequestService.getFilter(jiraServiceContext, new Long(filterId));

		Map<String, Object> velocityParams = new HashMap<>();
		velocityParams.put("startDate", formatter.format(startDate));
		velocityParams.put("endDate", formatter.format(endDate));
		velocityParams.put("statsGroup", searchMapIssueKeys(request, action.getLoggedInUser(), statsMapper));
		return descriptor.getHtml("view", velocityParams);
	}

	private void getComponentName(Long id)
	{
		CustomFieldManager customFieldManager = ComponentManager.getInstance().getComponent(CustomFieldManager.class);
		List customFieldObjects = customFieldManager.getCustomFieldObjects();
		Map allValues = new ListOrderedMap();
		Iterator iterator = customFieldObjects.iterator();
		while (iterator.hasNext())
		{
			CustomField customField = (CustomField) iterator.next();
			if (customField.getCustomFieldSearcher() instanceof CustomFieldStattable)
			{
				allValues.put(customField.getId(), customField.getName());
			}
		}

	}

	public StatsGroup searchMapIssueKeys(SearchRequest request, ApplicationUser searcher, StatisticsMapper mapper)
			throws SearchException
	{
		StatsGroup statsGroup = new StatsGroup(mapper);
		Collector hitCollector = new OneDimensionalDocIssueHitCollector(mapper.getDocumentConstant(), statsGroup,
				issueIndexManager.getIssueSearcher().getIndexReader(), issueFactory,
				fieldVisibilityManager, readerCache, fieldManager);
		if (request != null)
		{
			// searchProvider.searchAndSort(request.getQuery(), searcher, hitCollector, PagerFilter.getUnlimitedFilter());
			SearchQuery searchQuery = SearchQuery.create(request.getQuery(), searcher);
			searchProvider.search(searchQuery, hitCollector);
		}
		return statsGroup;
	}

	@Override
	public void validate(ProjectActionSupport action, Map params)
	{
		String filterId = (String) params.get("filterid");
		try
		{
			startDate = formatter.parse(ParameterUtils.getStringParam(params, "startDate"));
		}
		catch (IllegalArgumentException e)
		{
			action.addError("startDate", action.getText("report.issuecreation.startdate.required"));
			log.error("Exception while parsing startDate");
		}

		try
		{
			endDate = formatter.parse(ParameterUtils.getStringParam(params, "endDate"));
		}
		catch (IllegalArgumentException e)
		{
			action.addError("endDate", action.getText("report.issuecreation.enddate.required"));
			log.error("Exception while parsing endDate");
		}

		projectId = ParameterUtils.getLongParam(params, "selectedProjectId");
		if (projectId == null || projectManager.getProjectObj(projectId) == null)
		{
			action.addError("selectedProjectId", action.getText("report.issuecreation.projectid.invalid"));
			log.error("Invalid projectId");
		}

		if (startDate != null && endDate != null && endDate.before(startDate))
		{
			action.addError("endDate", action.getText("report.issuecreation.before.startdate"));
			log.error("Invalid dates: start date should be before end date");
		}
		try
		{
			JiraServiceContextImpl serviceContext = new JiraServiceContextImpl(action.getLoggedInUser(), new SimpleErrorCollection());
			SearchRequest searchRequest = searchRequestService.getFilter(serviceContext, new Long(filterId));
			if (searchRequest == null)
			{
				action.addErrorMessage(action.getText("report.error.no.filter"));
			}
		}
		catch (NumberFormatException nfe)
		{
			action.addError("filterId", action.getText("report.error.filter.id.not.a.number", filterId));
		}
	}

}
