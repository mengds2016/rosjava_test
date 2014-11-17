#!/usr/bin/env roseus

(require "package://eus_fftw/euslisp/eus_fftw.l")

(ros::roseus "audio_speed_filtere")
(ros::load-ros-manifest "audio_common_msgs")

(defvar *amp-data* (instantiate float-vector 800))
(defvar *freq-data* (instantiate float-vector (* 2 (length *amp-data*))))
;;
(defvar *speed-scale* 0.5)
(defvar *amp-scale-data*
  (instantiate float-vector (floor (* *speed-scale* (length *amp-data*)))))
(defvar *amp-data-string* (instantiate string (length *amp-scale-data*)))

(require "fourie_filter.l")

(defun audio-callback
  (msg)
  (setq hoge msg)
  (let (a b n)
    ;; (print msg))
    (setq a (send msg :data))
    (setq n (length a))
    (cond
     ((not (eq (length *amp-data*) n))
      (format t "[fourie-filter] update buffer size ~A -> ~A~%"
	      (length *amp-data*) n)
      (setq *amp-data* (instantiate float-vector n))
      (setq *freq-data* (instantiate float-vector (* 2 n)))
      ;;
      (setq *amp-scale-data*
	    (instantiate float-vector (floor (* *amp-scale* n))))
      (setq *amp-data-string* (instantiate string (length *amp-scale-data*)))
      ;;
      (sys::gc)
      ))
    (byte-array2float-vector a *amp-data*)
    (linear-scale-float-vector (/ 1.0 *amp-scale*) *amp-data* *amp-scale-data*)
    (float-vector2byte-array *amp-scale-data* *amp-data-string*
			     :scale (/ 1.0 n))
    (ros::publish "/audio_out"
		  (instance audio_common_msgs::audiodata :init
			    :data *amp-data-string*)))
  )

(ros::subscribe "/audio_in"
		audio_common_msgs::audiodata
		#'audio-callback
		10)
(ros::advertise "/audio_out"
		audio_common_msgs::audiodata
		10)

(ros::rate 100)
;;(ros::spin)
(do-until-key
  (if (not (ros::ok)) (return-from nil nil))
  (ros::spin-once)
  (ros::sleep))
