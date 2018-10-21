#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

case "$TEST" in

ci)
  mvn verify -B -e -V
  ;;

plugin)
  installTravisTools

  mvn package -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  if [ "$SQ_VERSION" = "DEV" ] ; then
    build_snapshot "SonarSource/sonarqube"
  fi

  cd its/$TEST
  mvn package -Dsonar.runtimeVersion="$SQ_VERSION" -DjavaVersion="LATEST_RELEASE" -Dmaven.test.redirectTestOutputToFile=false
  ;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
