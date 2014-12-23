#!/usr/bin/env roseus

(ros::load-ros-manifest "geometry_msgs")
(ros::load-ros-manifest "sensor_msgs")

(ros::roseus "vibration_observer")

(defvar *hz* 10)

(defun velocity-callback
  (msg)
  (if (> (print (norm (send msg :data))) 0.3)
      (ros::publish "/ros_chat/request/vibrate"
		    (instance std_msgs::int64 :init
			      :data (round (/ 1000.0 *hz*)))))
  )

(ros::subscribe "/currentor_socket/sensor_array/velocity"
		std_msgs::float32multiarray
		#'velocity-callback)
(ros::advertise "/ros_chat/request/vibrate"
		std_msgs::int64
		)

(ros::rate *hz*)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 )

