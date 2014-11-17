#!/usr/bin/env roseus

(ros::roseus "audio_reverse_filtere")

(ros::load-ros-manifest "audio_common_msgs")

(defvar *prev-data*)

(defun audio-callback
  (msg)
  ;; (print msg))
  (cond
   (*prev-data*
    (ros::publish "/audio_out"
		  (instance audio_common_msgs::audiodata :init
			    :data
			    (map string
				 #'(lambda (a b) (min 255 (+ a b)))
				 (send msg :data)
				 *prev-data*)))
    ))
  (setq *prev-data* (send msg :data))
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
