#!/usr/bin/env sh

rostopic pub /sensor_image_node/command/string std_msgs/String "/tmp/test_com.jpg"

