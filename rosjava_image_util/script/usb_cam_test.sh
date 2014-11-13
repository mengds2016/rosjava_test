#!/usr/bin/env sh

rosrun usb_cam usb_cam_node /usb_cam/image_raw/compressed:=/sensor_image_node/image/in/compressed
