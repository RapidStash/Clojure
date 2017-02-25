(in-ns 'rapidstash.core)
(import '[java.io RandomAccessFile])

(def frameSize 2048)
(def collectionHeader (atom nil))
(defn getSpecSize [s] (.capacity (.buffer (compose-buffer s))))

(def collectionHeader (spec :freeSpot (int32-type)
                            :numFrames (int32-type)))

(def collectionHeaderSize (getSpecSize collectionHeader))

(def frameHeader (spec :nextFrame (int32-type)
                       :docId (string-type 36)))

(def frameHeaderSize (getSpecSize frameHeader))

(def frameData (spec :data (string-type (- frameSize (getSpecSize frameHeader)))))

(def frameDataSize (getSpecSize frameData))

(def frame (concat frameHeader frameData))

(defn getFramePosition 
   [frameId] 
   (+ collectionHeaderSize (* frameId frameSize)))

(defn createLargeFile
   "creates a 2gb file that can be immmediately mapped"
   [filename]
   (doto (RandomAccessFile. filename "rw")
      (.seek (* 1024 1024 1024 2))
      (.writeInt 0)
      (.close)))

(defn writeBuffer
   [file buf spot]
   (let [arr (byte-array (.capacity (.buffer buf)))]
      (.getBytes (.buffer buf) 0 arr)
      (mmap/put-bytes file arr spot)))

(defn readBuffer
   [file spot frameSpec]
   (def dest (compose-buffer frameSpec))
   (def numBytes (.capacity (.buffer dest)))
   (def source (mmap/get-bytes file spot numBytes))
   (.setBytes (.buffer dest) 0 source 0 numBytes)
   dest)

(defn writeCollectionHeader
   [file freeSpot numFrames]
   "Write the header to the collection"
   (let [buf (compose-buffer collectionHeader)]
      (set-field buf :freeSpot freeSpot)
      (set-field buf :numFrames numFrames)
      (writeBuffer file buf 0)))

(defn readCollectionHeader
   [file]
   "Read the header from the collection file"
   (readBuffer file 0 collectionHeader))

(defn openCollection
   "open a collection and return the memory map"
   [collName]
   (def newCollection? false)
   (if (not (.exists (clojure.java.io/file collName)))
     (do
      	(createLargeFile collName)
        (def newCollection? true)))
   (def file (mmap/get-mmap collName :read-write))
   (if (= newCollection? true)
      (writeCollectionHeader file 0 0))
   (def collectionHeader (readCollectionHeader file))
   file)
