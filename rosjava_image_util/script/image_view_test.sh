#!/usr/bin/env sh

rosrun image_view image_view image:=/sensor_image_node/image/out _image_transport:=compressed
