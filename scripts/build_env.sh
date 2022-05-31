#!/bin/bash
set -e
sudo echo '#!/bin/bash'

sbt universal:packageBin
