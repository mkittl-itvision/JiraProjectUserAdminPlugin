package com.itvision.jira.plugin.project.useradmin;

import com.atlassian.sal.api.ApplicationProperties;

public class ITVProjectAdminPluginComponentImpl implements ITVProjectAdminPluginComponent
{
    private final ApplicationProperties applicationProperties;

    public ITVProjectAdminPluginComponentImpl(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    public String getName()
    {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName();
        }
        
        return "myComponent";
    }
}