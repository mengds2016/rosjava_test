#!/usr/bin/env roseus

(ros::roseus "audio_reverse_filtere")

(ros::load-ros-manifest "audio_common_msgs")

(defun audio-callback
  (msg)
  ;; (print msg))
  (ros::publish "/audio_out"
		(instance audio_common_msgs::audiodata :init
			  :data (reverse (send msg :data))))
  )

(ros::subscribe "/audio_in"
		audio_common_msgs::audiodata
		#'audio-callback
		10)
(ros::advertise "/audio_out"
		audio_common_msgs::audiodata
		10)

(ros::rate 100)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep))
