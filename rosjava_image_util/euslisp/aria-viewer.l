#!/usr/bin/env roseus

(ros::load-ros-manifest "sensor_msgs")
(ros::roseus "aria_viewer")

(require "package://aria_model/aria.l")
(require "package://aria_model/euslisp/aria-interface.l")
(require "screen-view-util.l")

(defvar *robot*)
(defvar *tmp_image_path* "/tmp/test.jpg")

(aria-init :viewer? nil)
(objects *robot*)
(send *irtviewer* :change-background #F(1.0 1.0 1.0))
(send *irtviewer* :title "ARIA VIEWER")
;; (send *irtviewer* :resize 400 556)
(send *irtviewer* :draw-objects)

(send *viewer* :adjust-viewport
      (send *viewer* :viewsurface :width)
      (send *viewer* :viewsurface :height))

(send-all (send *robot* :joint-list) :min-angle -50)
(send-all (send *robot* :joint-list) :max-angle 50)
(send *robot* :head :neck-p :max-angle 20)
(send *robot* :arms :shouder-y :min-angle -30)
(send *robot* :arms :shouder-y :max-angle +30)

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
     (min (float-vector -1000 -1000  -1000 0 0 0))
     (max (float-vector  1000  1000   1000 0 0 0))
     (root-link-virtual-joint-weight
      (scale 1e-3 (float-vector 1 1 1 0 0 0)))
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
   (move-target (list (send robot :get :reye-end-coords)
		      (send robot :get :leye-end-coords)
		      (send robot :head :end-coords)
		      (send robot :rarm :end-coords)
		      (send robot :larm :end-coords)
		      (send robot :torso :end-coords)
		      (send robot :get :relbow-end-coords)
		      (send robot :get :lelbow-end-coords)
		      (send robot :get :rshoulder-end-coords)
		      (send robot :get :lshoulder-end-coords)))
   (link-list (mapcar
	       #'(lambda (mt)
		   (send robot :link-list (send mt :parent)))
	       move-target))
   (target-msg (list *left-eye* *right-eye*
		     *chin* *left-hand* *right-hand* *pelvis*
		     *left-elbow* *right-elbow*
		     *left-shoulder* *right-shoulder*
		     ))
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
  (let* ((d1 (norm (v- (send (car target-coords) :worldpos)
		       (send (cadr target-coords) :worldpos))))
	 (d2 (norm (v- (send (car move-target) :worldpos)
		       (send (cadr move-target) :worldpos))))
	 (gain -10.0)
	 (min 10))
    (print (- d1 d2))
    (cond
     ((or (< (aref *right-eye* 0) 0)
	  (< (aref *left-eye* 0) 0)) 'nop)
     ((< (abs (- d1 d2)) min) 'conv)
     (t
      (send *robot* :translate
	    (scale (* gain (- d1 d2)) (calc-null-direction)) :world))))
  ;; (send-all target-coords
  ;; :draw-on :flush t :color #F(0 1 0) :size 10)
  ;; (read-line)
  (apply
   #'send
   (append
    (list robot :6dof-inverse-kinematics  target-coords)
    args
    (list
     ;; :root-link-virtual-joint-weight (float-vector 0 0 0 0 0 0)
     ;; :min (float-vector 0 0 0 0 0 0)
     ;; :max (float-vector 0 0 0 0 0 0)
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

(send *robot* :put :rshoulder-end-coords
      (make-cascoords
       :name :rshoulder-end-coords
       :coords (send (send *robot* :rarm :shoulder-p :child-link) :copy-worldcoords)
       :parent (send *robot* :rarm :shoulder-p :child-link)))

(send *robot* :put :lshoulder-end-coords
      (make-cascoords
       :name :lshoulder-end-coords
       :coords (send (send *robot* :larm :shoulder-p :child-link) :copy-worldcoords)
       :parent (send *robot* :larm :shoulder-p :child-link)))


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
  (if flush (send *viewer* :viewsurface :flush))
  ivec)

(defvar *left-shoulder* (float-vector -1 0 0 0))
(defvar *right-shoulder* (float-vector -1 0 0 0))
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
      (send *irtviewer* :resize
	    (round (* 0.95 (aref (send msg :data) 0)))
	    (aref (send msg :data) 1)))
  (send *viewer* :adjust-viewport
	(send *viewer* :viewsurface :width)
	(send *viewer* :viewsurface :height))
  (if flush (send *viewer* :draw-objects)))

(defun reset-callback
  (msg)
  (send *robot* :reset-pose)
  (dotimes (i 3) (send *robot* :newcoords (make-coords)))
  (send *viewer* :draw-objects))

(ros::subscribe "/aria_viewer/window_size"
		std_msgs::int32multiarray
		#'window-size-callback)
(ros::subscribe "/image_window_node/drop_file/path"
		std_msgs::string
		#'reset-callback)
(ros::subscribe "/image_window_node/left_shoulder/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-shoulder*)
(ros::subscribe "/image_window_node/right_shoulder/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-shoulder*)
(ros::subscribe "/image_window_node/left_elbow/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-elbow*)
(ros::subscribe "/image_window_node/right_elbow/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-elbow*)
(ros::subscribe "/image_window_node/left_hand/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *left-hand*)
(ros::subscribe "/image_window_node/right_hand/rect/normalize"
		std_msgs::float32multiarray
		#'copy-rect-callback *right-hand*)
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
 ;; (send *robot* :newcoords
 ;;       (make-coords
 ;; 	:rot (copy-object (send *robot* :worldrot))
 ;; 	:pos (let* ((p (send *robot* :worldpos))
 ;; 		    (nd (calc-null-direction)))
 ;; 	       (v- p (scale (v. p nd) nd)))))
 (send *viewer* :draw-objects :flush nil)
 ;;
 ;; (send *viewer* :viewsurface :color #f(1 0 0))
 ;; (draw-normalize-rectangle *face* :flush nil)
 ;; (send *viewer* :viewsurface :color #f(0 1 0))
 ;; (draw-normalize-rectangle *right-eye* :flush nil)
 ;; (draw-normalize-rectangle *left-eye* :flush nil)
 ;; (draw-normalize-rectangle *chin* :flush nil)
 ;; (send *viewer* :viewsurface :color #f(0 0 1))
 ;; (draw-normalize-rectangle *right-elbow* :flush nil)
 ;; (draw-normalize-rectangle *left-elbow* :flush nil)
 ;; (draw-normalize-rectangle *right-hand* :flush nil)
 ;; (draw-normalize-rectangle *left-hand* :flush nil)
 ;; (send (send (send *robot* :rarm :end-coords) :worldpos) :draw-on :flush t :color #F(1 0 0) :size 100 :width 100)
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
 )

