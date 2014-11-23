#!/usr/bin/env roseus

(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")

(defvar *robot*)
(defvar *tmp_image_path* "/tmp/test.jpg")

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :title "ARIA VIEWER")
(send *irtviewer* :resize 400 556)
(send *irtviewer* :draw-objects)

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(ros::advertise "/sensor_image_node/command/string"
                std_msgs::string
                1)

(defun draw-normalize-rectangle
  (nvec &key
	(scale (send *irtviewer* :width))
	(h (send *irtviewer* :height))
	(vec (scale scale nvec))
	(flush t))
  (send *viewer* :viewsurface :draw-rectangle
	(integer-vector (round (aref vec 0))
			(max 0 (- (- h (round (aref vec 1))) (round (aref vec 3)))))
	(round (aref vec 2)) (round (aref vec 3)))
  (if flush (send *viewer* :viewsurface :flush)))

(defvar *left-elbow* (float-vector 0 0 0 0))
(defvar *right-elbow* (float-vector 0 0 0 0))
(defvar *left-eye* (float-vector 0 0 0 0))
(defvar *right-eye* (float-vector 0 0 0 0))
(defvar *face* (float-vector 0 0 0 0))

(defun copy-rect-callback (vec msg)
  (dotimes (i (min (length (send msg :data)) (length vec)))
    (setf (aref vec i) (aref (send msg :data) i))))

(defun window-size-callback (msg &key (flush nil))
  (if (>= (length (send msg :data)) 2)
      (send *irtviewer* :resize (aref (send msg :data) 0)
	    (aref (send msg :data) 1)))
  (if flush (send *viewer* :draw-objects)))

(ros::subscribe "/aria_viewer/window_size"
		std_msgs::int32multiarray
		#'window-size-callback)

(ros::subscribe "/image_window_node/left_elbow/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-elbow*)
(ros::subscribe "/image_window_node/right_elbow/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-elbow*)
(ros::subscribe "/eus_animeface/eyes/left/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-eye*)
(ros::subscribe "/eus_animeface/eyes/right/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-eye*)
(ros::subscribe "/eus_animeface/face/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *face*)

(ros::rate 5)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (x::window-main-one)
 (send *viewer* :draw-objects :flush nil)
 ;;
 (send *viewer* :viewsurface :color #f(1 0 0))
 (draw-normalize-rectangle *face* :flush nil)
 (send *viewer* :viewsurface :color #f(0 1 0))
 (draw-normalize-rectangle *right-eye* :flush nil)
 (draw-normalize-rectangle *left-eye* :flush nil)
 (send *viewer* :viewsurface :color #f(0 0 1))
 (draw-normalize-rectangle *right-elbow* :flush nil)
 (draw-normalize-rectangle *left-elbow* :flush nil)
 ;;
 (send *viewer* :viewsurface :flush)
 (send *viewer* :viewsurface :write-to-image-file *tmp_image_path*)
 (ros::publish "/sensor_image_node/command/string"
	       (instance std_msgs::string :init
			 :data *tmp_image_path*))
 )

