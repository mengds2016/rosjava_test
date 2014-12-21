#!/usr/bin/env roseus

(ros::roseus "tagged_image_publisher")

(defun status-callback
  (msg)
  (let* ((str (send msg :data)))
    (cond
     ((and (>= (length str) (length "tag:"))
	   (string-equal "tag:" (subseq str 0 (length "tag:"))))
      (setq str (subseq str (length "tag:")))
      (cond
       ((string-equal str "my4")
	(print 'beats_me)
	(ros::publish "/aria/commandline"
		      (instance std_msgs::String :init
				:data "beats_me")))
       ((string-equal str "fuza1")
	(print 'donbiki)
	(ros::publish "/aria/commandline"
		      (instance std_msgs::String :init
				:data "donbiki")))
       )))))

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
