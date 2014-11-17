(defun intToBytes
  (value)
  (list (ash (logand value #x000000ff) 0)
	(ash (logand value #x0000ff00) -8)
	(ash (logand value #x00ff0000) -16)
	(ash (logand value #xff000000) -24)))

(defun create-wav-header
  (sampleRate datasize)
  (let* ((byteRIFF (list #\R #\I #\F #\F))
	 (byteFilesizeSub8 (+ datasize 36))
	 (byteWAVE (list #\W #\A #\V #\E))
	 (byteFMT_ (list #\f #\m #\t #\ ))
	 (byte16bit 16)
	 (byteSamplerate sampleRate)
	 (byteBytesPerSec (* sampleRate 2))
	 (bytePcmMono (list #x1 #x0 #x1 #x0))
	 (byteBlockBit (list #x02 #x00 #x10 #x00))
	 (byteDATA (list #\d #\a #\t #\a))
	 (byteDatasize datasize))
    (concatenate
     string
     (flatten (list byteRIFF byteFilesizeSub8 byteWAVE byteFMT_ byte16bit bytePcmMono byteSamplerate byteBytesPerSec byteBlockBit byteDATA byteDatasize)))
    ))
