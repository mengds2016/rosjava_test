#!/usr/bin/env roseus

(ros::roseus "file_path_relay")

(defvar *org_msg*)

(defun relay-callback
  (msg)
  (setq *org_msg* msg))

(ros::subscribe "/in_path"
		std_msgs::String
		#'relay-callback
		10)
(ros::advertise "/out_path1" std_msgs::string)
(ros::advertise "/out_path2" std_msgs::string)
(ros::advertise "/out_path3" std_msgs::string)

(ros::rate 1)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (cond
  (*org_msg*
   (ros::publish "/out_path1" *org_msg*)
   (ros::publish "/out_path2" *org_msg*)
   (ros::publish "/out_path3" *org_msg*)))
 (ros::sleep)
 )

