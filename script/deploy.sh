#!/bin/bash

downloadMetadata() {
	echo "Retrieving $1/$2"
	from="raw.githubusercontent.com/TheHolyWaffle/TeamSpeak-3-Java-API/mvn-repo/com/github/theholywaffle/teamspeak3-api$1/$2"
	to="target/mvn-repo/com/github/theholywaffle/teamspeak3-api$1"

	download "$from" "$to"
	download "$from.md5" "$to"
	download "$from.sha1" "$to"
}

download() {
	echo "Downloading $1 to $2"
	wget --no-check-certificate -q -P $2 $1
}

if [ "${TRAVIS_PULL_REQUEST}" == "true" ] || [ "${TRAVIS_BRANCH}" != "master" ]; then
	echo "Not master branch or pull request, aborting deployment"
	exit 0
fi

version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ex '^[[:digit:]][^/]+$')
echo "Project version: $version"

echo "Writing settings.xml"
echo "<settings><servers><server><id>github</id><password>${OAUTH2_DEPLOY_TOKEN}</password></server></servers></settings>" > ~/settings.xml

echo "Retrieving Maven metadata files"
downloadMetadata "" "maven-metadata.xml"
downloadMetadata "/$version" "maven-metadata.xml"

if [[ "$version" == *SNAPSHOT ]]; then
	echo "Snapshot build, deploying artifacts"
	echo "Running 'mvn deploy --settings ~/settings.xml -DskipTests=true -P snapshot -B -V'"
	mvn deploy --settings ~/settings.xml -DskipTests=true -P snapshot -B -V
else
	echo "Release build, deploying artifacts and publishing javadocs"
	echo "Running 'mvn deploy --settings ~/settings.xml -DskipTests=true -B -V'"
	mvn deploy --settings ~/settings.xml -DskipTests=true -B -V
fi
