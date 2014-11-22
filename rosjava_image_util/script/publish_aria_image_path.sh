#!/usr/bin/env sh

rostopic pub -r 1 /sensor_image_node/left/command/string std_msgs/String "`rospack find rosjava_test`/rosjava_image_util/img/aria.jpg" ;
