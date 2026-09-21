#!/bin/bash

# Usage: ./verifyall.sh [forge|fabric|whatever to put before ":classes"]

if [ -n "$1" ]; then
    prefix="$1:"
fi

clear
trap "echo; exit" INT

declare -a completed_builds
for version in $(ls ./versionProperties/); do
    version=${version%".properties"}
    
    result=""
    if ./gradlew "$prefix"classes -PmcVer=$version; then
        result+="\e[1;32m"
        echo -ne "\e[1;32m"
    else
        result+="\e[1;31m"
        echo -ne "\e[1;31m"
    fi
    result+=$version
    result+="\e[0m"
    
    version_length=${#version}
    top_chars=$(printf '^%.0s' $(seq 1 $version_length))
    bottom_chars=$(printf '=%.0s' $(seq 1 $version_length))
    echo "# $top_chars"
    echo "# $version"
    echo "# $bottom_chars"
    echo -e "\e[0m"
    
    completed_builds+=($result)
done

./gradlew clean
./gradlew classes

echo
echo -e "\e[1mBuild results:\e[0m"
echo -e "${completed_builds[*]}"