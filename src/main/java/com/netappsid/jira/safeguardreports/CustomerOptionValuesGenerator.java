package com.netappsid.jira.safeguardreports;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;

public class CustomerOptionValuesGenerator implements ValuesGenerator
{
	@Override
	public Map getValues(Map params)
	{
		GenericValue projectGV = (GenericValue) params.get("project");
		Long projectId = (Long) projectGV.get("id");
		Map<Long, String> customerMap = ListOrderedMap.decorate(new HashMap());
		if (projectGV != null)
		{
			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
			OptionsManager optionsManager = ComponentAccessor.getOptionsManager();

			Collection<CustomField> customFields = customFieldManager.getCustomFieldObjectsByName("Customer");
			for (CustomField customField : customFields)
			{
				List<FieldConfigScheme> configurationSchemes = customField.getConfigurationSchemes();
				for (FieldConfigScheme configurationScheme : configurationSchemes)
				{
					List<JiraContextNode> contexts = configurationScheme.getContexts();
					for (JiraContextNode context : contexts)
					{
						if (context.getProjectId().equals(projectId))
						{
							FieldConfig config = configurationScheme.getOneAndOnlyConfig();
							if (config != null)
							{
								Options options = optionsManager.getOptions(config);
								options.forEach((option) -> {
									customerMap.put(option.getOptionId(), option.getValue());
								});
							}

						}
					}
				}
			}
		}
		return customerMap;
	}
}