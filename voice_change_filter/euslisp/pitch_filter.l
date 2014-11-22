#!/usr/bin/env roseus

(require "package://eus_fftw/euslisp/eus_soundtouch.l")

(ros::roseus "audio_pitch_filtere")

(ros::load-ros-manifest "audio_common_msgs")

(defvar *samplerate* 16000)
(defvar *channel* 1)

(defvar *amp-data-string* (instantiate string (/ *samplerate* 10)))
(defvar *amp-data* (instantiate float-vector (* (length *amp-data-string*) 1)))

(defun byte-array2float-vector
  (barray fv &key (scale 100.0))
  (dotimes (i (length barray))
    (setf (aref fv i)
	  (- (* scale (aref barray i)) (* (/ scale 2.0) #xFF))
	  )))

(defun float-vector2byte-array
  (fv barray &key (scale 100.0))
  (dotimes (i (length barray))
    (setf (aref barray i)
	  (max #x00
	       (min #xFF
		    (round
		     (/ (+ (aref fv i) (* (/ scale 2.0) #xFF)) scale)))))))

(defun audio-callback
  (msg)
  (let (a b n)
    ;; (print msg))
    (setq a (send msg :data))
    (setq n (length a))
    (cond
     ((not (eq (length *amp-data-string*) n))
      (format t "[pitch-filter] update buffer size ~A -> ~A~%"
	      (length *amp-data*) n)
      (setq *amp-data-string* (instantiate string n))
      (setq *amp-data* (instantiate float-vector (* n 1)))
      (sys::gc)
      ))
    (byte-array2float-vector a *amp-data*)
    ;;(float-vector2byte-array *amp-data* *amp-data-string*)
    ;;(ros::publish "/audio_out"
    ;;(instance audio_common_msgs::audiodata :init
    ;;:data *amp-data-string*))
    (put-sample-sound-touch *amp-data* n *channel*)
    (while (plusp (setq b (receive-sample-sound-touch *amp-data* n *channel*)))
      (float-vector2byte-array *amp-data* *amp-data-string*)
      (ros::publish "/audio_out"
		    (instance audio_common_msgs::audiodata :init
			      :data (if (not (eq n b))
					(subseq *amp-data-string* 0 b)
				      *amp-data-string*)))))
  )

(ros::subscribe "/audio_in"
		audio_common_msgs::audiodata
		#'audio-callback
		10)
(ros::advertise "/audio_out"
		audio_common_msgs::audiodata
		10)

(set-param-sound-touch *samplerate* *channel* 1.0 3.0 0.0)
(ros::rate 100)
;;(ros::spin)
(do-until-key
  (if (not (ros::ok)) (return-from nil nil))
  (ros::spin-once)
  (ros::sleep))
