#!/bin/bash

if [ "$(git branch | grep '* master')" = "* master" ]; then
    >&2 echo "You are on the master branch.  Do you wish to publish to the production tag?"
    select yn in "Yes" "No"; do
	case $yn in
	    Yes ) VERSION_TAG="production"; break;;
	    No ) VERSION_TAG="latest"; break;;
	esac
    done
else
    VERSION_TAG="latest"
fi

echo "${VERSION_TAG}"
