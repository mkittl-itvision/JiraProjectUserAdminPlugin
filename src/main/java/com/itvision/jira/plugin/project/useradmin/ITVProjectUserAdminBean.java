package com.itvision.jira.plugin.project.useradmin;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.AddException;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleActor;
import com.atlassian.jira.security.roles.ProjectRoleActors;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.SimpleErrorCollection;

public class ITVProjectUserAdminBean {

	private String errorMessage = "";
	
	public ITVProjectUserAdminBean() {
	}
	
	public String getTestString() {
		return "To test if a bean is recognized";
	}
	
	public Collection<User> getAllUser() {
		return UserUtils.getAllUsers();
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public Map<String, String> getUsersForRole(String roleName, String projectName) {
		Map<String, String> nameMMap = new HashMap<String, String>();
		
		ProjectRoleService aProjectRoleService = ComponentAccessor.getComponentOfType(ProjectRoleService.class);
		SimpleErrorCollection aSimpleErrorCollection = new SimpleErrorCollection();
		ProjectRole aProjectRole = aProjectRoleService.getProjectRoleByName(roleName, aSimpleErrorCollection);
		Project aProject = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase(projectName);
		ProjectRoleActors theProjectRoleActors = aProjectRoleService.getProjectRoleActors(aProjectRole, aProject, aSimpleErrorCollection);
		Set<ApplicationUser> theUsers = theProjectRoleActors.getApplicationUsers();
		for (ApplicationUser aUser : theUsers) {
			System.out.println(
				MessageFormat.format("{0} {1} {2} {3}", aUser.getKey(), aUser.getName(), aUser.getUsername(), aUser.getDisplayName()));
			nameMMap.put(aUser.getKey(), aUser.getDisplayName());
		}
		return nameMMap;
	}
	
	public void deleteUsers(String[] userList, String roleName, String projectName) {
		if (userList != null) {
			Collection<String> aUserCollection = Arrays.asList(userList);
			ProjectRoleService aProjectRoleService = ComponentAccessor.getComponentOfType(ProjectRoleService.class);
			SimpleErrorCollection aSimpleErrorCollection = new SimpleErrorCollection();
			ProjectRole aProjectRole = aProjectRoleService.getProjectRoleByName(roleName, aSimpleErrorCollection);
			Project aProject = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase(projectName);
			aProjectRoleService.removeActorsFromProjectRole(aUserCollection, aProjectRole, aProject, 
				ProjectRoleActor.USER_ROLE_ACTOR_TYPE, aSimpleErrorCollection);
			ApplicationUser aLoggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
			for (String aUser : userList) {
				ApplicationUser userToDelete = ComponentAccessor.getUserUtil().getUserByKey(aUser);
				ComponentAccessor.getUserUtil().removeUser(aLoggedInUser, userToDelete);
				System.out.println(MessageFormat.format("{0} {1} {2} deleted", aUser, roleName, projectName));
			}
		}
	}
		
	public void createNewUser(String userName, String password, String eMail, String displayName, 
			String groupName, String roleName, String projectKey) {
		if (ComponentAccessor.getUserUtil() != null) {
			try {
				// new user
				User aUser = ComponentAccessor.getUserUtil().createUserNoNotification(userName, password, eMail, displayName);
				// delete default groups
				Collection<Group> aGroupSet = ComponentAccessor.getUserUtil().getGroupsForUser(userName);
				ComponentAccessor.getUserUtil().removeUserFromGroups(aGroupSet, aUser);
				// add correct group
				Group aNewGroup = ComponentAccessor.getUserUtil().getGroupObject(groupName);
				ComponentAccessor.getUserUtil().addUserToGroup(aNewGroup, aUser);
				// add user to correct project role
				Project aProject = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase(projectKey);
				ProjectRoleService aProjService = ComponentAccessor.getComponentOfType(ProjectRoleService.class);
				SimpleErrorCollection errorCollection = new SimpleErrorCollection();
				ProjectRole aProjectRole = aProjService.getProjectRoleByName(roleName, errorCollection);
				Collection<String> aSet = new HashSet<String>();
				aSet.add(aUser.getName());
				System.out.println("Adding : " +aUser.getName());
				aProjService.addActorsToProjectRole(aSet, aProjectRole, aProject, ProjectRoleActor.USER_ROLE_ACTOR_TYPE, errorCollection);
				for (String aMessage : errorCollection.getErrorMessages()){
					System.out.println(aMessage);
				}
				System.out.println("User successfully created.");
				errorMessage = null;
			} catch (PermissionException pe) {
				System.err.println("User has no Permission to create a user! " + pe.getLocalizedMessage());
				errorMessage = pe.getLocalizedMessage();
			} catch (CreateException ce) {
				System.err.println("could not create user. Perhaps this username already exists. " + ce.getLocalizedMessage());
				errorMessage = ce.getLocalizedMessage();
			} catch (RemoveException re) {
				System.err.println("could not create user. Cannot remove from default Group. " + re.getLocalizedMessage());
				errorMessage = re.getLocalizedMessage();				
			} catch (AddException ae) {
				System.err.println("could not create user. Cannot add to selected Group. " + ae.getLocalizedMessage());
				errorMessage = ae.getLocalizedMessage();								
			}
		} else {
			System.err.println("cannot access userutils. User creation not possible!");
			errorMessage = "Error: cannot acess userutils. Internal problem";
		}
		
	}
}
