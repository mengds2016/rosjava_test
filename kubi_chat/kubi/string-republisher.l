#!/usr/bin/env roseus

(ros::roseus "string_republisher")

(defun string-republisher
  (msg)
  (ros::publish "/string_republisher/out" msg))

(ros::advertise "/string_republisher/out"
		std_msgs::string)
(ros::subscribe "/string_republisher/in"
		std_msgs::string
		#'string-republisher 1)

(ros::rate 10)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 )
