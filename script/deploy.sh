#!/bin/bash

if [ "${TRAVIS_PULL_REQUEST}" == "true" ] || [ "${TRAVIS_BRANCH}" != "master" ]; then
	echo "Not master branch or pull request, aborting deployment"
	exit 0
fi

version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)')
echo "Project version: $version"

echo "Writing settings.xml"
echo "<settings><servers><server><id>github</id><password>${OAUTH2_DEPLOY_TOKEN}</password></server></servers></settings>" > ~/settings.xml

echo "Retrieving maven-metadata.xml"
wget --no-check-certificate -q -P target/mvn-repo/com/github/theholywaffle/teamspeak3-api raw.githubusercontent.com/TheHolyWaffle/TeamSpeak-3-Java-API/mvn-repo/com/github/theholywaffle/teamspeak3-api/maven-metadata.xml

if [[ "$version" == *SNAPSHOT ]]; then
	echo "Snapshot build, deploying artifacts"
	echo "Running 'mvn deploy --settings ~/settings.xml -DskipTests=true -P snapshot -B -V'"
	mvn deploy --settings ~/settings.xml -DskipTests=true -P snapshot -B -V
else
	echo "Release build, deploying artifacts and publishing javadocs"
	echo "Running 'mvn deploy --settings ~/settings.xml -DskipTests=true -B -V'"
	mvn deploy --settings ~/settings.xml -DskipTests=true -B -V
fi
