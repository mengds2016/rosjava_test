#!/usr/bin/env roseus

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")

(defvar *robot*)
(defvar *tmp_image_path* "/tmp/test.jpg")

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :title "ARIA VIEWER")
(send *irtviewer* :resize 400 556)
(send *irtviewer* :draw-objects)

(send *viewer* :adjust-viewport
      (send *viewer* :viewsurface :width)
      (send *viewer* :viewsurface :height))

(send-all (send *robot* :joint-list) :min-angle -10000)
(send-all (send *robot* :joint-list) :max-angle 10000)

(ros::advertise "/sensor_image_node/command/string"
                std_msgs::string
                1)
(ros::advertise "/image_window_node/image/in/raw"
                sensor_msgs::image
                1)
(ros::advertise "/aria_viewer/joint_states"
                sensor_msgs::jointstate
                1)


(defmethod robot-model
  (:6dof-inverse-kinematics
    (target-coords
     &rest args
     &key (move-target) (link-list)
     (min (float-vector -5000 -5000  -5000 -200 -200 -100))
     (max (float-vector  5000  5000   250  200  200  100))
     (root-link-virtual-joint-weight
      (scale 1e-3 (float-vector 1 1 1 1 1 1)))
     (joint-args nil)
     &allow-other-keys)
    (with-append-root-joint
     (link-list-with-robot-6dof self link-list
                                :joint-class 6dof-joint
                                :joint-args (append (list :min min :max max) joint-args))
     (send* self :inverse-kinematics target-coords
            :move-target move-target :link-list link-list-with-robot-6dof
            args)
     ))
  )

(defmethod coordinates
  (:difference-position
   (coords &key (translation-axis t))
   (let ((dif-pos
          (send self :inverse-transform-vector (send coords :worldpos))))
     (case
	 translation-axis
       ((:x :xx) (setf (elt dif-pos 0) 0))
       ((:y :yy) (setf (elt dif-pos 1) 0))
       ((:z :zz) (setf (elt dif-pos 2) 0))
       ((:xy :yx) (setf (elt dif-pos 0) 0) (setf (elt dif-pos 1) 0))
       ((:yz :zy) (setf (elt dif-pos 1) 0) (setf (elt dif-pos 2) 0))
       ((:zx :xz) (setf (elt dif-pos 2) 0) (setf (elt dif-pos 0) 0)))
     (if (vectorp translation-axis)
         (let ((ra
                (transform
                 (inverse-matrix (send self :worldrot))
                 translation-axis)))
           ;; (scale (v. dif-pos ra) ra))
	   (v- ra (scale (v. dif-pos ra) ra)))
       dif-pos))))

