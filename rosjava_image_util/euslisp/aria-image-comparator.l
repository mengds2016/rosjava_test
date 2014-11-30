#!/usr/bin/env roseus

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")
(require "screen-view-util.l")

(defvar *robot*)

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :title "ARIA IMAEG COMP")

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(defvar *ref-width* 100)
(defvar *in_image_path* "aria.jpg")
(defvar *comp-image*)

(defun set-pixel
  (image x y color) ;; color is BGR vector
  (let* ( (col (concatenate cons color))
          (depth (if (find-method image :depth)
                     (/ (send image :depth) 8)
                   1))
          (x (* x depth)) )
    (labels ((itter
              (off col-list)
              (unless (null col-list)
                (progn
                  (send image :set-pixel (+ x off) y (round (car col-list)))
                  (itter (+ 1 off) (cdr col-list))))))
      (itter 0 col))))

(defun get-pixel
 (image x y)
 (let ( (depth (if (find-method image :depth)
                   (/ (send image :depth) 8)
                 1))
        (pixel (send image :pixel x y)) )
   (labels ( (itter (cnt)
                    (if (minusp cnt)
                        nil
                      (cons (logand (ash pixel (* cnt -8)) #xff)
                            (itter (- cnt 1))))) )
     (itter (- depth 1)))))

(defun map-image
  (image func)
  (dotimes (x (send image :width))
    (dotimes (y (send image :height))
      (funcall func image x y))))

(defun resize-image
  (image width height)
  (let ((ret (instance (class image) :init (round width) (round height)))
        (w-step (/ (send image :width) (* 1.0 width)))
        (h-step (/ (send image :height) (* 1.0 height))))
    (map-image ret
               #'(lambda (ret x y)
                   (set-pixel
                    ret x y
                    (get-pixel
                     image
                     (floor (* x w-step))
                     (floor (* y h-step))))))
    ret))

(defun comp-target-image-callback
  (msg)
  (setq *in_image_path* (send msg :data))
  (let* ((img (img::read-image-file *in_image_path*)))
    (cond
     ((and img (subclassp (class img) color-image24))
      (setq *comp-image* img)
      (send *irtviewer* :resize
	    *ref-width*
	    (round (* *ref-width* (/ (* 1.0 (send *comp-image* :height))
				     (send *comp-image* :width)))))
      (ros::publish
       "/aria_viewer/window_size"
       (instance std_msgs::int32multiarray :init
		 :data (integer-vector (send *viewer* :viewsurface :width)
				       (send *viewer* :viewsurface :height))))
      (setq *comp-image* (resize-image *comp-image*
				       (send *viewer* :viewsurface :width)
				       (send *viewer* :viewsurface :height)))
      ))))

(comp-target-image-callback
 (instance std_msgs::string :init :data *in_image_path*))

(defun compare-with-glview
  (img
   &key
   (glimage (send *viewer* :viewsurface :getglimage))
   (width (send img :width))
   (height (send img :height))
   (skip-func #'(lambda (val) nil))
   (calc-func #'(lambda (val1 val2)
		  (let* ((val12 (+ (- #xFF (ash (logand val1 #xFF0000) -16))
				   (- #xFF (ash (logand val1 #x00FF00) -8))
				   (- #xFF (logand val1 #x0000FF))))
			 (val22 (+ (- #xFF (ash (logand val2 #xFF0000) -16))
				   (- #xFF (ash (logand val2 #x00FF00) -8))
				   (- #xFF (logand val2 #x0000FF)))))
		    (* 1e-6 val12 val22))))
   (ret 0))
  (dotimes (x width)
    (dotimes (y height)
      (if (not (funcall skip-func (send glimage :pixel x y)))
	  (setq ret (+ ret (funcall calc-func
				    (send glimage :pixel x y)
				    (send img :pixel x y)))))))
  ret)

(ros::subscribe "/aria_image_comparator/in/path"
                std_msgs::string
		#'comp-target-image-callback
                1)
(ros::advertise "/aria_viewer/window_size"
		std_msgs::int32multiarray
		)
(ros::advertise "/image_window_node/image/in/raw"
                sensor_msgs::image
                1)

(ros::rate 5)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (send *viewer* :draw-objects)
 (x::window-main-one)
 (let* ((img (send *viewer* :viewsurface :getglimage)))
   (ros::publish
    "/image_window_node/image/in/raw"
    (instance sensor_msgs::image :init
	      :width (send img :width)
	      :height (send img :height)
	      :encoding "rgb8"
	      :data (send img :entity)))
   (if *comp-image* (print (compare-with-glview *comp-image* :glimage img))))
 )

#|


(v+ (transform (send *viewer* :viewport :worldrot) v)
    (send *viewer* :viewport :worldpos))

(let* ((img (send *viewer* :viewsurface :getglimage)))
  (ros::publish
   "/image_window_node/image/in/raw"
   (instance sensor_msgs::image :init
	     :width (send img :width)
	     :height (send img :height)
	     :encoding "rgb8"
	     :data (send img :entity))))
