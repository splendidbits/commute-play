#!/bin/bash
COMMUTE_VERSION="commutealerts-0.3"
TEMP_DIR="commutealerts.temp"

cd /var/play/splendidbits

echo "* Downloading updated Commute application sources"
sudo rm -r $TEMP_DIR
git clone -b master https://splendidbits:9d560e33fd0c4382d2957744b907895bc589637e@github.com/splendidbits/commute-play $TEMP_DIR
cd $TEMP_DIR

echo "* Compiling updated application sources"
/var/play/splendidbits/sbt/bin/sbt clean compile stage dist

echo "* Stopping Commute Alerts Service"
sudo service commutealerts stop
sudo service postgresql stop

echo "* Replacing previous application"
cd /var/play/splendidbits
sudo rm -r $COMMUTE_VERSION
sudo mv $TEMP_DIR/ $COMMUTE_VERSION

rm commutealerts
ln -s $COMMUTE_VERSION/ commutealerts

echo "* Restarting Commute Alerts Service"
sudo service postgresql start
sudo service commutealerts start
sudo service commutealerts status
cd ~

## Optional if modules are installed.
# cd commutealerts-0.3
#git submodule update --init --recursive
#git pull --all --recurse-submodules
