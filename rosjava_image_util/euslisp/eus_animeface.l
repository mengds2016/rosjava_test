#!/usr/bin/env roseus

(ros::roseus "eus_animeface")

(defvar *inpath* "aria.jpg")
(defvar *outpath* "out.jpg")

(defun detect-animeface
  (&key (inpath *inpath*)
	(outpath *outpath*)
	(root-dir (format nil "~A/rosjava_image_util/script"
			  (ros::rospack-find "rosjava_test")))
	ret)
  (let* ((in (piped-fork (format nil "perl ~A/animeface_sample.pl ~A ~A"
				 root-dir inpath outpath)))
	 buf)
    (while (and (setq buf (read-line in))
		(not (substringp "eof" buf)))
      (push (read-from-string (format nil "(~A)" buf)) ret))
    ret))

(defun set-inpath-callback (msg) (setq *inpath* (send msg :data)))
(defun set-outpath-callback (msg) (setq *outpath* (send msg :data)))

(defvar *request-cnt*)
(defun request-callback (msg)
  (if (probe-file (send msg :data))
      (setq *inpath* (send msg :data)))
  (let* ((ret (detect-animeface :inpath *inpath* :outpath *outpath*))
	 (face (assoc :face ret))
	 (reye (find-if #'(lambda (d) (find :right d)) ret))
	 (leye (find-if #'(lambda (d) (find :left d)) ret)))
    (ros::publish "/eus_animeface/feedback/outputpath"
		  (instance std_msgs::string :init :data *outpath*))
    (if face
	(ros::publish "/eus_animeface/face/rectangle"
		      (instance std_msgs::int32multiarray :init
				:data (integer-vector
				       (or (nth 1 face) 0)
				       (or (nth 2 face) 0)
				       (or (nth 3 face) 0)
				       (or (nth 4 face) 0)))))
    (if reye
	(ros::publish "/eus_animeface/eyes/right/rectangle"
		      (instance std_msgs::int32multiarray :init
				:data (integer-vector
				       (or (nth 2 reye) 0)
				       (or (nth 3 reye) 0)
				       (or (nth 4 reye) 0)
				       (or (nth 5 reye) 0)))))
    (if leye
	(ros::publish "/eus_animeface/eyes/left/rectangle"
		      (instance std_msgs::int32multiarray :init
				:data (integer-vector
				       (or (nth 2 leye) 0)
				       (or (nth 3 leye) 0)
				       (or (nth 4 leye) 0)
				       (or (nth 5 leye) 0)))))
    ))

(ros::advertise "/eus_animeface/face/rectangle"
		std_msgs::int32multiarray)
(ros::advertise "/eus_animeface/eyes/right/rectangle"
		std_msgs::int32multiarray)
(ros::advertise "/eus_animeface/eyes/left/rectangle"
		std_msgs::int32multiarray)
(ros::advertise "/eus_animeface/feedback/outputpath"
		std_msgs::string)

(ros::subscribe "/eus_animeface/set_inpath/string"
		std_msgs::String
		#'set-inpath-callback
		1)
(ros::subscribe "/eus_animeface/set_outpath/string"
		std_msgs::String
		#'set-outpath-callback
		1)
(ros::subscribe "/eus_animeface/detect_request/string"
		std_msgs::String
		#'request-callback
		1)


(ros::rate 10)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::spin-once)
 (ros::sleep)
 )


;; (defun read-all-animeface
;;   (&key ret buf)
;;   (while (setq buf (read-line *perl-process* nil))
;;     (print buf)
;;     (push buf ret))
;;   ret)

;; (defun setup-animeface
;;   nil
;;   (format *perl-process*
;; 	  "use lib qw(/home/udp/local/lib/perl5/site_perl/5.8.8/mach /home/udp/local/lib/perl5/site_perl/5.8.8); use Imager; use Imager::AnimeFace; use strict; use warnings; use Data::Dumper; my $im = Imager->new(); my @results; my $results; my $blue = Imager::Color->new(0, 0, 255); my $red = Imager::Color->new(255, 0, 0); local $| = 1; ~%")
;;   (read-line *perl-process*)
;;   )

;; (defun draw-animeface-rect
;;   (&key (inpath "./aria.jpg")
;; 	(outpath "./out.jpg"))
;;   (format *perl-process*
;; 	  "$im->read(file=>\"~A\"); $results = detect_animeface($im); foreach my $face (@{$results}) { $im->box( xmin => $face->{face}->{x}, ymin => $face->{face}->{y}, xmax => $face->{face}->{x} + $face->{face}->{width}, ymax => $face->{face}->{y} + $face->{face}->{height}, color => $blue); $im->box( xmin => $face->{eyes}->{left}->{x}, ymin => $face->{eyes}->{left}->{y}, xmax => $face->{eyes}->{left}->{x} + $face->{eyes}->{left}->{width}, ymax => $face->{eyes}->{left}->{y} + $face->{eyes}->{left}->{height}, color => $red); $im->box(xmin => $face->{eyes}->{right}->{x}, ymin => $face->{eyes}->{right}->{y}, xmax => $face->{eyes}->{right}->{x} + $face->{eyes}->{right}->{width}, ymax => $face->{eyes}->{right}->{y} + $face->{eyes}->{right}->{height}, color => $red);} $im->write(file => \"~A\", type=>'jpeg'); ~%" inpath outpath)
;;   ;; (read-line *perl-process* nil)
;;   (read-line *perl-process*)
;;   )

;; (defvar *perl-process* (piped-fork "perl ../script/perl_interpreter.pl"))

;; (print (setup-animeface))
;; (print (draw-animeface-rect))
