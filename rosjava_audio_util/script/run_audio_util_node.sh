#!/usr/bin/env sh

NODE=$1;

if [ ! "$NODE" ];
then
    NODE=SpeakerNode;
fi

`rospack find rosjava_test`/rosjava_audio_util/build/install/rosjava_audio_util/bin/rosjava_audio_util com.github.rosjava_test.rosjava_audio_util.$NODE;

