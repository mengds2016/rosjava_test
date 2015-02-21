#!/usr/bin/env roseus

(require "package://aria_model/euslisp/aria-interface.l")

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "qumarion2aria")

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

(objects (list *robot*))
(send *irtviewer* :resize 200 200)

(defvar *recording?* nil)
(defvar *record-av-buf* nil)
(defvar *max-record-time* 30)
(defvar *record-title* nil)

(defvar *motion-root-dir*
  (format nil "~A/kubi_chat/kubi/motion" (ros::rospack-find "rosjava_test")))

(defun joint-state-callback
  (msg)
  (let ((names (send msg :name))
        (positions (send msg :position)))
    (map cons
	 #'(lambda (nm pos)
	     (let* ((l (find-if #'(lambda (j) (string-equal (format nil "~A" (send j :name)) nm))
				(cdr (send *robot* :links)))))
	       (if l (send (send l :joint) :joint-angle (rad2deg pos)))))
	 names positions)))

(defun record-command-callback
  (msg)
  (let* ((head "motion-record-")
	 (str (send msg :data)) collon-pos)
    (if (and str
	     (> (length str) (length head)))
	(setq collon-pos (position #\: str)))
    (if (> collon-pos 0)
	(let* ((on-off (subseq str (length head) collon-pos))
	       (on-off-str (subseq str (length head) collon-pos))
	       (body-str (subseq str (+ 1 collon-pos) (length str))))
	  (format t "~A : ~A~%" on-off-str body-str)
	  (cond
	   ((string-equal on-off-str "on")
	    (send *irtviewer* :title "RECORDING")
	    (send *irtviewer* :change-background #F(1.0 0.9 0.9))
	    (setq *recording?* *max-record-time*)
	    (setq *record-av-buf* nil)
	    (setq *record-title* body-str)
	    )
	   ((string-equal on-off-str "off")
	    (register-motion)
	    )
	   (t ))
	  ))))

(defun gen-tag-from-string
  (str
   &key
   (ret ""))
  (reduce
   '(lambda (a b) (format nil "~A~A" a b))
   (mapcar '(lambda (c) (format nil "~X" c)) (coerce str cons)))
  )

(defun register-motion
  nil
  (let* ((tag (gen-tag-from-string *record-title*))
	 (out (open (format nil "~A/~A.dat" *motion-root-dir* tag) :direction :output)))
    (mapcar
     #'(lambda (val)
	 (format out "~A~%" val))
     (reverse *record-av-buf*))
    (close out)
    (send *irtviewer* :title "NOP")
    (send *irtviewer* :change-background #F(1.0 1.0 1.0))
    (if *record-av-buf*
	(send *robot* :angle-vector
	      (nth (/ (length *record-av-buf*) 2) *record-av-buf*)))
    (send *viewer* :draw-objects)
    (rosparam-register-tag tag *record-title*)
    (setq *record-av-buf* nil)
    (setq *recording?* nil)
    ))

(defun rosparam-register-tag
  (tag name)
  (let* ((tags (ros::get-param "/robot_bar/motion/tag")))
    (cond
     ((or (not (stringp tags))
	  (not (substringp tag tags)))
      (ros::set-param "/robot_bar/motion/tag"
		      (if tags (format nil "~A ~A" tags tag)
			tag))
      ))
    (ros::set-param (format nil "/robot_bar/motion/~A/name" tag) name)
    (send *viewer* :viewsurface :write-to-image-file "/tmp/tmp.png")
    (unix:system
     (format nil
	     "base64 /tmp/tmp.png > /tmp/tmp.dat; rosparam set /robot_bar/motion/~A/icon \"`cat /tmp/tmp.dat`\"" tag))
    ))

(defun play-tagged-motion
  (msg)
  (let* ((tag (send msg :data))
	 (in (if (probe-file (format nil "~A/~A.dat" *motion-root-dir* tag))
		 (open (format nil "~A/~A.dat" *motion-root-dir* tag)
		       :direction :input)))
	 (data) buf)
    (cond
     ((null in) (format t "[play-tagged-motion] invalid input file = ~A~%"
			(format nil "~A/~A.dat" *motion-root-dir* tag)))
     (t
      (while (setq buf (read-line in nil))
	(push (read-from-string buf) data))
      (format t "[play-tagged-motion] ~A~%" data)
      (mapcar
       #'(lambda (av)
	   (send *robot* :angle-vector av)
	   (send *viewer* :draw-objects)
	   ;; (model2real :sleep-time 1000)
	   (send *ri*
		 (if (eq *robot-type* :aria) :_angle-vector :angle-vector)
		 (send *robot* :angle-vector) 1000)
	   (unix:sleep 1))
       (reverse data))
      (close in)
      ))))

(ros::subscribe
 (if (eq *robot-type* :aria)
     "/aria_joint_command/joint_state_fix_leg"
   "/pepper_joint_command/joint_state_fix_leg")
 sensor_msgs::JointState
 #'joint-state-callback 10)

(ros::subscribe
 "/aria/commandline/euslisp"
 std_msgs::string
 #'play-tagged-motion 1)

(ros::subscribe
 "/ros_chat/robot_bar_node/status/string"
 std_msgs::string
 #'record-command-callback 1)

(ros::advertise "/aira_record_viewer/image/in/raw"
                sensor_msgs::image 1)

(ros::rate 5)
(let* ((cnt 10))
  (do-until-key
   (if (not (ros::ok)) (return-from nil nil))
   (ros::sleep)
   (ros::spin-once)
   (x::window-main-one)
   (send *viewer* :draw-objects :flush nil)
   (send *viewer* :viewsurface :flush)
   (print cnt)
   (let* ((img (send *viewer* :viewsurface :getglimage)))
     (ros::publish
      "/aira_record_viewer/image/in/raw"
      (instance sensor_msgs::image :init
		:width (send img :width)
		:height (send img :height)
		:encoding "bgr8"
		:step (* (send img :width) 3)
		:data (send img :entity))))
   (cond
    ((minusp (decf cnt))
     (setq cnt 5)
     ;; (if *record-av-buf*
     ;; (push (send *robot* :angle-vector) *record-av-buf*))
     (cond
      ((and (numberp *recording?*)
	    (< *recording?* 0))
       (register-motion))
      ((numberp *recording?*)
       (decf *recording?*)
       (push (copy-seq (send *robot* :angle-vector)) *record-av-buf*)))))
   ))
