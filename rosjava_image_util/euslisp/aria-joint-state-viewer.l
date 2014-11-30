#!/usr/bin/env roseus

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")

(defvar *robot*)

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :title "ARIA JOINT_STATE VIEWER")
(send *irtviewer* :draw-objects)

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(defun js-callback
  (msg)
  (map cons
       #'(lambda (name pos)
	   (let* ((j (find-if #'(lambda (j) (string-equal name (send j :name)))
			      (send *robot* :joint-list))))
	     (cond
	      ((null j)
	       (format t "[js-callback] unknow joint name=~A~%" name))
	      (t (send j :joint-angle (rad2deg pos))))))
       (send msg :name)
       (send msg :position)))

(ros::subscribe "/aria_viewer/joint_states"
                sensor_msgs::jointstate
		#'js-callback
                1)

(ros::rate 10)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (send *viewer* :draw-objects)
 (x::window-main-one)
 )
