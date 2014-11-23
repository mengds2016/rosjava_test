#!/usr/bin/env sh

TOPIC_NAME="/aria_image/path std_msgs/String";
if [ "$1" ];
then
    TOPIC_NAME=$1;
fi;

rostopic pub -r 1 $TOPIC_NAME "`rospack find rosjava_test`/rosjava_image_util/img/aria.jpg" ;
