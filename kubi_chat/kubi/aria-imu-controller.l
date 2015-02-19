#!/usr/bin/env roseus

(ros::load-ros-manifest "geometry_msgs")
(ros::load-ros-manifest "sensor_msgs")

(ros::roseus "aria_imu_controller")

(defvar *robot-type* :aria)

(cond
 ((eq *robot-type* :aria)
  (require "package://aria_model/euslisp/aria-interface.l")
  (aria-init)
  (ros::unsubscribe "/kduino/converted_imu"))
 ((eq *robot-type* :pepper)
  (require "package://peppereus/pepper-interface.l")
  (setq *robot* (instance pepper-robot :init))
  (setq *ri* (instance pepper-interface :init))
  (defmethod pepper-robot
    (:reset-manip-pose
     nil
     (send self :angle-vector #f(0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0)))
    )
  )
 )


(require "package://aria_model/euslisp/aria-interface.l")

(aria-init)
(ros::unsubscribe "/kduino/converted_imu")

(send *robot* :head :neck-p :min-angle -30)
(send *robot* :head :neck-p :max-angle +30)
(send *robot* :head :neck-r :min-angle -20)
(send *robot* :head :neck-r :max-angle +20)
(send *robot* :head :neck-y :min-angle -45)
(send *robot* :head :neck-y :max-angle +45)
(send *robot* :rarm :shoulder-r :min-angle -30)
(send *robot* :rarm :shoulder-r :max-angle 0)

(send *robot* :reset-pose)
(send *robot* :newcoords (make-coords))
(defvar *fove-end-coords*
  (send *robot*
	:put
	:fove-end-coords
	(make-cascoords
	 :name :fove-end-coords
	 :coords (make-coords :pos (send *robot* :head :end-coords :worldpos))
	 :parent (send *robot* :head :end-coords :parent))))

(send *robot* :reset-manip-pose)
(send *robot* :newcoords (make-coords))
(defvar *beer-end-coords*
  (send *robot*
	:put
	:beer-end-coords
	(make-cascoords
	 :name :beer-end-coords
	 :coords (make-coords :pos (send *robot* :rarm :end-coords :worldpos))
	 :parent (send *robot* :rarm :end-coords :parent))))

(defvar *beer-zero-coords* (make-coords))
(defvar *beer-zero-coords-calib-flag* t)
(defvar *beer-obj* (make-cylinder 80 200))

(defvar *fove-zero-coords* (make-coords))
(defvar *fove-zero-coords-calib-flag* t)
(defvar *fove-obj* (make-cylinder 80 80))

(defvar *tablet-zero-coords* (make-coords))
(defvar *tablet-zero-coords-calib-flag* t)
(defvar *tablet-obj* (make-cube 30 120 300))

(defvar *control-mode* :user)
(defvar *last-user-command-receive-time* (car (unix:gettimeofday)))

(send *beer-obj* :set-color #F(1 1 0))
(send *fove-obj* :set-color #F(0 0 1))
(send *tablet-obj* :set-color #F(0 1 0))
(objects (list *robot* *beer-obj* *fove-obj* *tablet-obj*))

(defun android-imu-callback ;; for beer
  (msg)
  ;; (format t "[cup-ui-imu-callback]~%")
  (setq *control-mode* :user) ;; usr flag
  (setq *last-user-command-receive-time* (car (unix:gettimeofday)))
  (let* ((qv (float-vector
	      (send (send (send msg :pose) :orientation) :w)
	      (send (send (send msg :pose) :orientation) :x)
	      (send (send (send msg :pose) :orientation) :y)
	      (send (send (send msg :pose) :orientation) :z)))
	 (m (quaternion2matrix qv)))
    ;; (setq m (send (make-coords :rpy (list 0 (norm (coerce (car (rpy-angle m)) float-vector)) 0)) :worldrot))
    (cond
     (*beer-zero-coords-calib-flag*
      (setq *beer-zero-coords-calib-flag* nil)
      (setq *beer-zero-coords*
      	    (make-coords
      	     :rot
      	     (let* ((rpy (matrix-log (send (make-coords :rot (transpose m)) :worldrot)))
		    (v (map float-vector #'* '(-1 1 1) rpy)))
	       (matrix-exponent
		(map float-vector #'(lambda (id) (aref v id)) '(1 0 2)))
	       )))
      ;; (setq *beer-zero-coords*
      ;; (make-coords
      ;; :rpy
      ;; (map cons #'*
      ;; '(-1 -1 1)
      ;; (car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
      ;;(make-coords :rot (transpose m)))
      ))
    (send *beer-obj* :newcoords
	  (make-coords
	   :pos (copy-seq (send *beer-end-coords* :worldpos))
	   :rot
	   (let* ((r
		   (send
		    (send *beer-zero-coords*
			  :transformation
			  (make-coords
			   :rot
			   (let* ((rpy (matrix-log (send (make-coords :rot (transpose m)) :worldrot)))
				  (v (map float-vector #'* '(-1 1 1) rpy)))
			     (matrix-exponent
			      (map float-vector #'(lambda (id) (aref v id)) '(1 0 2)))
			     )))
		    :worldrot)))
	     ;; (matrix-exponent (map float-vector #'* '(1 1 -1) (matrix-log r)))
	     (matrix-exponent (map float-vector #'* '(1 1 -1) (matrix-log r)))
	     )))
    ;; (send *beer-obj* :newcoords
    ;; 	  (make-coords
    ;; 	   :pos (copy-seq (send *beer-end-coords* :worldpos))
    ;; 	   :rot
    ;; 	   (send
    ;; 	    (send *beer-zero-coords*
    ;; 		  :transformation
    ;; 		  (make-coords
    ;; 		   :rpy
    ;; 		   (map cons #'*
    ;; 			'(-1 -1 1)
    ;; 			(car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
    ;; 	    :worldrot)))
    ))

(defun android-imu-callback2 ;; for fove
  (msg)
  ;; (format t "[cup-ui-imu-callback]~%")
  (setq *control-mode* :user) ;; usr flag
  (setq *last-user-command-receive-time* (car (unix:gettimeofday)))
  (let* ((qv (float-vector
	      (send (send (send msg :pose) :orientation) :w)
	      (send (send (send msg :pose) :orientation) :x)
	      (send (send (send msg :pose) :orientation) :y)
	      (send (send (send msg :pose) :orientation) :z)))
	 (m (quaternion2matrix qv)))
    ;; (setq m (send (make-coords :rpy (list 0 (norm (coerce (car (rpy-angle m)) float-vector)) 0)) :worldrot))
    (cond
     (*fove-zero-coords-calib-flag*
      (setq *fove-zero-coords-calib-flag* nil)
      (setq *fove-zero-coords*
      	    (make-coords
      	     :rot
      	     (let* ((rpy (matrix-log (send (make-coords :rot (transpose m)) :worldrot)))
		    (v (map float-vector #'* '(-1 1 1) rpy)))
	       (matrix-exponent
		(map float-vector #'(lambda (id) (aref v id)) '(1 0 2)))
	       )))
      ;;(make-coords :rot (transpose m)))
      ))
    (send *fove-obj* :newcoords
	  (make-coords
	   :pos (copy-seq (send *fove-end-coords* :worldpos))
	   :rot
	   (let* ((r
		   (send
		    (send *fove-zero-coords*
			  :transformation
			  (make-coords
			   :rot
			   (let* ((rpy (matrix-log (send (make-coords :rot (transpose m)) :worldrot)))
				  (v (map float-vector #'* '(-1 1 1) rpy)))
			     (matrix-exponent
			      (map float-vector #'(lambda (id) (aref v id)) '(1 0 2)))
			     )))
		    :worldrot)))
	     ;; (matrix-exponent (map float-vector #'* '(1 1 -1) (matrix-log r)))
	     (matrix-exponent (map float-vector #'* '(1 1 -1) (matrix-log r)))
	     )))
    (send *tablet-obj* :newcoords
	  (send (send *fove-obj* :copy-worldcoords) :translate #F(300 0 0) :local))
    ;; (send *viewer* :draw-objects)
    ))

(defun cup-ui-imu-callback
  (msg)
  (format t "[cup-ui-imu-callback]~%")
  (setq *control-mode* :user) ;; usr flag
  (setq *last-user-command-receive-time* (car (unix:gettimeofday)))
  (let* ((qv (float-vector
	      (send (send msg :orientation) :w)
	      (send (send msg :orientation) :x)
	      (send (send msg :orientation) :y)
	      (send (send msg :orientation) :z)))
	 (m (quaternion2matrix qv)))
    ;; (setq m (send (make-coords :rpy (list 0 (norm (coerce (car (rpy-angle m)) float-vector)) 0)) :worldrot))
    (cond
     (*beer-zero-coords-calib-flag*
      (setq *beer-zero-coords-calib-flag* nil)
      (setq *beer-zero-coords*
	    (make-coords
	     :rpy
	     (map cons #'*
	     	  '(-1 1 1)
	     	  (car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
      ;;(setq *beer-zero-coords* (make-coords))
      ;;(make-coords :rot (transpose m)))
      ))
    (send *beer-obj* :newcoords
	  (make-coords
	   :pos (copy-seq (send *beer-end-coords* :worldpos))
	   :rot
	   (send
	    (send *beer-zero-coords*
		  :transformation
		  (make-coords
		   :rpy
		   (map cons #'*
			'(-1 1 1)
			(car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
	    :worldrot)))
    ;; (send *viewer* :draw-objects)
    ))

(defun fove-imu-callback
  (msg)
  (format t "[fove-imu-callback]~%")
  (setq *control-mode* :user) ;; usr flag
  (setq *last-user-command-receive-time* (car (unix:gettimeofday)))
  (let* ((qv (float-vector
	      (send (send msg :orientation) :w)
	      (send (send msg :orientation) :x)
	      (send (send msg :orientation) :y)
	      (send (send msg :orientation) :z)))
	 (m (quaternion2matrix qv)))
    (cond
     (*fove-zero-coords-calib-flag*
      (setq *fove-zero-coords-calib-flag* nil)
      ;; (setq *fove-zero-coords*
      ;; 	    (make-coords
      ;; 	     :rpy
      ;; 	     (let* ((v (car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot)))))
      ;; 	       (float-vector (* -1 (nth 0 v)) (nth 1 v) (nth 2 v)))))
      (setq *beer-zero-coords* (make-coords))
	     ;; (map cons #'*
	     ;; 	  '(-1 1 1)
	     ;; 	  (car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
      ;;(make-coords :rot (transpose m)))
      ))
    (send *fove-obj* :newcoords
	  (make-coords
	   :pos (copy-seq (send *fove-end-coords* :worldpos))
	   :rot
	   (send
	    (send *fove-zero-coords*
		  :transformation
		  (make-coords
		   :rpy
		   (let* ((v (car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot)))))
		     (float-vector (* 1 (nth 0 v)) (* -1 (nth 2 v)) (nth 1 v)))))
	    :worldrot)))
    ))

(defun calib-callback
  (msg)
  (setq *beer-zero-coords-calib-flag* t)
  (setq *fove-zero-coords-calib-flag* t))

(defun relative-lookat-callback
  (msg)
  (cond
   ((eq *control-mode* :auto)
    (let* ((dx (aref (send msg :data) 0))
	   (dy (aref (send msg :data) 1)))
      (send *robot* :head :neck-y
	    :joint-angle (* -10.0 dx) :relative t)
      (send *robot* :head :neck-p
	    :joint-angle (* -10.0 dy) :relative t)
      (send *fove-obj* :newcoords
	    (send *fove-end-coords* :copy-worldcoords))
      (send *tablet-obj* :newcoords
	    (send (send *fove-obj* :copy-worldcoords)
		  :translate #F(300 0 0) :local))
      ))))

(defun beer-reset-pose nil
  (send *robot* :reset-manip-pose)
  (send-all (send *robot* :head :joint-list) :joint-angle 0)
  (send-all (send *robot* :larm :joint-list) :joint-angle 0)
  (send *robot* :angle-vector))

(defun beer-kanpai-pose nil
  (send *robot* :reset-pose)
  (send *robot* :rarm :shoulder-p :joint-angle 110)
  (send *robot* :angle-vector))

(defun beer-gubigubi-pose nil
  (send *robot* :reset-pose)
  (send *robot* :rarm :shoulder-p :joint-angle 60)
  (send *robot* :rarm :elbow-p :joint-angle -100)
  (send *robot* :rarm :elbow-y :joint-angle 40)
  (send *robot* :angle-vector))

(setq *imu-angle-vector-mapper*
      (list
       (list (cons :name :reset)
	     (cons :coords
		   (progn (beer-reset-pose)
			  (send-all (send *robot* :links) :worldcoords)
			  (send *beer-end-coords* :copy-worldcoords)))
	     (cons :position (copy-seq (send *robot* :angle-vector))))
       ;; (list (cons :name :kanpai)
       ;; (cons :coords
       ;; (progn (beer-kanpai-pose)
       ;; (send-all (send *robot* :links) :worldcoords)
       ;; (send *beer-end-coords* :copy-worldcoords)))
       ;; (cons :position (copy-seq (send *robot* :angle-vector))))
       (list (cons :name :gubigubi)
	     (cons :coords
		   (progn (beer-gubigubi-pose)
			  (send-all (send *robot* :links) :worldcoords)
			  (send *beer-end-coords* :copy-worldcoords)))
	     (cons :position (copy-seq (send *robot* :angle-vector))))))

(defun imu-angle-vector-blend
  (coords
   &key
   (rest-av-list (list (copy-seq (send *robot* :angle-vector))))
   (rest-scale-list (list 3.0)))
  (let* ((dist
	  (mapcar
	   #'(lambda (d)
	       (/ 1.0
		  (max 1e-3
		       (norm (send coords :difference-rotation
				   (cdr (assoc :coords d)))))))
	   *imu-angle-vector-mapper*))
	 (avl
	  (append
	   rest-av-list
	   (mapcar '(lambda (d) (cdr (assoc :position d)))
		   *imu-angle-vector-mapper*)))
	 (sum (apply '+ dist))
	 (sum2 (apply '+ (cons 1.0 rest-scale-list)))
	 (scale
	  (append
	   (mapcar #'(lambda (d) (/ d sum2)) rest-scale-list)
	   (mapcar #'(lambda (d) (/ d (* sum2 sum))) dist)))
	 )
    (send *robot* :angle-vector
	  (reduce 'v+
		  (mapcar
		   #'(lambda (s av) (scale s av))
		   scale avl)))))

(ros::subscribe "/ros_chat/poseStamped"
		geometry_msgs::PoseStamped
		#'android-imu-callback2)
(ros::subscribe "/cup_ui/poseStamped"
		geometry_msgs::PoseStamped
		#'android-imu-callback)
(ros::subscribe "/fove/kduino/converted_imu"
		sensor_msgs::imu
		#'fove-imu-callback)
(ros::subscribe "/cup_ui/kduino/converted_imu"
		sensor_msgs::imu
		#'cup-ui-imu-callback)
(ros::subscribe "/aria_beer/request/calib"
		std_msgs::empty
		#'calib-callback)
(ros::subscribe "/look_face_node/lookpos_vector/relative"
		std_msgs::float32multiarray
		#'relative-lookat-callback)

(ros::advertise "/aria_imu_controller/angle_vector"
		std_msgs::float32multiarray)
(ros::advertise "/aria_imu_controller/beer_tilt"
		std_msgs::float32)
(ros::advertise "/aria_imu_controller/pan_tilt_vector"
		std_msgs::float32multiarray)
(ros::advertise "/aria_imu_controller/control_mode"
		std_msgs::string)

(ros::rate 10)
(do-until-key
 (print (car (unix::gettimeofday)))
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 ;;
 (if (> (- (car (unix:gettimeofday))
	   *last-user-command-receive-time*)
	10)
     (setq *control-mode* :auto) (setq *control-mode* :user))
 ;;
 (imu-angle-vector-blend (send *beer-obj* :copy-worldcoords))
 ;;
 (send-all (send *robot* :links) :worldcoords)
 (send *robot* :inverse-kinematics
       (list (send *fove-obj* :copy-worldcoords)
	     (send *beer-obj* :copy-worldcoords))
       :move-target
       (list *fove-end-coords* *beer-end-coords*)
       :link-list
       (list
	(send *robot* :link-list (send *fove-end-coords* :parent)
	      (send *robot* :head :root-link))
	(send *robot* :link-list (send *beer-end-coords* :parent)
	      (send *robot* :rarm :root-link)))
       :collision-avoidance-link-pair
       (apply
	#'append
	(mapcar
	 #'(lambda (hl)
	     (mapcar
	      #'(lambda (bl) (list hl bl))
	      (cdr (send *robot* :torso :links))))
	 (send *robot* :rarm :links)))
       :avoid-collision-distance 100
       :avoid-collision-null-gain 3.
       :avoid-collision-joint-gain 3.
       :rotation-axis (list t t)
       :translation-axis (list nil nil)
       :stop 5
       :revert-if-fail nil
       :debug-view nil)
 (send *robot* :torso :waist-p :joint-angle 0)
 (send *robot* :torso :waist-r :joint-angle 0)
 (send *robot* :torso :waist-y :joint-angle 0)
 (send-all (send *robot* :larm :joint-list) :joint-angle 0)
 ;;
 (send *beer-obj* :newcoords
       (send *beer-end-coords* :copy-worldcoords))
 (send *fove-obj* :newcoords
       (send *fove-end-coords* :copy-worldcoords))
 ;;
 (ros::publish "/aria_imu_controller/angle_vector"
	       (instance std_msgs::float32multiarray :init
			 :data (copy-seq (send *robot* :angle-vector))))
 (ros::publish "/aria_imu_controller/beer_tilt"
	       (instance std_msgs::float32 :init
			 :data  (cadr (car (rpy-angle (send *beer-obj* :worldrot))))))
 (ros::publish
  "/aria_imu_controller/pan_tilt_vector"
  (instance std_msgs::float32multiarray :init
	    :data
	    (float-vector
	     (rad2deg (aref (matrix-log (send *fove-obj* :worldrot)) 2))
	     (-
	      90
	      (rad2deg (aref (matrix-log (send *fove-obj* :worldrot)) 1)))
	     )))
 (ros::publish
  "/aria_imu_controller/control_mode"
  (instance std_msgs::string :init :data (format nil "~A" *control-mode*)))
 ;;
 (send *viewer* :draw-objects)
 (x::window-main-one)
 )
