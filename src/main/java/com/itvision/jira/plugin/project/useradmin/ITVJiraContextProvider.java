package com.itvision.jira.plugin.project.useradmin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.UserUtils;

/**
 * The ContextProvider is mapped to the Project Admin User Panel in
 * the atlassian-plugin.xml (resources)<br/>
 * The getContextMap exposes some objects to the view page (templates/useradmin.vm)
 * and makes the applicationProperties accessable to the velocity skript (useradmin.vm)<br/>
 * The ITVProjectUserAdminBean with most of the method calls is also handed over to vm
 * by the returned map.
 * @author itv kit
 *
 */
@SuppressWarnings("unchecked")
public class ITVJiraContextProvider extends AbstractJiraContextProvider {

	@SuppressWarnings("unused")
	private final ApplicationProperties applicationProperties;
	private static Collection<User> allUsers;
	private ITVProjectUserAdminBean itvProjectAdmBean = new ITVProjectUserAdminBean();
	static {
		allUsers = UserUtils.getAllUsers();
	}

	public ITVJiraContextProvider(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map getContextMap(User user, JiraHelper helper) {

		Map returnMap = new HashMap<String, Object>();
		returnMap.put("jiraHelper", helper);
		returnMap.put("projectUser", user);
		if (helper != null)
			returnMap.put("contextParams", helper.getContextParams());
		returnMap.put("allUsers", allUsers);
		returnMap.put("adminBean", itvProjectAdmBean);
		return returnMap;
	}

	public String getMyString() {
		return "this is my string";
	}
}
