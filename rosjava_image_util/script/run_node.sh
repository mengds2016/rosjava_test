#!/usr/bin/env bash

NODE=$1;

if [ ! "$NODE" ];
then
    NODE=DumpImageNode;
fi

echo ${@:1};
`rospack find rosjava_test`/rosjava_image_util/build/install/rosjava_image_util/bin/rosjava_image_util com.github.rosjava_test.rosjava_image_util.$NODE ${@:1};
