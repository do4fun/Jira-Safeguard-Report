package com.netappsid.jira.safeguardreports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.option.Option;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.issue.statistics.StatsGroup;

public class StatsGroupByCustomerOption extends StatsGroup
{

	private Long selectedProjectId;
	private Map<Option, Map<ProjectComponent, Collection<Issue>>> customerOptionProjectComponentMap;
	private Map<Option, Collection<Issue>> issuesWithoutComponents;

	public StatsGroupByCustomerOption(StatisticsMapper<?> mapper, Long projectId)
	{
		super(mapper);
		selectedProjectId = projectId;
	}

	public void buildComponentsByCustomerMap(Option option)
	{
		customerOptionProjectComponentMap = new HashMap<Option, Map<ProjectComponent, Collection<Issue>>>();
		issuesWithoutComponents = new HashMap<Option, Collection<Issue>>();
		issuesWithoutComponents.put(option, new ArrayList<Issue>());

		Map<ProjectComponent, Collection<Issue>> map = new HashMap<ProjectComponent, Collection<Issue>>();
		Iterator<Issue> it = ((Collection<Issue>) this.get(option)).iterator();
		while (it.hasNext())
		{
			Issue issue = it.next();

			if (issue.getComponents().size() > 0)
			{
				Iterator<ProjectComponent> componentIterator = issue.getComponents().iterator();
				ProjectComponent component = componentIterator.next();
				if (map.containsKey(component))
				{
					map.get(component).add(issue);
				}
				else
				{
					map.put(component, new ArrayList<Issue>()
						{
							{
								add(issue);
							}
						});
				}
			}
			else
			{
				issuesWithoutComponents.get(option).add(issue);
			}
		}
		customerOptionProjectComponentMap.put(option, map);
	}

	public Collection<ProjectComponent> getCustomerOptions(Option option)
	{
		return customerOptionProjectComponentMap.get(option).keySet();
	}

	public Collection<Issue> getIssuesByComponent(Option option, ProjectComponent component) throws Exception
	{
		return customerOptionProjectComponentMap.get(option).get(component);
	}

	public Collection<Issue> getIssuesWithoutComponent(Option option)
	{
		return issuesWithoutComponents.get(option);
	}

}
