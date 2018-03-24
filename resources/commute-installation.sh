#!/bin/bash
COMMUTE_VERSION="commutealerts-0.3"

echo "* Stopping Commute Alerts Service"
sudo service commutealerts stop 
cd /var/play/splendidbits

echo "* Removing previous application"
sudo rm -r $COMMUTE_VERSION

echo "* Downloading updated Commute application sources"
git clone -b master https://splendidbits:9d560e33fd0c4382d2957744b907895bc589637e@github.com/splendidbits/commute-play $COMMUTE_VERSION
ln -s $COMMUTE_VERSION commutealerts
cd $COMMUTE_VERSION

echo "* Compiling updated application sources"
../sbt/bin/sbt clean compile stage dist

echo "* Restarting Commute Alerts Service"
sudo service commutealerts start
sudo service commutealerts status
cd ~

## Optional if modules are installed.
# cd commutealerts-0.3
#git submodule update --init --recursive
#git pull --all --recurse-submodules
