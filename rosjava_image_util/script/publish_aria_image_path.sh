#!/usr/bin/env sh

TOPIC_NAME="/eus_animeface/detect_request/string";
# if [ "$1" ];
# then
#     TOPIC_NAME=$1;
# fi;

rostopic pub -r 1 $TOPIC_NAME std_msgs/String "`rospack find rosjava_test`/rosjava_image_util/img/aria.jpg" ;
