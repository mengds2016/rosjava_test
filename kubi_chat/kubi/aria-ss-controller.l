#!/usr/bin/env roseus

(ros::load-ros-manifest "geometry_msgs")
(ros::load-ros-manifest "sensor_msgs")

(ros::roseus "aria_ss_controller")

;; (require "aria-imu-controller.l")

(defvar *robot-type* :aria)

(cond
 ((eq *robot-type* :aria)
  (require "package://aria_model/euslisp/aria-interface.l")
  (aria-init :viewer? nil)
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

;;(require "package://aria_model/euslisp/aria-interface.l")
;;(aria-init :viewer? nil)

(defvar *kubi-command-timer* 0)
(defvar *head-only-time* 0)
(defvar *beer-tilt* 0)
(defvar *pan-tilt-vector* (float-vector 0 0))
(defvar *control-mode* :user)
(defvar *sync-imu* nil)

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
    (cons :event-triger ;; kanpai
	  '(lambda (&rest args)
	     (let* ((thre 0.4)
		    (beer-tilt *beer-tilt*))
	       ;; (format t "[kanpai triger] ~A > ~A~%" beer-tilt thre)
	       (> beer-tilt thre)
	       )))
    (cons :event-callback
	  '(lambda (&rest args)
	     (format t "[event-function-logger] kanpai detected~%")
	     (ros::publish "/aria/commandline"
			   (instance std_msgs::string :init
				     :data "cheers"))
	     (unix:sleep 3)
	     ))
    (cons :event-end
	  '(lambda (&rest args)
	     (ros::publish "/aria/commandline"
			   (instance std_msgs::string :init
				     :data "reset"))
	     (unix:sleep 3)
	     t
	     ) ;; hook
	  )
    )
   (list
    (cons :event-triger '(lambda (&rest args) t))
    (cons :event-callback
	  '(lambda (&rest args)
	     ;; kubi
	     (cond
	      ((> (- (car (unix:gettimeofday)) *kubi-command-timer*)  1.5)
	       (ros::publish
		"/kubi_chat/request/pan_tilt_vector"
		(instance std_msgs::float32multiarray :init
			  :data
			  *pan-tilt-vector*))
	       (setq *kubi-command-timer* (car (unix:gettimeofday)))
	       )
	      (t 'nop))
	     ;;
	     (if *sync-imu*
		 (tmp-model2real :sleep-time 1000 :wait? t)
	       (unix:sleep 1))
	     ))
    (cons :event-end '(lambda (&rest args) nil)))
   ))

(defun tmp-model2real
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

(defun aria-imu-angle-vector-callback
  (msg)
  (send *robot* :angle-vector (send msg :data))
  )

(defun pan-tilt-callback
  (msg) (setq *pan-tilt-vector* (send msg :data)))

(defun beer-tilt-callback (msg) (setq *beer-tilt* (send msg :data)))

(defun control-mode-callback (msg) (setq *control-mode* (send msg :data)))

(defun sync-callback
  (msg)
  (cond
   ((substringp "toggle:" (send msg :data))
    (cond
     ((substringp "toggle:o" (send msg :data))
      (setq *sync-imu* t))
     ((substringp "toggle:x" (send msg :data))
      (setq *sync-imu* nil)))
    (cond
     ((eq #\o (aref (send msg :data) (length "toggle:o")))
      (ros::publish "/beer_server/all_on_switch"
		    (instance std_msgs::uint16 :init :data 10))
      )
     ((eq #\x (aref (send msg :data) (length "toggle:o")))
      (ros::publish "/beer_server/all_off_switch"
		    (instance std_msgs::uint16 :init :data 10))
      ))
    )))

(ros::advertise "/beer_server/all_on_switch"
		std_msgs::uint16)
(ros::advertise "/beer_server/all_off_switch"
		std_msgs::uint16)

(ros::subscribe "/aria/commandline"
		std_msgs::string
		#'aria-command-callback)
(ros::subscribe "/aria/commandline/org"
		std_msgs::string
		#'aria-command-org-callback)
(ros::subscribe "/aria_imu_controller/angle_vector"
		std_msgs::float32multiarray
		#'aria-imu-angle-vector-callback)
(ros::subscribe "/aria_imu_controller/pan_tilt_vector"
		std_msgs::float32multiarray
		#'pan-tilt-callback)
(ros::subscribe "/aria_imu_controller/beer_tilt"
		std_msgs::float32
		#'beer-tilt-callback)
(ros::subscribe "/aria_imu_controller/control_mode"
		std_msgs::string
		#'control-mode-callback)
(ros::subscribe "/ros_chat/status/string"
		std_msgs::string
		#'sync-callback)

(ros::advertise "/kubi_chat/request/pan_tilt_vector"
		std_msgs::float32multiarray
		)
(ros::advertise "/aria/commandline"
		std_msgs::String
		)

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
 )
