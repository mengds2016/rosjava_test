#!/usr/bin/env roseus

(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")

(defvar *robot*)
(defvar *tmp_image_path* "/tmp/test.jpg")

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :draw-objects)
(send *irtviewer* :title "ARIA VIEWER")

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(ros::advertise "/sensor_image_node/command/string"
                std_msgs::string
                1)

(ros::rate 5)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (x::window-main-one)
 (send *viewer* :draw-objects :flush nil)
 (send *viewer* :viewsurface :flush)
 (send *viewer* :viewsurface :write-to-image-file *tmp_image_path*)
 (ros::publish "/sensor_image_node/command/string"
	       (instance std_msgs::string :init
			 :data *tmp_image_path*))
 )

