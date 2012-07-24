Installation
------------

Add the following repository to your pom.xml

	<repository>
			<id>scalascope-releases</id>
			<releases>
				<enabled>true</enabled>
				<updatepolicy>always</updatepolicy>
				<checksumpolicy>fail</checksumpolicy>
			</releases>
			<snapshots>
				<enabled>false</enabled>
				<updatepolicy>always</updatepolicy>
				<checksumpolicy>warn</checksumpolicy>
			</snapshots>
			<url>https://raw.github.com/scalascope/mavenrepo/master/releases</url>
	</repository>

and maven dependency

	<dependency>
		<groupId>com.mixpanel</groupId>
		<artifactId>mixpanel-java</artifactId>
		<version>0.1</version>
	</dependency>

Configuration
-------------

Put mixpanel.properties to the classpath.

	mixpanel.submitThreadTTL = 180000
	mixpanel.threadNumber = 3

Use
---

	MPMetrics mpMetrics = MPMetrics.getInstance("your_token");
	Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("ip", "123.123.123.123");
    properties.put("action", "test");

    mpMetrics.track("testEvent", properties);

