You have successfully created an Atlassian Plugin!

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK


use atlas-mvn package

atlas-run
or 
atlas-debug --jvm-debug-port 7456

If you want to retrieve data,
cut and paste database directory under Jira-Safeguard-Report\target\jira\home\database

restart the server 
atlas-run
or 
atlas-debug --jvm-debug-port 7456

select the gears (Admin Setup) and Issues
