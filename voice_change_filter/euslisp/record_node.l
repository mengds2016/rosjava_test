#!/usr/bin/env roseus

(require "package://eus_fftw/euslisp/eus_soundtouch.l")

(ros::roseus "audio_pitch_filtere")

(ros::load-ros-manifest "audio_common_msgs")

(defvar *samplerate* 44100)
(defvar *channel* 1)

(defvar *outpath* "out.wav")

(defvar *amp-data* (instantiate float-vector (/ *samplerate* 10)))

(defun byte-array2float-vector
  (barray fv &key (scale 0.9))
  (dotimes (i (length barray))
    (setf (aref fv i)
	  (- (* scale (aref barray i)) (* (/ scale 2.0) #xFF))
	  )))

(defun audio-callback
  (msg)
  (let (a b n)
    (setq a (send msg :data))
    (setq n (length a))
    (if (not (eq n (length *amp-data*)))
	(setq *amp-data* (instantiate float-vector n)))
    (byte-array2float-vector (send msg :data) *amp-data*)
    (write-wav-file *amp-data* (* n *channel*))
    ))

(ros::subscribe "/audio_in"
		audio_common_msgs::audiodata
		#'audio-callback
		10)
(ros::advertise "/audio_out"
		audio_common_msgs::audiodata
		10)

(open-out-wav-file *outpath* *samplerate* 8 *channel*)

(ros::rate 10)
;;(ros::spin)
(let* ((tm 0))
  (do-until-key
   (setq
    tm
    (+
     tm
     (bench2
      (if (not (ros::ok)) (return-from nil nil))
      (ros::spin-once)
      (ros::sleep))))
   (print tm)))
(close-out-wav-file)
