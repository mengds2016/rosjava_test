#!/usr/bin/env roseus

(ros::roseus "audio_reverse_filtere")

(ros::load-ros-manifest "audio_common_msgs")

(defvar *prev-data-queue-size* 10)
(defvar *prev-data-queue* (instantiate vector *prev-data-queue-size*))
(defvar *prev-data-queue-pos* 0)

(defun audio-callback
  (msg)
  ;; (print msg))
  (dotimes (i *prev-data-queue-size*)
    (dotimes (j (length (aref *prev-data-queue* i)))
      (setf (aref (aref *prev-data-queue* i) j)
	    (min 255 (round (* 0.5 (aref (aref *prev-data-queue* i) j)))))))
  (setf (aref *prev-data-queue* *prev-data-queue-pos*) (send msg :data))
  (setq *prev-data-queue-pos*
	(mod (+ *prev-data-queue-pos* 1) *prev-data-queue-size*))
  (ros::publish "/audio_out"
		(instance audio_common_msgs::audiodata :init
			  :data
			  (let* ((str (instantiate integer-vector
						   (length (send msg :data)))))
			    (dotimes (i *prev-data-queue-size*)
			      (dotimes (j (min (length str)
					       (length (aref *prev-data-queue* i))))
				(setf (aref str j)
				      (+ (aref str j)
					 (aref (aref *prev-data-queue* i) j)))))
			    (map string #'(lambda (val) (min 255 val)) str))))
  )

(ros::subscribe "/audio_in"
		audio_common_msgs::audiodata
		#'audio-callback
		10)
(ros::advertise "/audio_out"
		audio_common_msgs::audiodata
		10)

(ros::rate 100)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep))
