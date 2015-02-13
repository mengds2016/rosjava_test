#!/usr/bin/env bash

rostopic echo /image_window_node/drop_file/path | while read IMAGE_PATH;
do
    if [ "`echo ${IMAGE_PATH} | grep data\:`" ];
    then
	IMAGE_PATH=`echo ${IMAGE_PATH} | sed -e "s/^data\:\ \(.\+\)$/\\1/g"`;
	if [ -f "${IMAGE_PATH}" ];
	then
	    echo "[image_info_publisher] receive image ${IMAGE_PATH}";
	    IMAGE_WIDTH=`identify -format "%w" $IMAGE_PATH`;
	    IMAGE_HEGHT=`identify -format "%h" $IMAGE_PATH`;
	    rostopic pub -1 /image_info/size_array std_msgs/Int32MultiArray "{data: [${IMAGE_WIDTH}, ${IMAGE_HEGHT}]}";
	fi
    fi
done
