INTEGRATION TESTING
===================

TESTSUITE STRUCTURE
-------------------

|  -  testsuite (all non functional tests, maven module) -Dts (to enable)
   |
   |  -  jee (JEE related integration tests) -Dts.skip.integration.jee (to skip jee tests)




To enable testsuite tests run:

    $ mvn clean install -Dts -Pjboss_managed_local

In this scenario jee and jee_cluster integration tests will run by default.

    - To skip jee integration tests run:
      $ mvn clean install -Dts -Dts.skip.integration.jee



By default, the JBoss of your local system is used to run the jee integration tests (the "JBOSS_HOME" environment variable
must be configured correctly on your system, and all required JBoss modules must be install in your JBoss)

Or you can choose to use the "jboss_managed_local" profile to download JBoss, install required JBoss modules automatically
and run the jee integration tests as below:

    $ mvn clean install -Dts -Pjboss_managed_local



NOTE: Functional tests are located in MODULE_X/src/test folder