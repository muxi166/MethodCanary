#!/usr/bin

sh disable_plugin.sh

./gradlew :plugin:clean :plugin:bintrayUpload -PbintrayUser=kyson -PbintrayKey=$BINTRAY_KEY -PdryRun=false

./gradlew :lib:clean :lib:bintrayUpload -PbintrayUser=kyson -PbintrayKey=$BINTRAY_KEY -PdryRun=false

./gradlew :libnoop:clean :libnoop:bintrayUpload -PbintrayUser=kyson -PbintrayKey=$BINTRAY_KEY -PdryRun=false

sh enable_plugin.sh.sh
