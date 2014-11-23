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

(send *viewer* :adjust-viewport
      (send *viewer* :viewsurface :width)
      (send *viewer* :viewsurface :height))

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(ros::advertise "/sensor_image_node/command/string"
                std_msgs::string
                1)

(defun calc-eye-position
  nil
  (let* ((reye (send *robot* :get-val 'eyer_end_link_lk))
	 (leye (send *robot* :get-val 'eyel_end_link_lk))
	 (init (send-all (list reye leye) :worldcoords))
	 (reye-faces (flatten (send-all (send reye :bodies) :faces)))
	 (leye-faces (flatten (send-all (send leye :bodies) :faces)))
	 (reye-vertices (flatten (send-all reye-faces :vertices)))
	 (leye-vertices (flatten (send-all leye-faces :vertices)))
	 )
    ;;(send-all (list reye leye) :draw-on :flush t :color #F(0 1 0))
    ;;(send-all
    (list
     (scale (/ 1.0 (length reye-vertices)) (reduce #'v+ reye-vertices))
     (scale (/ 1.0 (length leye-vertices)) (reduce #'v+ leye-vertices)))
    ;;:draw-on :flush t :color #F(1 0 0))
    ))

(defun point-to-screen
  (v
   &key
   (eye (send *viewer* :get-val 'geometry::eye))
   (vc (send eye :get-val 'geometry::viewcoords))
   (pm (send eye :projection))
   (ir (send *viewer* :viewport :worldrot))
   (ip (send *viewer* :viewport :worldpos))
   ;;
   (vv (v+ (transform (send vc :worldrot) v)
	   (send vc :worldpos)))
   (vv1 (geo::homogenize vv))
   (vp (transform pm vv1))
   (nvp (float-vector (/ (aref vp 0) (aref vp 3))
		      (/ (aref vp 1) (aref vp 3))
		      (/ (aref vp 2) (aref vp 3))))
   (nvp_ (v+ (transform ir nvp) ip))
   )
  ;;(format t "~A ~A ~A ~A~%" vv vv1 vp nvp)
  nvp_
  )

(defun screen-to-point
  (nvp_
   &key
   (gain 0.0)
   (eye (send *viewer* :get-val 'geometry::eye))
   (null-dir (send eye :view-direction))
   (vc (send eye :get-val 'geometry::viewcoords))
   (pm (send eye :projection))
   (ir (send *viewer* :viewport :worldrot))
   (ip (send *viewer* :viewport :worldpos))
   ;;
   (nvp (transform (pseudo-inverse ir) (v- nvp_ ip)))
   (vp (concatenate float-vector nvp '(1)))
   (vv1 (transform (pseudo-inverse pm) vp))
   (vv (float-vector (/ (aref vv1 0) (aref vv1 3))
		     (/ (aref vv1 1) (aref vv1 3))
		     (/ (aref vv1 2) (aref vv1 3))))
   (v (transform (pseudo-inverse (send vc :worldrot))
		 (v- vv (send vc :worldpos))))
   ;; (ans (send *viewer* :point-to-screen v))
   )
  ;; (format t "~A ~A ~A ~A~%" vv vv1 vp nvp)
  (v+ v (scale gain null-dir))
  )

(defun draw-normalize-rectangle
  (nvec &key
	(scale (send *irtviewer* :width))
	(h (send *irtviewer* :height))
	(vec (scale scale nvec))
	(flush t))
  (send *viewer* :viewsurface :draw-rectangle
	(integer-vector (round (aref vec 0))
			(max 0 (- (- h (round (aref vec 1))) (round (aref vec 3)))))
	(max 5 (round (aref vec 2))) (max 5 (round (aref vec 3))))
  (if flush (send *viewer* :viewsurface :flush)))

(defun draw-shoulder-line
  (&key
   (shoulder-pos-list
    (send *robot* :arms :shoulder-p :child-link :worldpos))
   (shoulder-pos-screen-list
    (mapcar
     #'(lambda (pos)
	 (let ((spos (point-to-screen pos)))
	   ;;(send *viewer* :point-to-screen pos)))
	   ;;(send *viewer* :viewport :ndc-point-to-screen
	   ;;(scale (/ 1.0 (aref pos 2)) pos))))
	   (integer-vector (round (aref spos 0))
			   (round (aref spos 1)))))
     shoulder-pos-list))
   (shoulder-distance (apply #'v- shoulder-pos-screen-list))
   (flush t))
  (print shoulder-pos-list)
  (print shoulder-pos-screen-list)
  (send *viewer* :viewsurface :draw-line
	(car shoulder-pos-screen-list) (cadr shoulder-pos-screen-list))
  (if flush (send *viewer* :viewsurface :flush)))

(defvar *left-elbow* (float-vector 0 0 0 0))
(defvar *right-elbow* (float-vector 0 0 0 0))
(defvar *left-eye* (float-vector 0 0 0 0))
(defvar *right-eye* (float-vector 0 0 0 0))
(defvar *face* (float-vector 0 0 0 0))
(defvar *chin* (float-vector 0 0 0 0))

(defun copy-rect-callback (vec msg)
  (dotimes (i (min (length (send msg :data)) (length vec)))
    (setf (aref vec i) (aref (send msg :data) i))))

(defun window-size-callback (msg &key (flush nil))
  (if (>= (length (send msg :data)) 2)
      (send *irtviewer* :resize (aref (send msg :data) 0)
	    (aref (send msg :data) 1)))
  (send *viewer* :adjust-viewport
	(send *viewer* :viewsurface :width)
	(send *viewer* :viewsurface :height))
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
(ros::subscribe "/eus_animeface/chin/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *chin*)

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
 (draw-normalize-rectangle *chin* :flush nil)
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

#|


(v+ (transform (send *viewer* :viewport :worldrot) v)
    (send *viewer* :viewport :worldpos))
