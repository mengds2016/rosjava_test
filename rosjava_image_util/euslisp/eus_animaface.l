#!/usr/bin/env roseus

(ros::roseus "eus_animeface")

(defun detect-animeface
  (&key (inpath "aria.jpg")
	(outpath "out.jpg")
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
