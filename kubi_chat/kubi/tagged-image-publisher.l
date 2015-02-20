#!/usr/bin/env roseus

(ros::roseus "tagged_image_publisher")

(defvar *tag-map*
  (list (cons "my4" "beats_me")
	(cons "my3" "guruguru")
	(cons "my2" "high_five")
	(cons "fuza2" "nonono")
	(cons "my1" "pad_my_back")
	(cons "fuza1" "donbiki")))

(defvar *root-dir* (format nil "~A/kubi_chat/kubi" (ros::rospack-find "rosjava_test")))

(defun check-motion-tag
  (tag)
  (find-if
   #'(lambda (d)
       (string-equal d (format nil "~A.dat" tag)))
   (directory (format nil "~A/motion" *root-dir*))))

(defun gather-tagged-demo
  (tag
   &key
   (demo-head "/robot_bar/demo")
   (motion-head "/robot_bar/motion")
   (sound-head "/robot_bar/sound")
   )
  (let* ((motion-tag (ros::get-param (format nil "~A/~A/motion" demo-head tag)))
	 (sound-tag (ros::get-param (format nil "~A/~A/sound" demo-head tag)))
	 (sound-text (ros::get-param (format nil "~A/~A/text" sound-head sound-tag)))
	 )
    (format t "[gather-tagged-demo] ~A~%  motion: ~A~%  sound: ~A~%"
	    tag motion-tag sound-text)
    (if motion-tag
	(if (check-motion-tag motion-tag)
	    (ros::publish "/aria/commandline/euslisp"
			  (instance std_msgs::String :init
				    :data motion-tag))
	  (ros::publish "/aria/commandline"
			(instance std_msgs::String :init
				  :data motion-tag)))
      (ros::publish "/aria/commandline"
		    (instance std_msgs::String :init
			      :data tag)))
    (if sound-text
	(ros::publish "/voice_echo/mei"
		      (instance std_msgs::String :init
				:data sound-text)))
    ))

(defun status-callback
  (msg)
  (format t "[status-callback] ~A~%" (send msg :data))
  (let* ((str (send msg :data)) tag val)
    (cond
     ((substringp "toggle:" str) 'nop)
     ((and (>= (length str) (length "tag:"))
	   (setq tag (subseq str (length "tag:")))
	   (string-equal "tag:" (subseq str 0 (length "tag:")))
	   (setq val (cdr (find-if #'(lambda (l) (substringp (car l) str))
				   *tag-map*))))
      (format t "[tagged-image] publish ~A~%" val)
      (ros::publish "/aria/commandline"
		    (instance std_msgs::String :init
			      :data val)))
     ((and (substringp "tag:" str) 'nop
	   (and tag (> (length tag) 0)))
      (gather-tagged-demo tag))
     )))
;; (setq str (subseq str (length "tag:")))
;; (cond
;;  ((string-equal str "my4")
;; 	(print 'beats_me)
;; 	(ros::publish "/aria/commandline"
;; 		      (instance std_msgs::String :init
;; 				:data "beats_me")))
;;  ((string-equal str "fuza1")
;; 	(print 'donbiki)
;; 	(ros::publish "/aria/commandline"
;; 		      (instance std_msgs::String :init
;; 				:data "donbiki")))
;;  )))))

(ros::advertise "/aria/commandline"
		std_msgs::String)
(ros::advertise "/voice_echo/mei"
		std_msgs::String)
(ros::advertise "/aria/commandline/euslisp"
		std_msgs::String)
(ros::subscribe "/ros_chat/status/string"
		std_msgs::String
		#'status-callback)

(ros::rate 10)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 )
