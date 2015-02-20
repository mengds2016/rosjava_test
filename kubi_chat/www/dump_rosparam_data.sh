#!/usr/bin/env bash

BASE64_DATA="";

BASE_DIR=`pwd`/rosparam_data;

if [ ! -e ${BASE_DIR} ];
then
    mkdir $BASE_DIR;
fi

function get_image_from_tag(){
    PARAM_DATA=`rosparam get /robot_bar/demo/$1/icon`;
    if [ "$PARAM_DATA" ];
    then
	for val in ${PARAM_DATA};
	do
	    BASE64_DATA=${BASE64_DATA}${val};
	done
	## echo $BASE64_DATA;
	echo $BASE64_DATA | sed -e "s/'//g" | base64 -d  > ${BASE_DIR}/$1.png;
    else
	PARAM_DATA=`rosparam get /robot_bar/demo/$1/name`;
	if [ ! "${PARAM_DATA}" ];
	then
	    PARAM_DATA="nothing";
	fi
	convert -size 100x100 -font Verdana-Negreta -gravity center label:"${PARAM_DATA}" ${BASE_DIR}/$1.png;
    fi
}

function gen_tag_list(){
    RET="";
    for p in `ls $BASE_DIR`;
    do
	if [ ! "${RET}" ];
	then
	    RET="$p";
	else
	    RET="${RET}_$p"
	fi
    done
    echo $RET;
}

function get_all_image(){
    PARAM_DATA=`rosparam get /robot_bar/demo/tag`;
    if [ "$PARAM_DATA" ];
    then
	for val in ${PARAM_DATA};
	do
	    get_image_from_tag ${val};
	done;
    fi
}

function update_ros_index_html(){
    img_path="";
    tag_name="";
    for p in `ls $BASE_DIR`;
    do
	TAG=`echo $p | sed -e "s/^\(.\+\)\..\+$/\\1/g"`;
	## echo $TAG;
	if [ "$img_path" ];
	then
	    img_path="${img_path},'rosparam_data\/$p'";
	    tag_name="${tag_name},'$TAG'";
	else
	    img_path="['rosparam_data\/$p'";
	    tag_name="['$TAG'";
	fi
    done
    img_path="${img_path}]";
    tag_name="${tag_name}]";
    echo $img_path;
    echo $tag_name;
    ## echo "insert_container('tag','bottom_insert_container',$img_path,$tag_name);";
    sed -i -e "s/insert_container(.\+/insert_container('tag','bottom_insert_container',${img_path},${tag_name});/g" ros-index.html;
}


# rosparam set /robot_bar/demo/tag "standby pour serve"
# rosparam set /robot_bar/demo/standby/name "standby"
# rosparam set /robot_bar/demo/pour/name "pour"
# rosparam set /robot_bar/demo/serve/name "serve"

## get_all_image
## update_ros_index_html