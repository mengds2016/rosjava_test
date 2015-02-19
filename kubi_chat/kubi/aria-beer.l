#!/usr/bin/env roseus

(ros::load-ros-manifest "geometry_msgs")
(ros::load-ros-manifest "sensor_msgs")

(ros::roseus "aria_beer")

(defvar *robot-type* :pepper)
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

(defvar *head-only-time* 0)

(defvar *control-mode* :user)
(defvar *last-user-command-receive-time*
  (car (unix:gettimeofday)))

(send *beer-obj* :set-color #F(1 1 0))
(send *fove-obj* :set-color #F(0 0 1))
(send *tablet-obj* :set-color #F(0 1 0))
(objects (list *robot* *beer-obj* *fove-obj* *tablet-obj*))

(defvar *base-beer-pose*
  (float-vector 0.0 -1.45573 0.027471 -61.1792 -0.972318 -0.653704 9.32218 -3.00486 -154.84 -15.8 2.4 -98.4 43.2 -36.0 43.2 6.68738 5.8092 1.85949 -0.543847 0.0 0.0 0.0))

(defvar *kubi-command-timer* 0)

;; (defvar *kanpai-flag* 0)
(defvar *mode-id*)
(setq *event-functions*
  (list
   (list
    (cons :event-triger ;; logger
	  '(lambda (&rest args)
	     (format t "[event-function-logger] mode=~A~%" *control-mode*)
	     nil)))
   (list
    (cons :event-triger
	  '(lambda (&rest args)
	     (let* ((thre 0.4)
		    (beer-tilt (cadr (car (rpy-angle (send *beer-obj* :worldrot))))))
	       ;; (format t "[kanpai triger] ~A > ~A~%" beer-tilt thre)
	       (> beer-tilt thre)
	       )))
    (cons :event-callback
	  '(lambda (&rest args)
	     (ros::publish "/aria/commandline"
			   (instance std_msgs::string :init
				     :data "cheers"))
	     (send *robot* :angle-vector (float-vector 0.0 -1.45573 0.027471 -61.1792 -0.972318 -0.653704 9.32218 -3.00486 -154.84 -15.8 10.251 -30.0 -7.89955 8.59526 80.0 6.68738 5.8092 1.85949 -0.543847 0.0 0.0 0.0))
	     (send *viewer* :draw-objects)
	     ;; (model2real :sleep-time 4000)
	     (unix:sleep 3)
	     ))
    (cons :event-end
	  '(lambda (&rest args)
	     (reset-manip-pose)
	     (send *viewer* :draw-objects)
	     (ros::publish "/aria/commandline"
			   (instance std_msgs::string :init
				     :data "reset"))
	     (unix:sleep 3)
	     ;; (model2real :sleep-time 4000)
	     ;; (unix:sleep 4)
	     t
	     ;; nil
	     ) ;; hook
	  )
    )
   (list
    (cons :event-triger '(lambda (&rest args) t))
    (cons :event-callback
	  '(lambda (&rest args)
	     ;; (send *robot* :angle-vector
	     ;; (v+ (scale 0.5 *base-beer-pose*)
	     ;; (scale 0.5 (send *robot* :angle-vector))))
	     (send-all (send *robot* :links) :worldcoords)
	     (send *robot* :inverse-kinematics
		   (list (send *fove-obj* :copy-worldcoords)
			 (send *beer-obj* :copy-worldcoords))
		   :move-target
		   (list *fove-end-coords* *beer-end-coords*)
		   :link-list
		   (list
		    (send *robot* :link-list (send *fove-end-coords* :parent) (send *robot* :head :root-link))
		    (send *robot* :link-list (send *beer-end-coords* :parent) (send *robot* :rarm :root-link)))
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
	     ;;(send *robot* :head :neck-p :joint-angle 0)
	     ;;(send *robot* :head :neck-r :joint-angle 0)
	     ;;(send *robot* :head :neck-y :joint-angle 0)
	     (send *robot* :torso :waist-p :joint-angle 0)
	     (send *robot* :torso :waist-r :joint-angle 0)
	     (send *robot* :torso :waist-y :joint-angle 0)
	     ;;(send-all (send *robot* :rarm :joint-list) :joint-angle 0)
	     (send-all (send *robot* :larm :joint-list) :joint-angle 0)
	     ;; kubi
	     (cond
	      ((> (- (car (unix:gettimeofday)) *kubi-command-timer*)  1.5)
	       (ros::publish
		"/kubi_chat/request/pan_tilt_vector"
		(instance std_msgs::float32multiarray :init
			  :data
			  (float-vector
			   (rad2deg (aref (matrix-log (send *fove-obj* :worldrot)) 2))
			   (-
			    90
			    (rad2deg (aref (matrix-log (send *fove-obj* :worldrot)) 1)))
			   )))
	       (setq *kubi-command-timer* (car (unix:gettimeofday)))
	       )
	      (t 'nop))
	     ;;
	     (beer-model2real :sleep-time 1000 :wait? t)
	     ;; (unix:sleep 1)
	     ))
    (cons :event-end '(lambda (&rest args) nil)))
   ))

(defun beer-model2real
  (&key (sleep-time 1000) (wait? t)
	(only-head?;; nil)
	 (cond
	  ((> *head-only-time* (car (unix:gettimeofday))) t)
	  (t (setq *head-only-time* 0) nil)))
	(jlist (send *robot* :joint-list)))
  ;; (model2real :sleep-time sleep-time)
  ;; (return-from beer-model2real nil)
  (send *ri*
	(if (eq *robot-type* :aria) :_angle-vector :angle-vector)
	(map float-vector
	     #'(lambda (j v)
		 (cond
		  ((and only-head?
			(not (find j (send *robot* :head :joint-list))))
		   1e+6)
		  (t v)))
	     jlist
	     (send *robot* :angle-vector))
	sleep-time)
  (if wait?
      (unix:usleep (round (* sleep-time 1000)))))

(defun android-imu-callback ;; for beer
  (msg)
  (format t "[cup-ui-imu-callback]~%")
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
      	     :rpy
      	     (map cons #'*
      		  '(-1 -1 1)
      		  (car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
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
			'(-1 -1 1)
			(car (rpy-angle (send (make-coords :rot (transpose m)) :worldrot))))))
	    :worldrot)))
    (send *tablet-obj* :newcoords
	  (send (send *beer-obj* :copy-worldcoords) :translate #F(300 0 0) :local))
    ;; (send *viewer* :draw-objects)
    ;; (cond
    ;;  ((> (print (cadr (car (rpy-angle (send *beer-obj* :worldrot))))) 0.3)
    ;;   (print 'kanpai) (setq *kanpai-flag* 1))
    ;;  ((> *kanpai-flag* 0)
    ;;   (setq *kanpai-flag* 2))
    ;;  (t (setq *kanpai-flag* 0)))
    ))

(defun android-imu-callback2 ;; for fove
  (msg)
  (format t "[cup-ui-imu-callback]~%")
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
	     (matrix-exponent (map float-vector #'* '(-1 -1 -1) (matrix-log r)))
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

(defun aria-command-org-callback
  (msg)
  (setq *head-only-time*
	(+ (car (unix:gettimeofday)) 6))
  (ros::publish "/aria/commandline" msg)
  )

(defun aria-command-callback
  (msg)
  (setq *head-only-time*
	(+ (car (unix:gettimeofday)) 6))
  )

(defun relative-lookat-callback
  (msg)
  (cond
   ((or (eq *control-mode* :auto)
	(> (- (car (unix:gettimeofday))
	      *last-user-command-receive-time*)
	   10))
    (setq *control-mode* :auto)
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
(ros::subscribe "/aria_beer/request/calib"
		std_msgs::empty
		#'calib-callback)
(ros::subscribe "/aria/commandline"
		std_msgs::string
		#'aria-command-callback)
(ros::subscribe "/aria/commandline/org"
		std_msgs::string
		#'aria-command-org-callback)
(ros::subscribe "/look_face_node/lookpos_vector/relative"
		std_msgs::float32multiarray
		#'relative-lookat-callback)
;; (ros::subscribe "/android_pose/poseStamped"
;;		geometry_msgs::PoseStamped
;;		#'android-pose-callback)

(ros::advertise "/kubi_chat/request/pan_tilt_vector"
		std_msgs::float32multiarray
		)
(ros::advertise "/aria/commandline"
		std_msgs::String
		)

(defun reset-manip-pose
  nil
  (send *robot* :angle-vector (float-vector 0.0 -2.21381 -0.17029 -120.0 -0.071416 4.33423 30.0 -0.598775 0.0 0.89541 0.499902 -120.0 0.049437 2.87301 -30.0 2.09296 -0.348825 0.085145 -1.25797 0.0 0.0 0.0)))

(ros::rate 10)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 ;;
 (let* ((id -1))
   (dolist (f *event-functions*)
     (incf id)
     (cond
      ((funcall (cdr (assoc :event-triger f)))
       (cond
	((and *mode-id*
	      (not (eq *mode-id* id))
	      (< *mode-id* (length *event-functions*))
	      (funcall
	       (cdr (assoc :event-end (nth *mode-id* *event-functions*)))))
	 'hook)
	(t
	 (funcall (cdr (assoc :event-callback f)))))
       (setq *mode-id* id)
       ;; (return-from nil nil)
       ))))
 ;;
 (send *beer-obj* :newcoords
       (send *beer-end-coords* :copy-worldcoords))
 (send *fove-obj* :newcoords
       (send *fove-end-coords* :copy-worldcoords))
 ;;
 (send *viewer* :draw-objects)
 (x::window-main-one)
 )
