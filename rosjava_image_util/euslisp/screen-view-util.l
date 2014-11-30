#!/usr/bin/env roseus

(defun calc-null-direction
  (&key (x (float-vector 0 0 0))) ;; yi' = yi * y3'/y3
  (let* ((eye (send *viewer* :get-val 'geometry::eye))
	 (ec (send eye :get-val 'geometry::viewcoords))
	 (P (send eye :projection))
	 (Rx+p (v+ (send ec :worldpos)
		   (transform (send ec :worldrot) x)))
	 (Rx+p/1 (concatenate float-vector Rx+p '(1)))
	 (y (transform P Rx+p/1))
	 (y_ (transform P (float-vector 0 0 1 0)))
	 (y3_ (aref y_ 3))
	 (y3 (aref y 3)) u
	 ret)
    (dotimes (i 2)
      (setf (aref y_ i) (* (/ y3_ y3) (aref y i))))
    (setq u (transform (pseudo-inverse P) y_))
    ;; (dotimes (i 2)
    ;;   (let* ((gain (random 10.0)))
    ;; 	(format t "yi/y3 = ~A vs ~A [gain:~A]~%"
    ;; 		(/ (aref y i) (aref y 3))
    ;; 		(/ (+ (aref y i) (* gain (aref y_ i)))
    ;; 		   (+ (aref y 3) (* gain (aref y_ 3))))
    ;; 		gain)))
    (cond
     ((or (> (abs (- (aref u 2) 1.0)) 1e-3)
	  (> (abs (- (aref u 3) 0.0)) 1e-3))
      (format t "[calc-null-direction] invalid 2~3 elements ~A~%" u)))
    (setq u (subseq u 0 3))
    ;; (print Rx+p/1) (print y) (print y_)
    ;; (print u)
    (setq
     ret
     (normalize-vector
      (transform
       (transpose (send ec :worldrot)) u)))
    ;; (format t " ~A vs ~A~%" (send eye :view-direction) ret)
    ret
    ;; (send eye :view-direction)
    ))

;; (let* ((v (random-vector 100.0))) (dotimes (i 10) (point-to-screen (v+ v (scale i (calc-null-direction :x v))))))
(defun point-to-screen
  (v
   &key
   (eye (send *viewer* :get-val 'geometry::eye))
   (vc (send eye :get-val 'geometry::viewcoords))
   (pm (send eye :projection))
   (ir (send *viewer* :viewport :worldrot))
   (ip (send *viewer* :viewport :worldpos))
   ;;
   (vv (v+ (transform (send vc :worldrot) v)
	   (send vc :worldpos)))
   (vv1 (geo::homogenize vv))
   (vp (transform pm vv1))
   (nvp (float-vector (/ (aref vp 0) (aref vp 3))
		      (/ (aref vp 1) (aref vp 3))
		      (/ (aref vp 2) (aref vp 3))))
   (nvp_ (v+ (transform ir nvp) ip))
   )
  ;;(format t "~A ~A ~A ~A~%" vv vv1 vp nvp)
  nvp_
  )

(defun calc-null-gain-for-min-distance
  (target-pos vc)
  (let* ((null-dir (calc-null-direction :x vc)))
    ;;(send eye :view-direction)))
    (v. (normalize-vector null-dir)
	(v- target-pos vc))))

;; (point-to-screen (screen-to-point #F(100 100 0) :gain 2000.0))
(defun screen-to-point
  (nvp_
   &key
   (gain nil)
   (null-dir (if gain (calc-null-direction :x (screen-to-point nvp_))))
   ;; (send eye :view-direction))
   (eye (send *viewer* :get-val 'geometry::eye))
   (vc (send eye :get-val 'geometry::viewcoords))
   (pm_ (send eye :projection))
   (pm pm_)
   ;;(let* ((buf (copy-object pm_)))
   ;;(dotimes (i 4) (setf (aref buf 2 i) 0))
   ;;(print buf)))
   (ir (send *viewer* :viewport :worldrot))
   (ip (send *viewer* :viewport :worldpos))
   ;;
   (nvp (transform (pseudo-inverse ir) (v- nvp_ ip)))
   (vp (concatenate float-vector nvp '(1)))
   (ipm (pseudo-inverse pm))
   (vv1 (transform ipm vp))
   ;;(ivv1 (transform ipm (float-vector 0 0 1 0)))
   ;;(ivv (float-vector (/ (aref ivv1 0) (aref ivv1 3))
   ;;(/ (aref ivv1 1) (aref ivv1 3))
   ;;(/ (aref ivv1 2) (aref ivv1 3))))
   (vv (float-vector (/ (aref vv1 0) (aref vv1 3))
		     (/ (aref vv1 1) (aref vv1 3))
		     (/ (aref vv1 2) (aref vv1 3))))
   (v (transform (pseudo-inverse (send vc :worldrot))
		 (v- vv (send vc :worldpos))))
   ;; (ans (send *viewer* :point-to-screen v))
   )
  ;; (format t "~A ~A ~A ~A~%" vv vv1 vp nvp)
  (if gain
      (v+ v (scale gain null-dir))
    v)
  ;; v
  )

(defun draw-shoulder-line
  (&key
   (shoulder-pos-list
    (send *robot* :arms :shoulder-p :child-link :worldpos))
   (shoulder-pos-screen-list
    (mapcar
     #'(lambda (pos)
	 (let ((spos (point-to-screen pos)))
	   ;;(send *viewer* :point-to-screen pos)))
	   ;;(send *viewer* :viewport :ndc-point-to-screen
	   ;;(scale (/ 1.0 (aref pos 2)) pos))))
	   (integer-vector (round (aref spos 0))
			   (round (aref spos 1)))))
     shoulder-pos-list))
   (shoulder-distance (apply #'v- shoulder-pos-screen-list))
   (flush t))
  (print shoulder-pos-list)
  (print shoulder-pos-screen-list)
  (send *viewer* :viewsurface :draw-line
	(car shoulder-pos-screen-list) (cadr shoulder-pos-screen-list))
  (if flush (send *viewer* :viewsurface :flush)))

