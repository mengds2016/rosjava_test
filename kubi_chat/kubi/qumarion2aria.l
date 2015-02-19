#!/usr/bin/env roseus

(require "package://aria_model/euslisp/aria-interface.l")

;;(require "euslib/drivers/qumarion/Win32/qumarion.l")
;;(defvar *robot* (hrp2jsknt-simple-detail))
(aria-init)
(ros::unsubscribe "/currentor_socket/sensor_array/position")
(defvar *init* :main)

(defvar *joint-state-hash* (make-hash-table))
(defvar *pose-matrix* (unit-matrix))

(defun joint-state-callback
  (msg)
  (let ((names (send msg :name))
        (positions (send msg :position)))
    (map cons
	 #'(lambda (nm pos)
	     (if (not (and (stringp nm)
			   (> (length nm) 0)))
		 (format t "invalid link name=~A~%" nm)
	       (let ((sym (read-from-string nm)))
		 (send *joint-state-hash* :enter sym pos))))
	 names positions)))

(defun pose-callback
  (msg)
  (setq *pose-matrix*
	(quaternion2matrix
	 (float-vector
	  (send (send msg :pose) :orientation :w)
	  (send (send msg :pose) :orientation :x)
	  (send (send msg :pose) :orientation :y)
	  (send (send msg :pose) :orientation :z)))))

(defun qumarion2aria
  (&optional (robot *robot*)
	     (map *joint-state-hash*)
	     (pose *pose-matrix*))
  ;; (send (car (send robot :links))
  ;; 	:newcoords
  ;; 	(make-coords
  ;; 	 :rpy
  ;; 	 (let ((rpy (matrix-log pose)))
  ;; 	   (list 0
  ;; 		 (+ (deg2rad -45) (* -1 (aref rpy 0)))
  ;; 		 (+ (deg2rad 0) (* -1 (aref rpy 2)))))))
  ;; (send robot :newcoords (make-coords))
  (send robot :head :neck-p :joint-angle
	(or (* -1 (send map :get 'head0))
	    (send robot :head :neck-p :joint-angle)))
  (send robot :head :neck-y :joint-angle
	(or  (send map :get 'head1) (send robot :head :neck-y :joint-angle)))
  (send robot :head :neck-r :joint-angle
	(or (send map :get 'neck0) (send robot :head :neck-r :joint-angle)))
  (send robot :rarm :shoulder-p :joint-angle
	(or (send map :get 'r_shoulder0) (send robot :rarm :shoulder-p :joint-angle)))
  (send robot :larm :shoulder-p :joint-angle
	(or (send map :get 'l_shoulder0) (send robot :larm :shoulder-p :joint-angle)))
  (send robot :rarm :shoulder-r :joint-angle
	(or (+ -90 (send map :get 'r_shoulder1))
	    (send robot :rarm :shoulder-r :joint-angle)))
  (send robot :larm :shoulder-r :joint-angle
	(or (+ 90 (send map :get 'l_shoulder1))
	    (send robot :larm :shoulder-r :joint-angle)))
  (send robot :arms :shoulder-y :joint-angle 0)
  (send robot :rarm :elbow-y :joint-angle
	(or (* -1 (send map :get 'r_shoulder2))
	    (send robot :rarm :elbow-y :joint-angle)))
  (send robot :larm :elbow-y :joint-angle
	(or (* -1 (send map :get 'l_shoulder2))
	    (send robot :larm :elbow-y :joint-angle)))
  (send robot :rarm :elbow-p :joint-angle
	(or (* +1 (send map :get 'r_elbow0))
	    (send robot :rarm :elbow-p :joint-angle)))
  (send robot :larm :elbow-p :joint-angle
	(or (* -1 (send map :get 'l_elbow0))
	    (send robot :larm :elbow-p :joint-angle)))
  (send robot :rarm :wrist-y :joint-angle
	(or (- 0 (send map :get 'r_hand0))
	    (send robot :rarm :wrist-y :joint-angle)))
  (send robot :larm :wrist-y :joint-angle
	(or (- 0 (send map :get 'l_hand0))
	    (send robot :larm :wrist-y :joint-angle)))
  (send robot :rarm :wrist-p :joint-angle
	(or (- 0 (send map :get 'r_hand1))
	    (send robot :rarm :wrist-p :joint-angle)))
  (send robot :larm :wrist-p :joint-angle
	(or (- 0 (send map :get 'l_hand1))
	    (send robot :larm :wrist-p :joint-angle)))
  (send robot :torso :waist-p :joint-angle
	(or (send map :get 'waist_h0) (send robot :torso :waist-p :joint-angle)))
  (send robot :torso :waist-y :joint-angle
	(or (send map :get 'waist_v0) (send robot :torso :waist-y :joint-angle)))
  (send robot :torso :waist-y :joint-angle
	(or (send map :get 'waist_h1) (send robot :torso :waist-y :joint-angle)))
  (send robot :torso :waist-r :joint-angle
	(or (send map :get 'waist_v1) (send robot :torso :waist-r :joint-angle)))
  )

(defun publish-now-joint-state
  nil
  (ros::publish
   "/aria_joint_command/joint_state_fix_leg"
   (instance sensor_msgs::jointstate
	     :init
	     :name
	     (mapcar
	      #'(lambda (l) (format nil "~A" (send l :name)))
	      (cdr (send *robot* :links)))
	     :position
	     (mapcar
	      #'(lambda (l) (deg2rad (send (send l :joint) :joint-angle)))
	      (cdr (send *robot* :links))))))

(defun main-loop
  nil
  (do-until-key
   (if (not (ros::ok)) (return-from nil nil))
   (ros::spin-once)
   (qumarion2aria)
   (send *viewer* :draw-objects)
   (publish-now-joint-state)
   (print 'running)
   (cond
    ((and (boundp '*viewer*) *viewer*)
     (x::window-main-one)
     (ros::sleep)))
   ;; (model2real :sleep-time 1000)
   ;; (unix:sleep 1)
   ))

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "qumarion2aria")

(ros::subscribe
 "/qumarion_stream_interface/joint_state"
 sensor_msgs::JointState
 #'joint-state-callback 1)

(ros::subscribe
 "/qumarion_stream_interface/pose_stamped"
 geometry_msgs::PoseStamped
 #'pose-callback 1)

(ros::advertise
 "/aria_joint_command/joint_state_fix_leg"
 sensor_msgs::JointState)

(ros::rate 5)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (qumarion2aria)
 (publish-now-joint-state)
 ;; (print 'running)
 (cond
  ((and (boundp '*viewer*) *viewer*)
   (send *viewer* :draw-objects)
   (x::window-main-one)))
 ;; (model2real :sleep-time 1000)
 ;; (unix:sleep 1)
 )