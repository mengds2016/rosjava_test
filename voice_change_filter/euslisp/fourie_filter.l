#!/usr/bin/env roseus

(require "package://eus_fftw/euslisp/eus_fftw.l")

(ros::roseus "audio_foulier_filtere")

(ros::load-ros-manifest "audio_common_msgs")

(defvar *amp-data* (instantiate float-vector 800))
(defvar *amp-data-string* (instantiate string (length *amp-data*)))
(defvar *freq-data* (instantiate float-vector (* 2 (length *amp-data*))))
;;
(defvar *freq-scale* 0.5)
(defvar *freq-scale-data*
  (instantiate float-vector (* 2 (floor (* *freq-scale* (length *amp-data*))))))
(defvar *amp-scale-data*
  (instantiate float-vector (floor (* *freq-scale* (length *amp-data*)))))

(defun byte-array2float-vector
  (barray fv)
  (dotimes (i (length barray))
    (setf (aref fv i) (* 1.0 (aref barray i)))))

(defun float-vector2byte-array
  (fv barray &key (scale 1.0))
  (dotimes (i (length barray))
    (setf (aref barray i)
	  (max 0 (min (round (* scale (aref fv i))) 255)))))

(defun linear-gain-tune
  (start end fv
	 &key
	 (size (/ (length fv) 2))
	 buf)
  (dotimes (i size)
    (setq buf (/ i (- size 1.0)))
    (setq gain (+ (* buf end)
		  (* (- 1 buf) start)))
    ;; (format t " gain ~A vs ~A~%" i gain)
    (dotimes (j 2)
      (setf (aref fv (+ j (* 2 i)))
	    (* gain (aref fv (+ j (* 2 i))))))))

(defun right-shift-array
  (step fv
	&key
	(n (length fv))
	(size (/ n 2))
	buf i)
  (dotimes (_i size)
    (setq i (- (- size _i) 1))
    (dotimes (j 2)
      (cond
       ((< i step)
	(setf (aref fv (+ j (* 2 i))) 0))
       (t
	(setf (aref fv (+ j (* 2 i)))
	      (aref fv (+ j (* 2 (- i step)))))))))
  )

(defun linear-scale-float-vector
  (scale in out &key (max (- (length in) 1)))
  (dotimes (i (length out))
    (let* ((val (/ (* 1.0 i) scale))
	   (sml (floor val))
	   (big (+ sml 1)))
      (setf (aref out i)
	    (+ (* (- val sml) (aref in (min max big)))
	       (* (- big val) (aref in (min max sml))))))))

;; (defun convert-test
;;   nil
;;   (let ((buf (copy-seq *amp-data-string*)))
;;     (byte-array2float-vector *amp-data-string* *amp-data*)
;;     (float-vector2byte-array *amp-data* *amp-data-string*)
;;     (dotimes (i (length buf))
;;       (format t " ~A vs ~A~%" (aref *amp-data-string* i) (aref buf i)))
;;     buf))

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
      (setq *amp-data-string* (instantiate string n))
      (setq *freq-data* (instantiate float-vector (* 2 n)))
      ;;
      (setq *freq-scale-data*
	    (instantiate float-vector (* 2 (floor (* *freq-scale* n)))))
      (setq *amp-scale-data*
	    (instantiate float-vector (floor (* *freq-scale* n))))
      ;;
      (sys::gc)
      ))
    (byte-array2float-vector a *amp-data*)
    (fft-forward *amp-data* *freq-data* n)
    ;; impl
    ;; (linear-gain-tune 0.9 1.1 *freq-data*)
    ;; (right-shift-array 100 *freq-data*)
    ;; (linear-gain-tune 0.0 1.0 *freq-data* :size 100)
    ;;
    ;; (linear-scale-float-vector *freq-scale* *freq-data* *freq-scale-data*)
    ;; (fft-backward *freq-scale-data* *amp-scale-data* (length *amp-scale-data*))
    ;; (linear-scale-float-vector (/ 1.0 *freq-scale*) *amp-scale-data* *amp-data*)
    ;;
    (fft-backward *freq-data* *amp-data* n)
    (float-vector2byte-array *amp-data* *amp-data-string*
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
