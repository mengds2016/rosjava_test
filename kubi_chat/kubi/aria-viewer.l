#!/usr/bin/env roseus

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")

(defvar *robot*)

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :title "ARIA VIEWER")
(send *irtviewer* :resize 400 556)
(send *irtviewer* :draw-objects)

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(ros::advertise "/image_window_node/image/in/raw"
                sensor_msgs::image
                1)

(ros::rate 5)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (x::window-main-one)
 (send *viewer* :viewsurface :flush)
 (let* ((img (send *viewer* :viewsurface :getglimage)))
   (ros::publish
    "/image_window_node/image/in/raw"
    (instance sensor_msgs::image :init
	      :width (send img :width)
	      :height (send img :height)
	      :encoding "bgr8" ;;"rgb8"
	      :step (* (send img :width) 3)
	      :data (send img :entity))))
 )
