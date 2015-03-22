#!/bin/bash

version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)')
echo "Project version: $version"

if [[ "${version}" == *SNAPSHOT ]] || [ "${TRAVIS_PULL_REQUEST}" == "true" ]; then
	echo "Snapshot build, omitting javadoc"
	echo "Running 'mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V'"
	mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V
else
	echo "Release build, generating javadoc"
	echo "Running 'mvn install -DskipTests=true -B -V'"
	mvn install -DskipTests=true -B -V
fi
