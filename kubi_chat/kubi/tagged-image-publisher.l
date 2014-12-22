#!/usr/bin/env roseus

(ros::roseus "tagged_image_publisher")

(defvar *tag-map*
  (list (cons "my4" "beats_me")
	(cons "my3" "guruguru")
	(cons "my2" "high_five")
	(cons "fuza2" "nonono")
	(cons "my1" "pad_my_back")
	(cons "fuza1" "donbiki")))

(defun status-callback
  (msg)
  (let* ((str (send msg :data)) val)
    (cond
     ((and (>= (length str) (length "tag:"))
	   (string-equal "tag:" (subseq str 0 (length "tag:")))
	   (setq val (cdr (find-if #'(lambda (l) (substringp (car l) str))
				   *tag-map*))))
      (format t "[tagged-image] publish ~A~%" val)
      (ros::publish "/aria/commandline"
		    (instance std_msgs::String :init
			      :data val))))))
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
(ros::subscribe "/ros_chat/status/string"
		std_msgs::String
		#'status-callback)

(ros::rate 10)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 )