(defun solve-point-ik
  (&rest
   args
   &key
   (robot *robot*)
   (move-target (list (send robot :head :end-coords)
		      (send robot :rarm :end-coords)
		      (send robot :larm :end-coords)
		      (send robot :torso :end-coords)
		      (send robot :get :reye-end-coords)
		      (send robot :get :leye-end-coords)
		      (send robot :get :relbow-end-coords)
		      (send robot :get :lelbow-end-coords)))
   (link-list (mapcar
	       #'(lambda (mt)
		   (send robot :link-list (send mt :parent)))
	       move-target))
   (target-msg (list *chin* *left-hand* *right-hand* *pelvis*
		     *left-eye* *right-eye* *left-elbow* *right-elbow*))
   (_screen-target-point
    (mapcar #'screen-to-point (mapcar #'normalize-rectangle-centroid-pos target-msg)))
   (screen-target-point-min-gain
    (mapcar #'calc-null-gain-for-min-distance
	    (send-all (send-all move-target :worldcoords) :worldpos)
	    _screen-target-point))
   (null-dir (mapcar #'(lambda (v) (calc-null-direction :x v))  _screen-target-point))
   (screen-target-point
    (mapcar #'(lambda (v g dir) (v+ (scale g dir) v))
	    _screen-target-point screen-target-point-min-gain null-dir))
   (target-coords
    (mapcar #'(lambda (v) (make-coords :pos v)) screen-target-point))
   ;; (translation-axis null-dir)
   (translation-axis
    (mapcar #'(lambda (vec) (if (find-if #'minusp vec) nil t))
	    target-msg))
   (rotation-axis (make-list (length null-dir)))
   (debug-view :no-message)
   (revert-if-fail t)
   &allow-other-keys
   )
  ;; (send-all target-coords
  ;; :draw-on :flush t :color #F(0 1 0) :size 10)
  ;; (read-line)
  (apply
   #'send
   (append
    (list robot :6dof-inverse-kinematics  target-coords)
    args
    (list
     :move-target move-target
     :link-list link-list
     :translation-axis translation-axis
     :rotation-axis rotation-axis
     :debug-view debug-view
     :revert-if-fail revert-if-fail))))

(defun calc-eye-position
  nil
  (let* ((reye (send *robot* :get-val 'eyer_end_link_lk))
	 (leye (send *robot* :get-val 'eyel_end_link_lk))
	 (init (send-all (list reye leye) :worldcoords))
	 (reye-faces (flatten (send-all (send reye :bodies) :faces)))
	 (leye-faces (flatten (send-all (send leye :bodies) :faces)))
	 (reye-vertices (flatten (send-all reye-faces :vertices)))
	 (leye-vertices (flatten (send-all leye-faces :vertices)))
	 )
    ;;(send-all (list reye leye) :draw-on :flush t :color #F(0 1 0))
    ;;(send-all
    (list
     (scale (/ 1.0 (length reye-vertices)) (reduce #'v+ reye-vertices))
     (scale (/ 1.0 (length leye-vertices)) (reduce #'v+ leye-vertices)))
    ;;:draw-on :flush t :color #F(1 0 0))
    ))

;; def eye-end-coords
(let* ((eye-pos (calc-eye-position))
       (reye-cascoords
	(make-cascoords
	 :name :reye-end-coords
	 :coords (make-coords :pos (car eye-pos))
	 :parent (send *robot* :head :end-coords :parent)))
       (leye-cascoords
	(make-cascoords
	 :name :reye-end-coords
	 :coords (make-coords :pos (cadr eye-pos))
	 :parent (send *robot* :head :end-coords :parent))))
  (send *robot* :put :reye-end-coords reye-cascoords)
  (send *robot* :put :leye-end-coords leye-cascoords))

(send *robot* :put :relbow-end-coords
      (make-cascoords
       :name :relbow-end-coords
       :coords (send (send *robot* :rarm :elbow-p :child-link) :copy-worldcoords)
       :parent (send *robot* :rarm :elbow-p :child-link)))

(send *robot* :put :lelbow-end-coords
      (make-cascoords
       :name :lelbow-end-coords
       :coords (send (send *robot* :larm :elbow-p :child-link) :copy-worldcoords)
       :parent (send *robot* :larm :elbow-p :child-link)))

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

(defun update-head-position
  (&key (flush t))
  (labels ((calc-target-pos
	    (vec tpos)
	    (let* ((screen-chin-pos (normalize-rectangle-centroid-pos vec))
		   (chin-pos_ (screen-to-point screen-chin-pos))
		   ;;(chin-cascoords (send (send *robot* :head :end-coords) :worldcoords))
		   (chin-gain (calc-null-gain-for-min-distance tpos chin-pos_))
		   (chin-pos2 (screen-to-point screen-chin-pos :gain chin-gain)))
	      ;; (print chin-gain)
	      (scale 1.0 chin-pos2))))
    (let* ((now-pos-list
	    (send-all
	     (send-all
	      (list (send *robot* :head :end-coords)
		    (send *robot* :get :reye-end-coords)
		    (send *robot* :get :leye-end-coords))
	      :worldcoords)
	     :worldpos))
	   ;;(append (list (send *robot* :head :end-coords :worldpos))
	   ;;(calc-eye-position)))
	   (target-pos-list
	    (mapcar #'calc-target-pos
		    (list *chin* *right-eye* *left-eye*)
		    ;; (list *right-elbow* *right-elbow* *right-elbow*)
		    now-pos-list))
	   (move-pos-list (mapcar #'v- target-pos-list now-pos-list))
	   )
      ;;(print now-pos-list)
      ;; (format t "~A -> ~A -> ~A -> ~A [~A]~%"
      ;; 	      (normalize-rectangle-centroid-pos *right-elbow*)
      ;; 	      (screen-to-point (normalize-rectangle-centroid-pos *right-elbow*))
      ;; 	      (car target-pos-list)
      ;; 	      (point-to-screen (car target-pos-list))
      ;; 	      (car scaled-move-pos-list))
      ;;(print target-pos-list)
      (send *robot* :translate
	    (scale (/ 1.0 (length target-pos-list))
		   (reduce #'v+ move-pos-list))
	    :world)
      (send-all (send *robot* :links) :worldcoords)
      ;;(print
      ;;(append (list (send *robot* :head :end-coords :worldpos))
      ;;(calc-eye-position)))
      (if flush (send *viewer* :draw-objects))
      )))

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

(defun normalize-rectangle-centroid-pos
  (nvec &key
	(scale (send *irtviewer* :width))
	(h (send *irtviewer* :height))
	(vec (scale scale nvec)))
  (float-vector (+ (round (aref vec 0)) (/ (round (aref vec 2)) 2.0))
		(+ (round (aref vec 1)) (/ (round (aref vec 3)) 2.0))
		0))

(defun normalize-rectangle
  (nvec &key
	(scale (send *irtviewer* :width))
	(h (send *irtviewer* :height))
	(vec (scale scale nvec)))
  (integer-vector (round (aref vec 0))
		  (max 0 (- (- h (round (aref vec 1))) (round (aref vec 3))))
		  (max 5 (round (aref vec 2)))
		  (max 5 (round (aref vec 3)))))

(defun draw-normalize-rectangle
  (nvec &key
	(ivec (normalize-rectangle nvec))
	(flush t))
  (send *viewer* :viewsurface :draw-rectangle
	(subseq ivec 0 2) (aref ivec 2) (aref ivec 3))
  (if flush (send *viewer* :viewsurface :flush)))

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

(defvar *left-elbow* (float-vector -1 0 0 0))
(defvar *right-elbow* (float-vector -1 0 0 0))
(defvar *left-hand* (float-vector -1 0 0 0))
(defvar *right-hand* (float-vector -1 0 0 0))
(defvar *left-eye* (float-vector -1 0 0 0))
(defvar *right-eye* (float-vector -1 0 0 0))
(defvar *face* (float-vector -1 0 0 0))
(defvar *chin* (float-vector -1 0 0 0))
(defvar *pelvis* (float-vector -1 0 0 0))

(defun copy-rect-callback (vec msg)
  (dotimes (i (min (length (send msg :data)) (length vec)))
    (setf (aref vec i) (aref (send msg :data) i))))

(defun window-size-callback (msg &key (flush nil))
  (if (>= (length (send msg :data)) 2)
      (send *irtviewer* :resize (aref (send msg :data) 0)
	    (aref (send msg :data) 1)))
  (send *viewer* :adjust-viewport
	(send *viewer* :viewsurface :width)
	(send *viewer* :viewsurface :height))
  (if flush (send *viewer* :draw-objects)))

(ros::subscribe "/aria_viewer/window_size"
		std_msgs::int32multiarray
		#'window-size-callback)

(ros::subscribe "/image_window_node/left_hand/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-hand*)
(ros::subscribe "/image_window_node/right_hand/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-hand*)
(ros::subscribe "/image_window_node/left_elbow/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-elbow*)
(ros::subscribe "/image_window_node/right_elbow/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-elbow*)
(ros::subscribe "/eus_animeface/eyes/left/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-eye*)
(ros::subscribe "/eus_animeface/eyes/right/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-eye*)
(ros::subscribe "/eus_animeface/face/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *face*)
(ros::subscribe "/eus_animeface/chin/rectangle/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *chin*)
(ros::subscribe "/image_window_node/pelvis/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *pelvis*)

(ros::rate 5)
(do-until-key
 (if (not (ros::ok)) (return-from nil nil))
 (ros::sleep)
 (ros::spin-once)
 (x::window-main-one)
 ;;  (update-head-position :flush nil)
 (solve-point-ik :stop 3 :debug-view nil :revert-if-fail nil)
 (send *viewer* :draw-objects :flush nil)
 ;;
 (send *viewer* :viewsurface :color #f(1 0 0))
 (draw-normalize-rectangle *face* :flush nil)
 (send *viewer* :viewsurface :color #f(0 1 0))
 (draw-normalize-rectangle *right-eye* :flush nil)
 (draw-normalize-rectangle *left-eye* :flush nil)
 (draw-normalize-rectangle *chin* :flush nil)
 (send *viewer* :viewsurface :color #f(0 0 1))
 (draw-normalize-rectangle *right-elbow* :flush nil)
 (draw-normalize-rectangle *left-elbow* :flush nil)
 (draw-normalize-rectangle *right-hand* :flush nil)
 (draw-normalize-rectangle *left-hand* :flush nil)
 ;;
 (send *viewer* :viewsurface :flush)
 (let* ((img (send *viewer* :viewsurface :getglimage)))
   (ros::publish
    "/image_window_node/image/in/raw"
    (instance sensor_msgs::image :init
	      :width (send img :width)
	      :height (send img :height)
	      :encoding "bgr8" ;;"rgb8"
	      :step (* (send img :width) 3)
	      :data (send img :entity))))
 (ros::publish
  "/aria_viewer/joint_states"
  (instance sensor_msgs::jointstate :init
	    :name (send-all (send *robot* :joint-list) :name)
	    :position (mapcar #'deg2rad (send-all (send *robot* :joint-list) :joint-angle))))
 ;;(send *viewer* :viewsurface :write-to-image-file *tmp_image_path*)
 ;;(ros::publish "/sensor_image_node/command/string"
 ;;(instance std_msgs::string :init
 ;;:data *tmp_image_path*))
 )

#|


(v+ (transform (send *viewer* :viewport :worldrot) v)
    (send *viewer* :viewport :worldpos))

(let* ((img (send *viewer* :viewsurface :getglimage)))
  (ros::publish
   "/image_window_node/image/in/raw"
   (instance sensor_msgs::image :init
	     :width (send img :width)
	     :height (send img :height)
	     :encoding "rgb8"
	     :data (send img :entity))))
