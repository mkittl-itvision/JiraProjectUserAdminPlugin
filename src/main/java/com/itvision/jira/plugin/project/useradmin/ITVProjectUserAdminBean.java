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

/**
 * ITVProjectUserAdminBean is kind of a Backing bean and provides methods
 * to be called in useradmin.vm script.<br/>
 * This class is handed over to the script using the ITVJiraContextProvider
 * @author itv kit
 *
 */
public class ITVProjectUserAdminBean {

	private String errorMessage = "";
	
	public ITVProjectUserAdminBean() {
	}
	
	/* Please delete me again */
	public String getTestString() {
		return "To test if a bean is recognized";
	}
	
	/* Please delete me again */
	public Collection<User> getAllUser() {
		return UserUtils.getAllUsers();
	}
	
	/**
	 * useradmin.vm uses this method to determine if an error has occurred
	 * and shows this message
	 * @return error message or null
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * returns a list of users that are contained in a specific role.<br>
	 * useradmin.vm builds the list with the checkboxes with this list
	 * @param roleName the role name like Handwerker
	 * @param projectName the project name that useradmin.vm finds via jiraHelper
	 * @return a map that maps the user key to the display name
	 */
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
	
	/**
	 * deletes users that are contained in the list, the role within the project.<br/>
	 * The list is returned in aseradmin.vm form the request parameter values. <br/>
	 * Only user role actors are deleted! It does not care about group role actors!<br> 
	 * @param userList the list with the user keys
	 * @param roleName the role name like Handwerker
	 * @param projectName the project name found in the useradmin.vm
	 */
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
		
	/**
	 * Creates a user that is applied to the correct group (defaults are deleted before) and
	 * the correct role.<br/>
	 * If something goes wrong (e.g. user already exists) only the errorMessage is set, otherwise
	 * the errorMessage is null showing that the user creation succeeded.
	 * @param userName the login user name
	 * @param password the login password
	 * @param eMail the email address
	 * @param displayName the human readable displayed name
	 * @param groupName the group it should belong to (only one group is possible!)
	 * @param roleName the role like Handwerker the new user should be assigned to
	 * @param projectKey the project key from useradmin.vm
	 */
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
